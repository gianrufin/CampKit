package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CampTrip
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.PackingItem
import com.example.model.TripType
import com.example.model.WeightUnit
import com.example.model.formatWeightShort
import kotlin.math.min

fun categoryIcon(category: GearCategory): ImageVector = when (category) {
    GearCategory.SHELTER -> Icons.Rounded.Forest
    GearCategory.SLEEP -> Icons.Rounded.Bedtime
    GearCategory.COOKING -> Icons.Rounded.LocalFireDepartment
    GearCategory.CLOTHING -> Icons.Rounded.Checkroom
    GearCategory.HYDRATION -> Icons.Rounded.WaterDrop
    GearCategory.SAFETY -> Icons.Rounded.MedicalServices
    GearCategory.TOOLS -> Icons.Rounded.Build
    GearCategory.OTHER -> Icons.Rounded.Backpack
}

fun tripTypeIcon(tripType: TripType): ImageVector = when (tripType) {
    TripType.BACKPACKING -> Icons.Rounded.Terrain
    TripType.CAR_CAMPING -> Icons.Rounded.DirectionsCar
    TripType.BIKEPACKING -> Icons.AutoMirrored.Rounded.DirectionsBike
    TripType.DAY_HIKE -> Icons.AutoMirrored.Rounded.DirectionsWalk
}

