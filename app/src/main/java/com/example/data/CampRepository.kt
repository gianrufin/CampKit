package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CampTrip
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.PackingItem
import com.example.model.StarterKits
import com.example.model.TripType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CampRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireVerifiedUid(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Sign in with Google before accessing CampKit data.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Authenticated user changed; refusing cross-user access." }
        }
        return uid
    }

    fun observeGear(userId: String): Flow<List<GearItem>> = flow {
        val uid = requireVerifiedUid(userId)
        val path = "users/$uid/gear"
        emitAll(
            db.collection("users").document(uid).collection("gear")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GearItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<GearItem> { it.category }.thenBy { it.name.lowercase() })
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeTrips(userId: String): Flow<List<CampTrip>> = flow {
        val uid = requireVerifiedUid(userId)
        val path = "users/$uid/trips"
        emitAll(
            db.collection("users").document(uid).collection("trips")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampTrip::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observePackingItems(userId: String, tripId: String): Flow<List<PackingItem>> = flow {
        val uid = requireVerifiedUid(userId)
        require(isValidDocumentId(tripId)) { "Invalid trip ID." }
        val path = "users/$uid/trips/$tripId/items"
        emitAll(
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PackingItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<PackingItem> { it.packed }.thenBy { it.category }.thenBy { it.name.lowercase() })
                }
                .catch { error ->
                    handleStreamError(error, OperationType.LIST, path, uid)
                }
        )
    }

    private fun handleStreamError(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    // --- Gear Locker Mutations ---

    suspend fun addGearItem(
        userId: String,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantityOwned: Long,
        essential: Boolean,
        notes: String?
    ): Result<Unit> {
        val docPath = "users/$userId/gear"
        return try {
            val uid = requireVerifiedUid(userId)
            val cleanName = name.trim()
            require(cleanName.length in 1..80) { "Gear name must be 1–80 characters." }
            require(weightGrams in 0L..500_000L) { "Weight must be 0–500,000 g." }
            require(quantityOwned in 1L..100L) { "Quantity must be 1–100." }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300)

            val docRef = db.collection("users").document(uid).collection("gear").document()
            val payload = buildMap<String, Any> {
                put("name", cleanName)
                put("category", category.name)
                put("weightGrams", weightGrams)
                put("quantityOwned", quantityOwned)
                put("essential", essential)
                if (cleanNotes != null) put("notes", cleanNotes)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateGearItem(
        userId: String,
        gearId: String,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantityOwned: Long,
        essential: Boolean,
        notes: String?
    ): Result<Unit> {
        val docPath = "users/$userId/gear/$gearId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(gearId)) { "Invalid gear ID." }
            val cleanName = name.trim()
            require(cleanName.length in 1..80) { "Gear name must be 1–80 characters." }
            require(weightGrams in 0L..500_000L) { "Weight must be 0–500,000 g." }
            require(quantityOwned in 1L..100L) { "Quantity must be 1–100." }
            val cleanNotes = notes?.trim()?.take(300)

            val docRef = db.collection("users").document(uid).collection("gear").document(gearId)
            val updates = buildMap<String, Any> {
                put("name", cleanName)
                put("category", category.name)
                put("weightGrams", weightGrams)
                put("quantityOwned", quantityOwned)
                put("essential", essential)
                if (!cleanNotes.isNullOrEmpty()) {
                    put("notes", cleanNotes)
                } else {
                    put("notes", FieldValue.delete())
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGearItem(userId: String, gearId: String): Result<Unit> {
        val docPath = "users/$userId/gear/$gearId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(gearId)) { "Invalid gear ID." }
            db.collection("users").document(uid).collection("gear").document(gearId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedStarterGear(userId: String): Result<Unit> {
        val path = "users/$userId/gear"
        return try {
            val uid = requireVerifiedUid(userId)
            val gearCol = db.collection("users").document(uid).collection("gear")
            val batch = db.batch()
            StarterKits.lockerDefaults.forEach { template ->
                val ref = gearCol.document()
                val payload = buildMap<String, Any> {
                    put("name", template.name.take(80))
                    put("category", template.category.name)
                    put("weightGrams", template.weightGrams)
                    put("quantityOwned", template.quantity)
                    put("essential", template.essential)
                    template.notes?.take(300)?.let { put("notes", it) }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(ref, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Camp Trips Mutations ---

    suspend fun createTrip(
        userId: String,
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        startDateLabel: String,
        notes: String?,
        includeStarterChecklist: Boolean
    ): Result<String> {
        val path = "users/$userId/trips"
        return try {
            val uid = requireVerifiedUid(userId)
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..80) { "Trip title must be 1–80 characters." }
            val cleanLocation = location.trim().take(100)
            val safeNights = nights.coerceIn(0L, 365L)
            val cleanDate = startDateLabel.trim().take(40)
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

            val tripRef = db.collection("users").document(uid).collection("trips").document()
            val batch = db.batch()
            val tripPayload = buildMap<String, Any> {
                put("title", cleanTitle)
                put("location", cleanLocation)
                put("tripType", tripType.name)
                put("nights", safeNights)
                put("startDateLabel", cleanDate)
                if (cleanNotes != null) put("notes", cleanNotes)
                put("archived", false)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            batch.set(tripRef, tripPayload)

            if (includeStarterChecklist) {
                val itemsCol = tripRef.collection("items")
                StarterKits.itemsForTripType(tripType).forEach { template ->
                    val itemRef = itemsCol.document()
                    val itemPayload = mapOf(
                        "name" to template.name.take(80),
                        "category" to template.category.name,
                        "weightGrams" to template.weightGrams,
                        "quantity" to template.quantity,
                        "packed" to false,
                        "worn" to template.worn,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(itemRef, itemPayload)
                }
            }

            batch.commit().await()
            Result.success(tripRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTrip(
        userId: String,
        tripId: String,
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        startDateLabel: String,
        notes: String?
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId)) { "Invalid trip ID." }
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..80) { "Trip title must be 1–80 characters." }
            val cleanLocation = location.trim().take(100)
            val safeNights = nights.coerceIn(0L, 365L)
            val cleanDate = startDateLabel.trim().take(40)
            val cleanNotes = notes?.trim()?.take(500)

            val docRef = db.collection("users").document(uid).collection("trips").document(tripId)
            val updates = buildMap<String, Any> {
                put("title", cleanTitle)
                put("location", cleanLocation)
                put("tripType", tripType.name)
                put("nights", safeNights)
                put("startDateLabel", cleanDate)
                if (!cleanNotes.isNullOrEmpty()) {
                    put("notes", cleanNotes)
                } else {
                    put("notes", FieldValue.delete())
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setTripArchived(userId: String, tripId: String, archived: Boolean): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId)) { "Invalid trip ID." }
            db.collection("users").document(uid).collection("trips").document(tripId)
                .update(
                    mapOf(
                        "archived" to archived,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(userId: String, tripId: String): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId)) { "Invalid trip ID." }
            val tripRef = db.collection("users").document(uid).collection("trips").document(tripId)
            val existingItems = tripRef.collection("items").get().await()
            val batch = db.batch()
            existingItems.documents.forEach { batch.delete(it.reference) }
            batch.delete(tripRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Packing Items Mutations ---

    suspend fun addPackingItem(
        userId: String,
        tripId: String,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantity: Long,
        worn: Boolean,
        gearId: String? = null
    ): Result<Unit> {
        val path = "users/$userId/trips/$tripId/items"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId)) { "Invalid trip ID." }
            val cleanName = name.trim()
            require(cleanName.length in 1..80) { "Item name must be 1–80 characters." }
            require(weightGrams in 0L..500_000L) { "Weight must be 0–500,000 g." }
            require(quantity in 1L..100L) { "Quantity must be 1–100." }
            val validGearId = gearId?.takeIf { isValidDocumentId(it) }

            val itemRef = db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document()
            val payload = buildMap<String, Any> {
                put("name", cleanName)
                put("category", category.name)
                put("weightGrams", weightGrams)
                put("quantity", quantity)
                put("packed", false)
                put("worn", worn)
                if (validGearId != null) put("gearId", validGearId)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            itemRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addGearItemsToTrip(
        userId: String,
        tripId: String,
        gearItems: List<GearItem>
    ): Result<Unit> {
        val path = "users/$userId/trips/$tripId/items"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId)) { "Invalid trip ID." }
            if (gearItems.isEmpty()) return Result.success(Unit)

            val itemsCol = db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items")
            val batch = db.batch()
            gearItems.forEach { gear ->
                val ref = itemsCol.document()
                val payload = buildMap<String, Any> {
                    put("name", gear.name.trim().take(80))
                    put("category", gear.categoryEnum.name)
                    put("weightGrams", gear.weightGrams.coerceIn(0L, 500_000L))
                    put("quantity", gear.quantityOwned.coerceIn(1L, 100L))
                    put("packed", false)
                    put("worn", false)
                    if (isValidDocumentId(gear.id)) put("gearId", gear.id)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(ref, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePackingItemPacked(
        userId: String,
        tripId: String,
        itemId: String,
        packed: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/items/$itemId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId) && isValidDocumentId(itemId)) { "Invalid ID." }
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document(itemId)
                .update(
                    mapOf(
                        "packed" to packed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePackingItem(
        userId: String,
        tripId: String,
        itemId: String,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantity: Long,
        worn: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/items/$itemId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId) && isValidDocumentId(itemId)) { "Invalid ID." }
            val cleanName = name.trim()
            require(cleanName.length in 1..80) { "Item name must be 1–80 characters." }
            require(weightGrams in 0L..500_000L) { "Weight must be 0–500,000 g." }
            require(quantity in 1L..100L) { "Quantity must be 1–100." }

            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document(itemId)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "category" to category.name,
                        "weightGrams" to weightGrams,
                        "quantity" to quantity,
                        "worn" to worn,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePackingItem(
        userId: String,
        tripId: String,
        itemId: String
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/items/$itemId"
        return try {
            val uid = requireVerifiedUid(userId)
            require(isValidDocumentId(tripId) && isValidDocumentId(itemId)) { "Invalid ID." }
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document(itemId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun isValidDocumentId(id: String): Boolean =
        id.length in 1..128 && ID_REGEX.matches(id)

    private companion object {
        val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
