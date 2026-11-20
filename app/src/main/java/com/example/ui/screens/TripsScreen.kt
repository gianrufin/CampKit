package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CampTrip
import com.example.model.TripType
import com.example.ui.components.TripFormDialog
import com.example.ui.components.tripTypeIcon
import com.example.ui.viewmodel.UiState

@Composable
fun TripsScreen(
    tripsState: UiState<List<CampTrip>>,
    showArchived: Boolean,
    onToggleShowArchived: () -> Unit,
    onOpenTrip: (CampTrip) -> Unit,
    onCreateTrip: (
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        dateLabel: String,
        notes: String?,
        includeStarter: Boolean
    ) -> Unit,
    onUpdateTrip: (
        tripId: String,
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        dateLabel: String,
        notes: String?
    ) -> Unit,
    onToggleArchive: (CampTrip) -> Unit,
    onDeleteTrip: (CampTrip) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<CampTrip?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (tripsState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = tripsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val allTrips = tripsState.data
                val visibleTrips = remember(allTrips, showArchived) {
                    allTrips.filter { it.archived == showArchived }
                }
                val archivedCount = remember(allTrips) { allTrips.count { it.archived } }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TripsBannerHeader(
                            activeCount = allTrips.count { !it.archived },
                            archivedCount = archivedCount,
                            showArchived = showArchived,
                            onToggleShowArchived = onToggleShowArchived
                        )
                    }

                    if (visibleTrips.isEmpty()) {
                        item {
                            EmptyTripsCard(
                                showArchived = showArchived,
                                onCreateClick = { showCreateDialog = true },
                                onQuickBackpacking = {
                                    onCreateTrip(
                                        "Alpine Ridge Weekend",
                                        "High Sierra Trail",
                                        TripType.BACKPACKING,
                                        2L,
                                        "Upcoming Weekend",
                                        "Bear canister required above 9,000 ft.",
                                        true
                                    )
                                }
                            )
                        }
                    } else {
                        items(visibleTrips, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                onClick = { onOpenTrip(trip) },
                                onEdit = { editingTrip = trip },
                                onToggleArchive = { onToggleArchive(trip) },
                                onDelete = { onDeleteTrip(trip) }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_trip_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.new_trip))
        }
    }

    if (showCreateDialog) {
        TripFormDialog(
            initialTrip = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, location, type, nights, dateLabel, notes, starter ->
                onCreateTrip(title, location, type, nights, dateLabel, notes, starter)
                showCreateDialog = false
            }
        )
    }

    editingTrip?.let { trip ->
        TripFormDialog(
            initialTrip = trip,
            onDismiss = { editingTrip = null },
            onSave = { title, location, type, nights, dateLabel, notes, _ ->
                onUpdateTrip(trip.id, title, location, type, nights, dateLabel, notes)
                editingTrip = null
            }
        )
    }
}

@Composable
private fun TripsBannerHeader(
    activeCount: Int,
    archivedCount: Int,
    showArchived: Boolean,
    onToggleShowArchived: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_camp_hero),
                contentDescription = stringResource(R.string.trips_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xD9111714), Color(0x44111714))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EXPEDITION LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE9C77B)
                )
                Text(
                    text = if (activeCount == 1) "1 Active Trip" else "$activeCount Active Trips",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Tap any trip to pack & weigh your kit",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDDE5DF)
                )
            }
        }

        if (archivedCount > 0 || showArchived) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { if (showArchived) onToggleShowArchived() },
                    label = { Text("Active ($activeCount)") }
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { if (!showArchived) onToggleShowArchived() },
                    label = { Text("${stringResource(R.string.show_archived)} ($archivedCount)") }
                )
            }
        }
    }
}

@Composable
private fun EmptyTripsCard(
    showArchived: Boolean,
    onCreateClick: () -> Unit,
    onQuickBackpacking: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = stringResource(R.string.trips_empty_title),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = if (showArchived) "No archived trips" else stringResource(R.string.trips_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (showArchived) {
                    "Completed trips you archive will appear here."
                } else {
                    stringResource(R.string.trips_empty_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!showArchived) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Button(
                        onClick = onCreateClick,
                        modifier = Modifier.testTag("empty_new_trip_button")
                    ) {
                        Text(stringResource(R.string.new_trip))
                    }
                    OutlinedButton(
                        onClick = onQuickBackpacking,
                        modifier = Modifier.testTag("quick_starter_trip_button")
                    ) {
                        Text("Sample Kit")
                    }
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: CampTrip,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("trip_card_${trip.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tripTypeIcon(trip.tripTypeEnum),
                            contentDescription = trip.tripTypeEnum.label,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = trip.tripTypeEnum.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_trip),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleArchive,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (trip.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = stringResource(
                                if (trip.archived) R.string.unarchive_trip else R.string.archive_trip
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete_trip),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trip.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = trip.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = "Nights",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (trip.nights == 1L) "1 night" else "${trip.nights} nights",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (trip.startDateLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = trip.startDateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!trip.notes.isNullOrBlank()) {
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