@Composable
fun CategoryBadge(category: GearCategory, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = category.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = category.shortCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PackProgressRing(
    progress: Float,
    packedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeColor = if (clamped >= 1f && totalCount > 0) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 6.dp.toPx()
            val diameter = min(size.width, size.height) - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$packedCount/$totalCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuantityStepper(
    label: String,
    value: Long,
    minValue: Long,
    maxValue: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > minValue) onValueChange(value - 1) },
                enabled = value > minValue,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label")
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { if (value < maxValue) onValueChange(value + 1) },
                enabled = value < maxValue,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
fun TripFormDialog(
    initialTrip: CampTrip?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        location: String,
        tripType: TripType,
        nights: Long,
        dateLabel: String,
        notes: String?,
        includeStarter: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTrip?.title ?: "") }
    var location by remember { mutableStateOf(initialTrip?.location ?: "") }
    var tripType by remember { mutableStateOf(initialTrip?.tripTypeEnum ?: TripType.BACKPACKING) }
    var nights by remember { mutableLongStateOf(initialTrip?.nights ?: 2L) }
    var dateLabel by remember { mutableStateOf(initialTrip?.startDateLabel ?: "") }
    var notes by remember { mutableStateOf(initialTrip?.notes ?: "") }
    var includeStarter by remember { mutableStateOf(initialTrip == null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (initialTrip == null) R.string.new_trip else R.string.edit_trip),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it; validationError = null },
                    label = { Text("Trip Name *") },
                    placeholder = { Text("e.g., Desolation Wilderness Loop") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_title_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 100) location = it },
                    label = { Text("Campsite / Trail") },
                    placeholder = { Text("e.g., Lake Tahoe, CA") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_location_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trip Style", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TripType.entries.forEach { type ->
                            FilterChip(
                                selected = tripType == type,
                                onClick = { tripType = type },
                                label = { Text(type.label) }
                            )
                        }
                    }
                }

                QuantityStepper(
                    label = "Nights",
                    value = nights,
                    minValue = 0L,
                    maxValue = 365L,
                    onValueChange = { nights = it }
                )

                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { if (it.length <= 40) dateLabel = it },
                    label = { Text("Dates") },
                    placeholder = { Text("e.g., Nov 28 – Nov 30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = { Text("Permits & Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (initialTrip == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Preload starter checklist", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Adds curated ${tripType.label.lowercase()} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeStarter,
                            onCheckedChange = { includeStarter = it },
                            modifier = Modifier.testTag("trip_starter_switch")
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Please enter a trip name."
                    } else {
                        onSave(trimmed, location, tripType, nights, dateLabel, notes, includeStarter)
                    }
                },
                modifier = Modifier.testTag("save_trip_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun GearFormDialog(
    initialGear: GearItem?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantityOwned: Long,
        essential: Boolean,
        notes: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialGear?.name ?: "") }
    var category by remember { mutableStateOf(initialGear?.categoryEnum ?: GearCategory.SHELTER) }
    var weightText by remember { mutableStateOf(initialGear?.weightGrams?.toString() ?: "") }
    var quantityOwned by remember { mutableLongStateOf(initialGear?.quantityOwned ?: 1L) }
    var essential by remember { mutableStateOf(initialGear?.essential ?: false) }
    var notes by remember { mutableStateOf(initialGear?.notes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(if (initialGear == null) R.string.add_gear else R.string.edit_gear),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it; validationError = null },
                    label = { Text("Gear Name *") },
                    placeholder = { Text("e.g., 2P Ultralight Tent") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gear_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GearCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        weightText = input.filter { it.isDigit() }.take(6)
                        validationError = null
                    },
                    label = { Text("Unit Weight (grams)") },
                    placeholder = { Text("e.g., 1190") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gear_weight_input")
                )

                QuantityStepper(
                    label = "Quantity Owned",
                    value = quantityOwned,
                    minValue = 1L,
                    maxValue = 100L,
                    onValueChange = { quantityOwned = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Core Essential", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = essential,
                        onCheckedChange = { essential = it }
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 300) notes = it },
                    label = { Text("Notes / Specs") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    val weight = weightText.toLongOrNull() ?: 0L
                    when {
                        trimmed.isEmpty() -> validationError = "Please enter a gear name."
                        weight !in 0L..500_000L -> validationError = "Weight must be 0–500,000 g."
                        else -> onSave(trimmed, category, weight, quantityOwned, essential, notes)
                    }
                },
                modifier = Modifier.testTag("save_gear_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun PackingItemFormDialog(
    initialItem: PackingItem?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: GearCategory,
        weightGrams: Long,
        quantity: Long,
        worn: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var category by remember { mutableStateOf(initialItem?.categoryEnum ?: GearCategory.OTHER) }
    var weightText by remember { mutableStateOf(initialItem?.weightGrams?.toString() ?: "") }
    var quantity by remember { mutableLongStateOf(initialItem?.quantity ?: 1L) }
    var worn by remember { mutableStateOf(initialItem?.worn ?: false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) stringResource(R.string.add_packing_item) else "Edit Item",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it; validationError = null },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("packing_item_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GearCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Unit Weight (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("packing_item_weight_input")
                )

                QuantityStepper(
                    label = "Quantity",
                    value = quantity,
                    minValue = 1L,
                    maxValue = 100L,
                    onValueChange = { quantity = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Worn / Carried", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Excluded from pack base weight",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = worn,
                        onCheckedChange = { worn = it }
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    val weight = weightText.toLongOrNull() ?: 0L
                    when {
                        trimmed.isEmpty() -> validationError = "Please enter an item name."
                        weight !in 0L..500_000L -> validationError = "Weight must be 0–500,000 g."
                        else -> onSave(trimmed, category, weight, quantity, worn)
                    }
                },
                modifier = Modifier.testTag("save_packing_item_button")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ImportFromLockerDialog(
    gearItems: List<GearItem>,
    weightUnit: WeightUnit,
    onDismiss: () -> Unit,
    onImport: (List<GearItem>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.import_from_gear), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            if (gearItems.isEmpty()) {
                Text(
                    text = "Your Gear Locker has no items yet. Add gear in the Gear Locker tab first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(gearItems, key = { it.id }) { gear ->
                        val checked = selectedIds.contains(gear.id)
                        Surface(
                            color = if (checked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    if (checked) selectedIds.remove(gear.id) else selectedIds.add(gear.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedIds.contains(gear.id)) selectedIds.add(gear.id)
                                        } else {
                                            selectedIds.remove(gear.id)
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gear.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${gear.categoryEnum.label} • ${formatWeightShort(gear.weightGrams, weightUnit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = gearItems.filter { selectedIds.contains(it.id) }
                    onImport(chosen)
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.testTag("confirm_import_gear_button")
            ) {
                Text("Add (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
