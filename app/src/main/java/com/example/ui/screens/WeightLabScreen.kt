package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CampTrip
import com.example.model.GearCategory
import com.example.model.GearItem
import com.example.model.PackingItem
import com.example.model.TripType
import com.example.model.WeightUnit
import com.example.model.formatWeight
import com.example.ui.viewmodel.UiState

private data class CategoryWeightEntry(
    val category: GearCategory,
    val grams: Long
)

@Composable
fun WeightLabScreen(
    tripsState: UiState<List<CampTrip>>,
    activeTripId: String?,
    packingItemsState: UiState<List<PackingItem>>,
    gearState: UiState<List<GearItem>>,
    weightUnit: WeightUnit,
    onSelectTripForAnalysis: (String?) -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trips = (tripsState as? UiState.Success)?.data.orEmpty().filter { !it.archived }
    val selectedTrip = trips.firstOrNull { it.id == activeTripId }
    val packingItems = (packingItemsState as? UiState.Success)?.data.orEmpty()
    val lockerItems = (gearState as? UiState.Success)?.data.orEmpty()

    val analyzingTrip = selectedTrip != null
    val baseGrams = remember(analyzingTrip, packingItems, lockerItems) {
        if (analyzingTrip) {
            packingItems.filter { !it.worn }.sumOf { it.totalGrams }
        } else {
            lockerItems.sumOf { it.weightGrams * it.quantityOwned }
        }
    }
    val wornGrams = remember(analyzingTrip, packingItems) {
        if (analyzingTrip) packingItems.filter { it.worn }.sumOf { it.totalGrams } else 0L
    }
    val totalGrams = baseGrams + wornGrams

    val categoryBreakdown = remember(analyzingTrip, packingItems, lockerItems) {
        val grouped = if (analyzingTrip) {
            packingItems.groupBy { it.categoryEnum }.mapValues { (_, list) -> list.sumOf { it.totalGrams } }
        } else {
            lockerItems.groupBy { it.categoryEnum }.mapValues { (_, list) -> list.sumOf { it.weightGrams * it.quantityOwned } }
        }
        grouped.entries
            .map { CategoryWeightEntry(it.key, it.value) }
            .filter { it.grams > 0L }
            .sortedByDescending { it.grams }
    }

    val topHeavyItems = remember(analyzingTrip, packingItems, lockerItems) {
        if (analyzingTrip) {
            packingItems.sortedByDescending { it.totalGrams }.take(5).map { it.name to it.totalGrams }
        } else {
            lockerItems.sortedByDescending { it.weightGrams * it.quantityOwned }
                .take(5)
                .map { it.name to (it.weightGrams * it.quantityOwned) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.weight_lab_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.weight_lab_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilterChip(
                    selected = weightUnit == WeightUnit.IMPERIAL,
                    onClick = onToggleUnit,
                    label = {
                        Text(
                            if (weightUnit == WeightUnit.METRIC) {
                                stringResource(R.string.unit_metric)
                            } else {
                                stringResource(R.string.unit_imperial)
                            }
                        )
                    },
                    modifier = Modifier.testTag("unit_toggle_chip")
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !analyzingTrip,
                    onClick = { onSelectTripForAnalysis(null) },
                    label = { Text("Full Gear Locker") }
                )
                trips.forEach { trip ->
                    FilterChip(
                        selected = activeTripId == trip.id,
                        onClick = { onSelectTripForAnalysis(trip.id) },
                        label = { Text(trip.title) }
                    )
                }
            }
        }

        item {
            val targetGrams = selectedTrip?.tripTypeEnum?.targetBaseGrams ?: TripType.BACKPACKING.targetBaseGrams
            val ratio = if (targetGrams > 0L) (baseGrams.toFloat() / targetGrams.toFloat()).coerceIn(0f, 1.4f) else 0f

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (selectedTrip != null) {
                            "${selectedTrip.title.uppercase()} • BASE WEIGHT"
                        } else {
                            "LOCKER TOTAL WEIGHT"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = formatWeight(baseGrams, weightUnit),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedTrip != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Target ${formatWeight(targetGrams, weightUnit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Worn ${formatWeight(wornGrams, weightUnit)} • Total ${formatWeight(totalGrams, weightUnit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (selectedTrip != null) {
                        val barTrack = MaterialTheme.colorScheme.surfaceContainerHigh
                        val barFill = if (baseGrams <= targetGrams) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                        ) {
                            val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            drawRoundRect(
                                color = barTrack,
                                topLeft = Offset.Zero,
                                size = size,
                                cornerRadius = corner
                            )
                            val fillWidth = (size.width * ratio.coerceAtMost(1f)).coerceAtLeast(0f)
                            if (fillWidth > 0f) {
                                drawRoundRect(
                                    color = barFill,
                                    topLeft = Offset.Zero,
                                    size = Size(fillWidth, size.height),
                                    cornerRadius = corner
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Weight by Category",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (categoryBreakdown.isEmpty()) {
                        Text(
                            text = "Add gear or select a trip with items to inspect category weight distribution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxCategoryGrams = categoryBreakdown.maxOf { it.grams }.coerceAtLeast(1L)
                        val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        val fillColor = MaterialTheme.colorScheme.primary
                        val accentColor = MaterialTheme.colorScheme.secondary

                        categoryBreakdown.forEachIndexed { idx, entry ->
                            val fraction = (entry.grams.toFloat() / maxCategoryGrams.toFloat()).coerceIn(0.03f, 1f)
                            val pct = if (totalGrams > 0L) ((entry.grams * 100) / totalGrams).toInt() else 0
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = entry.category.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${formatWeight(entry.grams, weightUnit)} ($pct%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                ) {
                                    val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    drawRoundRect(
                                        color = trackColor,
                                        size = size,
                                        cornerRadius = corner
                                    )
                                    drawRoundRect(
                                        color = if (idx == 0) accentColor else fillColor,
                                        size = Size(size.width * fraction, size.height),
                                        cornerRadius = corner
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (topHeavyItems.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Heaviest Items",
                            style = MaterialTheme.typography.titleMedium
                        )
                        topHeavyItems.forEachIndexed { index, (name, grams) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatWeight(grams, weightUnit),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
