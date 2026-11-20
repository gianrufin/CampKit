package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.WeightUnit
import com.example.model.formatWeight
import com.example.model.formatWeightShort
import com.example.ui.components.CategoryBadge
import com.example.ui.components.GearFormDialog
import com.example.ui.viewmodel.UiState

@Composable
fun GearLockerScreen(
    gearState: UiState<List<GearItem>>,
    searchQuery: String,
    selectedCategory: GearCategory?,
    weightUnit: WeightUnit,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (GearCategory?) -> Unit,
    onSaveGear: (
        existingId: String?,
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantityOwned: Long,
        essential: Boolean,
        notes: String?
    ) -> Unit,
    onDeleteGear: (GearItem) -> Unit,
    onLoadStarterGear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGear by remember { mutableStateOf<GearItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (gearState) {
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
                            text = gearState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val allGear = gearState.data
                val filteredGear = remember(allGear, searchQuery, selectedCategory) {
                    allGear.filter { item ->
                        val matchesCat = selectedCategory == null || item.categoryEnum == selectedCategory
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            item.name.contains(q, ignoreCase = true) ||
                            (item.notes?.contains(q, ignoreCase = true) == true)
                        matchesCat && matchesQuery
                    }
                }
                val totalGrams = remember(allGear) {
                    allGear.sumOf { it.weightGrams * it.quantityOwned }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "GEAR LOCKER INVENTORY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${allGear.size} Items • ${formatWeight(totalGrams, weightUnit)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Backpack,
                                            contentDescription = stringResource(R.string.gear_title),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (allGear.isNotEmpty()) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text(stringResource(R.string.search_gear_hint)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchChange("") }) {
                                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gear_search_input")
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { onSelectCategory(null) },
                                    label = { Text("All") }
                                )
                                GearCategory.entries.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = {
                                            onSelectCategory(if (selectedCategory == cat) null else cat)
                                        },
                                        label = { Text(cat.label) }
                                    )
                                }
                            }
                        }
                    }

                    if (allGear.isEmpty()) {
                        item {
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
                                    Text(
                                        text = stringResource(R.string.gear_empty_title),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.gear_empty_body),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { showAddDialog = true },
                                            modifier = Modifier.testTag("empty_add_gear_button")
                                        ) {
                                            Text(stringResource(R.string.add_gear))
                                        }
                                        OutlinedButton(
                                            onClick = onLoadStarterGear,
                                            modifier = Modifier.testTag("load_starter_gear_button")
                                        ) {
                                            Text(stringResource(R.string.load_starter_gear))
                                        }
                                    }
                                }
                            }
                        }
                    } else if (filteredGear.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No gear matches your current filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredGear, key = { it.id }) { gear ->
                            GearCard(
                                gear = gear,
                                weightUnit = weightUnit,
                                onClick = { editingGear = gear },
                                onDelete = { onDeleteGear(gear) }
                            )
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
                .testTag("add_gear_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_gear))
        }
    }

    if (showAddDialog) {
        GearFormDialog(
            initialGear = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, weight, qty, essential, notes ->
                onSaveGear(null, name, cat, weight, qty, essential, notes)
                showAddDialog = false
            }
        )
    }

    editingGear?.let { gear ->
        GearFormDialog(
            initialGear = gear,
            onDismiss = { editingGear = null },
            onSave = { name, cat, weight, qty, essential, notes ->
                onSaveGear(gear.id, name, cat, weight, qty, essential, notes)
                editingGear = null
            }
        )
    }
}

@Composable
private fun GearCard(
    gear: GearItem,
    weightUnit: WeightUnit,
    onClick: () -> Unit,
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
            .testTag("gear_card_${gear.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = gear.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (gear.essential) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Core essential",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBadge(category = gear.categoryEnum)
                    Text(
                        text = formatWeightShort(gear.weightGrams, weightUnit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (gear.quantityOwned > 1L) {
                        Text(
                            text = "x${gear.quantityOwned}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!gear.notes.isNullOrBlank()) {
                    Text(
                        text = gear.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${gear.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
