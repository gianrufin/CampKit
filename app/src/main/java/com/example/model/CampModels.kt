package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale
import kotlin.math.roundToInt

enum class GearCategory(val label: String, val shortCode: String) {
    SHELTER("Shelter", "SHL"),
    SLEEP("Sleep System", "SLP"),
    COOKING("Cook & Stove", "COK"),
    CLOTHING("Layers & Apparel", "CLT"),
    HYDRATION("Water & Filter", "H2O"),
    SAFETY("First Aid & Nav", "SAF"),
    TOOLS("Tools & Light", "TLS"),
    OTHER("Camp Extras", "EXT");

    companion object {
        fun fromRaw(raw: String?): GearCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class TripType(val label: String, val targetBaseGrams: Long) {
    BACKPACKING("Backpacking", 5500L),
    CAR_CAMPING("Car Camping", 22000L),
    BIKEPACKING("Bikepacking", 4500L),
    DAY_HIKE("Day Hike", 2500L);

    companion object {
        fun fromRaw(raw: String?): TripType =
            entries.firstOrNull { it.name == raw } ?: BACKPACKING
    }
}

enum class WeightUnit(val label: String) {
    METRIC("Metric (g / kg)"),
    IMPERIAL("Imperial (oz / lb)")
}

fun formatWeight(grams: Long, unit: WeightUnit): String {
    val safeGrams = grams.coerceAtLeast(0L)
    return when (unit) {
        WeightUnit.METRIC -> {
            if (safeGrams >= 1000L) {
                val kg = safeGrams / 1000.0
                String.format(Locale.US, "%.2f kg", kg)
            } else {
                "$safeGrams g"
            }
        }
        WeightUnit.IMPERIAL -> {
            val totalOz = safeGrams / 28.3495
            if (totalOz >= 16.0) {
                val lbs = totalOz / 16.0
                String.format(Locale.US, "%.2f lb", lbs)
            } else {
                String.format(Locale.US, "%.1f oz", totalOz)
            }
        }
    }
}

fun formatWeightShort(grams: Long, unit: WeightUnit): String {
    val safeGrams = grams.coerceAtLeast(0L)
    return when (unit) {
        WeightUnit.METRIC -> {
            if (safeGrams >= 1000L) {
                String.format(Locale.US, "%.1fkg", safeGrams / 1000.0)
            } else {
                "${safeGrams}g"
            }
        }
        WeightUnit.IMPERIAL -> {
            val totalOz = safeGrams / 28.3495
            if (totalOz >= 16.0) {
                String.format(Locale.US, "%.1flb", totalOz / 16.0)
            } else {
                "${totalOz.roundToInt()}oz"
            }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class GearItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = GearCategory.OTHER.name,
    val weightGrams: Long = 0L,
    val quantityOwned: Long = 1L,
    val essential: Boolean = false,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: GearCategory
        get() = GearCategory.fromRaw(category)
}

@Keep
@IgnoreExtraProperties
data class CampTrip(
    @DocumentId val id: String = "",
    val title: String = "",
    val location: String = "",
    val tripType: String = TripType.BACKPACKING.name,
    val nights: Long = 2L,
    val startDateLabel: String = "",
    val notes: String? = null,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val tripTypeEnum: TripType
        get() = TripType.fromRaw(tripType)
}

@Keep
@IgnoreExtraProperties
data class PackingItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = GearCategory.OTHER.name,
    val weightGrams: Long = 0L,
    val quantity: Long = 1L,
    val packed: Boolean = false,
    val worn: Boolean = false,
    val gearId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: GearCategory
        get() = GearCategory.fromRaw(category)

    val totalGrams: Long
        get() = weightGrams.coerceAtLeast(0L) * quantity.coerceAtLeast(1L)
}

data class StarterItemTemplate(
    val name: String,
    val category: GearCategory,
    val weightGrams: Long,
    val quantity: Long = 1L,
    val essential: Boolean = false,
    val worn: Boolean = false,
    val notes: String? = null
)

object StarterKits {
    val lockerDefaults: List<StarterItemTemplate> = listOf(
        StarterItemTemplate("2P Ultralight Dome Tent", GearCategory.SHELTER, 1190L, 1L, true, false, "Includes stakes and footprint"),
        StarterItemTemplate("20°F Down Sleeping Quilt", GearCategory.SLEEP, 620L, 1L, true, false, "850-fill hydrophobic down"),
        StarterItemTemplate("Insulated Sleeping Pad (R-4.5)", GearCategory.SLEEP, 430L, 1L, true, false, "Regular wide"),
        StarterItemTemplate("Titanium Pocket Stove", GearCategory.COOKING, 73L, 1L, true, false, "Isobutane canister compatible"),
        StarterItemTemplate("750ml Titanium Pot", GearCategory.COOKING, 98L, 1L, false, false, "Nesting lid"),
        StarterItemTemplate("Hollow-Fiber Water Filter", GearCategory.HYDRATION, 65L, 1L, true, false, "Squeeze filter + 1L pouch"),
        StarterItemTemplate("USB-C Rechargeable Headlamp", GearCategory.TOOLS, 82L, 1L, true, false, "350 lumens + red mode"),
        StarterItemTemplate("Backcountry First Aid Kit", GearCategory.SAFETY, 145L, 1L, true, false, "Blister care, trauma gauze, meds"),
        StarterItemTemplate("Packable Rain Shell", GearCategory.CLOTHING, 215L, 1L, true, true, "3-layer waterproof breathable"),
        StarterItemTemplate("Merino Midweight Hoodie", GearCategory.CLOTHING, 240L, 1L, false, true, "Camp & summit layer")
    )

    fun itemsForTripType(tripType: TripType): List<StarterItemTemplate> = when (tripType) {
        TripType.BACKPACKING -> listOf(
            StarterItemTemplate("2P Ultralight Dome Tent", GearCategory.SHELTER, 1190L, 1L, true, false),
            StarterItemTemplate("20°F Down Sleeping Quilt", GearCategory.SLEEP, 620L, 1L, true, false),
            StarterItemTemplate("Insulated Sleeping Pad", GearCategory.SLEEP, 430L, 1L, true, false),
            StarterItemTemplate("Titanium Pocket Stove + Fuel", GearCategory.COOKING, 275L, 1L, true, false),
            StarterItemTemplate("750ml Pot & Long Spork", GearCategory.COOKING, 115L, 1L, false, false),
            StarterItemTemplate("Hollow-Fiber Water Filter", GearCategory.HYDRATION, 65L, 1L, true, false),
            StarterItemTemplate("2x 1L Trail Water Bottles", GearCategory.HYDRATION, 78L, 1L, true, false),
            StarterItemTemplate("Headlamp & Spare Cable", GearCategory.TOOLS, 88L, 1L, true, false),
            StarterItemTemplate("First Aid & Satellite Beacon", GearCategory.SAFETY, 240L, 1L, true, false),
            StarterItemTemplate("Packable Rain Shell", GearCategory.CLOTHING, 215L, 1L, true, true),
            StarterItemTemplate("Trail Runners & Gaiters", GearCategory.CLOTHING, 690L, 1L, true, true)
        )
        TripType.CAR_CAMPING -> listOf(
            StarterItemTemplate("4P Basecamp Cabin Tent", GearCategory.SHELTER, 4800L, 1L, true, false),
            StarterItemTemplate("Double Camp Quilt & Pillows", GearCategory.SLEEP, 1950L, 1L, true, false),
            StarterItemTemplate("Self-Inflating Foam Mattress", GearCategory.SLEEP, 2100L, 1L, true, false),
            StarterItemTemplate("Two-Burner Propane Camp Stove", GearCategory.COOKING, 4400L, 1L, true, false),
            StarterItemTemplate("45L Hard-Sided Cooler", GearCategory.COOKING, 5900L, 1L, false, false),
            StarterItemTemplate("20L Water Jerry Can", GearCategory.HYDRATION, 920L, 1L, true, false),
            StarterItemTemplate("Rechargeable Basecamp Lantern", GearCategory.TOOLS, 460L, 1L, false, false),
            StarterItemTemplate("Folding Camp Chairs", GearCategory.OTHER, 2200L, 2L, false, false),
            StarterItemTemplate("First Aid & Firewood Hatchet", GearCategory.SAFETY, 980L, 1L, true, false)
        )
        TripType.BIKEPACKING -> listOf(
            StarterItemTemplate("1P Bikepack Tent (Short Poles)", GearCategory.SHELTER, 980L, 1L, true, false),
            StarterItemTemplate("30°F Compact Down Bag", GearCategory.SLEEP, 540L, 1L, true, false),
            StarterItemTemplate("Ultralight Air Pad", GearCategory.SLEEP, 340L, 1L, true, false),
            StarterItemTemplate("Compact Titanium Mug + Stove", GearCategory.COOKING, 165L, 1L, false, false),
            StarterItemTemplate("Frame Water Bladders (2L)", GearCategory.HYDRATION, 130L, 1L, true, false),
            StarterItemTemplate("Multi-tool, Pump & Patch Kit", GearCategory.TOOLS, 310L, 1L, true, false),
            StarterItemTemplate("Helmet & Cycling Wind Jacket", GearCategory.CLOTHING, 420L, 1L, true, true),
            StarterItemTemplate("Compact Trauma & Beacon Kit", GearCategory.SAFETY, 180L, 1L, true, false)
        )
        TripType.DAY_HIKE -> listOf(
            StarterItemTemplate("22L Daypack", GearCategory.OTHER, 590L, 1L, true, false),
            StarterItemTemplate("2L Hydration Reservoir", GearCategory.HYDRATION, 185L, 1L, true, false),
            StarterItemTemplate("Emergency Bivy & First Aid", GearCategory.SAFETY, 220L, 1L, true, false),
            StarterItemTemplate("Headlamp", GearCategory.TOOLS, 82L, 1L, true, false),
            StarterItemTemplate("Wind & Rain Layer", GearCategory.CLOTHING, 215L, 1L, true, true),
            StarterItemTemplate("Trekking Poles", GearCategory.TOOLS, 410L, 1L, false, true)
        )
    }
}
