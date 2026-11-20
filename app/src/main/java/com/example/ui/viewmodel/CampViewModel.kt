package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CampRepository
import com.example.model.CampTrip
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.PackingItem
import com.example.model.TripType
import com.example.model.WeightUnit
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class CampViewModel(
    private val repository: CampRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _weightUnit = MutableStateFlow(WeightUnit.METRIC)
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    private val _activeTripId = MutableStateFlow<String?>(null)
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()

    private val _gearSearchQuery = MutableStateFlow("")
    val gearSearchQuery: StateFlow<String> = _gearSearchQuery.asStateFlow()

    private val _gearCategoryFilter = MutableStateFlow<GearCategory?>(null)
    val gearCategoryFilter: StateFlow<GearCategory?> = _gearCategoryFilter.asStateFlow()

    private val _showArchivedTrips = MutableStateFlow(false)
    val showArchivedTrips: StateFlow<Boolean> = _showArchivedTrips.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val tripsState: StateFlow<UiState<List<CampTrip>>> = repository.observeTrips(currentUserId)
        .map<List<CampTrip>, UiState<List<CampTrip>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing trips", root)
            emit(UiState.Error(root.message ?: "Failed to load trips"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val gearState: StateFlow<UiState<List<GearItem>>> = repository.observeGear(currentUserId)
        .map<List<GearItem>, UiState<List<GearItem>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing gear", root)
            emit(UiState.Error(root.message ?: "Failed to load gear locker"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val packingItemsState: StateFlow<UiState<List<PackingItem>>> = _activeTripId
        .flatMapLatest { tripId ->
            if (tripId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observePackingItems(currentUserId, tripId)
                    .map<List<PackingItem>, UiState<List<PackingItem>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error)
                        Log.w(TAG, "Error observing packing items", root)
                        emit(UiState.Error(root.message ?: "Failed to load checklist"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun unwrapFirestoreError(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    fun selectTrip(tripId: String?) {
        _activeTripId.value = tripId
    }

    fun toggleWeightUnit() {
        _weightUnit.value = when (_weightUnit.value) {
            WeightUnit.METRIC -> WeightUnit.IMPERIAL
            WeightUnit.IMPERIAL -> WeightUnit.METRIC
        }
    }

    fun updateGearSearch(query: String) {
        _gearSearchQuery.value = query
    }

    fun selectGearCategoryFilter(category: GearCategory?) {
        _gearCategoryFilter.value = category
    }

    fun toggleShowArchivedTrips() {
        _showArchivedTrips.value = !_showArchivedTrips.value
    }

    // --- Gear Actions ---

    fun saveGearItem(
        existingId: String?,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantityOwned: Long,
        essential: Boolean,
        notes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (existingId == null) {
                repository.addGearItem(
                    currentUserId, name, category, weightGrams, quantityOwned, essential, notes
                )
            } else {
                repository.updateGearItem(
                    currentUserId, existingId, name, category, weightGrams, quantityOwned, essential, notes
                )
            }
            result.onSuccess {
                _snackbarMessage.tryEmit(if (existingId == null) "Added to Gear Locker" else "Gear updated")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to save gear")
            }
        }
    }

    fun deleteGearItem(gearId: String) {
        viewModelScope.launch {
            repository.deleteGearItem(currentUserId, gearId)
                .onSuccess { _snackbarMessage.tryEmit("Gear removed") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun loadStarterGear() {
        viewModelScope.launch {
            repository.seedStarterGear(currentUserId)
                .onSuccess { _snackbarMessage.tryEmit("Starter gear loaded into locker") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Could not load starter gear") }
        }
    }

    // --- Trip Actions ---

    fun createTrip(
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        startDateLabel: String,
        notes: String?,
        includeStarterChecklist: Boolean,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createTrip(
                currentUserId, title, location, tripType, nights, startDateLabel, notes, includeStarterChecklist
            ).onSuccess { tripId ->
                _snackbarMessage.tryEmit("Trip created")
                onCreated(tripId)
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Could not create trip")
            }
        }
    }

    fun updateTrip(
        tripId: String,
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        startDateLabel: String,
        notes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateTrip(
                currentUserId, tripId, title, location, tripType, nights, startDateLabel, notes
            ).onSuccess {
                _snackbarMessage.tryEmit("Trip updated")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Update failed")
            }
        }
    }

    fun setTripArchived(tripId: String, archived: Boolean) {
        viewModelScope.launch {
            repository.setTripArchived(currentUserId, tripId, archived)
                .onSuccess {
                    _snackbarMessage.tryEmit(if (archived) "Trip archived" else "Trip restored")
                }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun deleteTrip(tripId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTrip(currentUserId, tripId)
                .onSuccess {
                    if (_activeTripId.value == tripId) _activeTripId.value = null
                    _snackbarMessage.tryEmit("Trip deleted")
                    onDeleted()
                }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Delete failed") }
        }
    }

    // --- Packing Checklist Actions ---

    fun savePackingItem(
        tripId: String,
        existingItemId: String?,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantity: Long,
        worn: Boolean,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = if (existingItemId == null) {
                repository.addPackingItem(
                    currentUserId, tripId, name, category, weightGrams, quantity, worn
                )
            } else {
                repository.updatePackingItem(
                    currentUserId, tripId, existingItemId, name, category, weightGrams, quantity, worn
                )
            }
            result.onSuccess {
                _snackbarMessage.tryEmit(if (existingItemId == null) "Item added to pack" else "Item updated")
                onDone()
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Failed to save item")
            }
        }
    }

    fun importGearToTrip(tripId: String, gearItems: List<GearItem>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addGearItemsToTrip(currentUserId, tripId, gearItems)
                .onSuccess {
                    _snackbarMessage.tryEmit("Added ${gearItems.size} item(s) from locker")
                    onDone()
                }
                .onFailure { err ->
                    _snackbarMessage.tryEmit(err.localizedMessage ?: "Import failed")
                }
        }
    }

    fun togglePackingItemPacked(tripId: String, itemId: String, packed: Boolean) {
        viewModelScope.launch {
            repository.togglePackingItemPacked(currentUserId, tripId, itemId, packed)
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Could not update item") }
        }
    }

    fun deletePackingItem(tripId: String, itemId: String) {
        viewModelScope.launch {
            repository.deletePackingItem(currentUserId, tripId, itemId)
                .onSuccess { _snackbarMessage.tryEmit("Removed from checklist") }
                .onFailure { _snackbarMessage.tryEmit(it.localizedMessage ?: "Remove failed") }
        }
    }

    private companion object {
        const val TAG = "CampViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
