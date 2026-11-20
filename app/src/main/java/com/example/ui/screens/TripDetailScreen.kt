package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CampTrip
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.PackingItem
import com.example.model.WeightUnit
import com.example.model.formatWeight
import com.example.model.formatWeightShort
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ImportFromLockerDialog
import com.example.ui.components.PackProgressRing
import com.example.ui.components.PackingItemFormDialog
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: CampTrip?,
    packingItemsState: UiState<List<PackingItem>>,
    lockerGear: List<GearItem>,
    weightUnit: WeightUnit,
    onBack: () -> Unit,
    onTogglePacked: (PackingItem) -> Unit,
    onSavePackingItem: (
        existingItemId: String?,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantity: Long,
        worn: Boolean
    ) -> Unit,
    onImportFromLocker: (List<GearItem>) -> Unit,
    onDeletePackingItem: (PackingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PackingItem?>(null) }
    var selectedCategory by remember { mutableStateOf<GearCategory?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trip?.title ?: stringResource(R.string.checklist_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (trip != null && trip.location.isNotBlank()) {
                            Text(
                                text = "${trip.location} • ${trip.nights}n",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("trip_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to trips"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            when (packingItemsState) {
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
                                text = packingItemsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val items = packingItemsState.data
                    val filteredItems = remember(items, selectedCategory) {
                        if (selectedCategory == null) items else items.filter { it.categoryEnum == selectedCategory }
                    }
                    val packedCount = remember(items) { items.count { it.packed } }
                    val baseWeightGrams = remember(items) { items.filter { !it.worn }.sumOf { it.totalGrams } }
                    val wornWeightGrams = remember(items) { items.filter { it.worn }.sumOf { it.totalGrams } }
                    val totalWeightGrams = baseWeightGrams + wornWeightGrams
                    val progress = if (items.isEmpty()) 0f else packedCount.toFloat() / items.size.toFloat()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            PackProgressRing(
                                                progress = progress,
                                                packedCount = packedCount,
                                                totalCount = items.size
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = stringResource(R.string.base_weight_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = formatWeight(baseWeightGrams, weightUnit),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Total ${formatWeight(totalWeightGrams, weightUnit)} • Worn ${formatWeightShort(wornWeightGrams, weightUnit)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showImportDialog = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("import_from_locker_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Backpack,
                                                contentDescription = stringResource(R.string.import_from_gear),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.import_from_gear), maxLines = 1)
                                        }
                                        Button(
                                            onClick = { showAddDialog = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("add_custom_item_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.add_packing_item),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.add_packing_item), maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        if (items.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text("All (${items.size})") }
                                    )
                                    GearCategory.entries.forEach { cat ->
                                        val count = items.count { it.categoryEnum == cat }
                                        if (count > 0) {
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = {
                                                    selectedCategory = if (selectedCategory == cat) null else cat
                                                },
                                                label = { Text("${cat.label} ($count)") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredItems.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.checklist_empty_title),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = stringResource(R.string.checklist_empty_body),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredItems, key = { it.id }) { item ->
                                PackingItemRow(
                                    item = item,
                                    weightUnit = weightUnit,
                                    onToggle = { onTogglePacked(item) },
                                    onEdit = { editingItem = item },
                                    onDelete = { onDeletePackingItem(item) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("trip_detail_add_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_packing_item))
        }
    }

    if (showAddDialog) {
        PackingItemFormDialog(
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, category, weightGrams, qty, worn ->
                onSavePackingItem(null, name, category, weightGrams, qty, worn)
                showAddDialog = false
            }
        )
    }

    editingItem?.let { item ->
        PackingItemFormDialog(
            initialItem = item,
            onDismiss = { editingItem = null },
            onSave = { name, category, weightGrams, qty, worn ->
                onSavePackingItem(item.id, name, category, weightGrams, qty, worn)
                editingItem = null
            }
        )
    }

    if (showImportDialog) {
        ImportFromLockerDialog(
            gearItems = lockerGear,
            weightUnit = weightUnit,
            onDismiss = { showImportDialog = false },
            onImport = { chosen ->
                onImportFromLocker(chosen)
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun PackingItemRow(
    item: PackingItem,
    weightUnit: WeightUnit,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (item.packed) {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (item.packed) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .testTag("packing_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.packed,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("packing_checkbox_${item.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (item.quantity > 1L) "${item.quantity}x ${item.name}" else item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = if (item.packed) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.packed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.worn) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "WORN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(category = item.categoryEnum)
                    Text(
                        text = formatWeightShort(item.totalGrams, weightUnit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit ${item.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${item.name}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
