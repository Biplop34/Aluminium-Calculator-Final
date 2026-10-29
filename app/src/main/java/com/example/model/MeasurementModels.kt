package com.example.model

data class MeasurementSet(
    val id: Long = System.currentTimeMillis() + (0..9999).random(),
    val height: String = "",
    val length1: String = "",
    val length2: String = "",
    val isOverridden: Boolean = false,
    val isShutterOverridden: Boolean = false,
    val isInchMode: Boolean = false
)

data class CalculationResult(
    val id: Long,
    val label: String,
    val outerSide: String,
    val outerTop: String,
    val outerBottom: String,
    val lockVal: String,
    val shutterTopBottom: String,
    val glassHeightInch: String,
    val glassLengthInch: String,
    val netSectionHeight: String = "",
    val netSectionLength: String = "",
    val isSmallFrame: Boolean = false,
    val isOneShutter: Boolean = false,
    val isPartition: Boolean = false,
    val isInchMode: Boolean = false,
    val rawHeight: Double = 0.0,
    val rawLength1: Double = 0.0,
    val rawLength2: Double = 0.0,
    val isGaton: Boolean? = null
)

data class HardwareItem(
    val name: String,
    val value: String,
    val category: String,
    val note: String,
    val isMosquitoNet: Boolean = false
)

data class HardwareCalculationResult(
    val items: List<HardwareItem>,
    val slidingLock: String,
    val slidingWheelRegular: String,
    val slidingWheelGaton: String,
    val slidingWheelDoubt: String,
    val slidingRubber: String,
    val screws1_5: String,
    val kotScrews: String,
    val mohiar: String,
    val royalPlugs: String,
    val mosquitoNetRubber: String,
    val mosquitoNetAngel: String,
    val mosquitoNetHandle: String,
    val mosquitoNetWheel: String,
    val mosquitoNet: String
)

data class CustomerDetails(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerLocation: String = "",
    val alumColor: String = "Silver Color",
    val alumBrand: String = "Chung Hua",
    val otherAlumBrand: String = "",
    val glassType: String = "Clear Glass",
    val glassThickness: String = "5 MM"
)

data class SavedJob(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val thaiSize: String = "3",
    val customerDetails: CustomerDetails = CustomerDetails(),
    val sets: List<MeasurementSet> = emptyList(),
    val results: List<CalculationResult> = emptyList()
)

data class Base8HistoryItem(
    val id: String = System.currentTimeMillis().toString(),
    val expression: String,
    val result: String,
    val resultParts: Int
)

object FabricationConstants {
    val ALUM_COLORS = listOf(
        "Silver Color",
        "Bronze Color",
        "Golden Color",
        "SS Color",
        "Black Powder Coated",
        "White Powder Coated",
        "Wood Grain"
    )

    val ALUM_BRANDS = listOf(
        "Chung Hua",
        "KAI",
        "PHP Shiny",
        "PHP Solid",
        "MAX",
        "AXX",
        "AluTech",
        "Others"
    )

    val GLASS_TYPES = listOf(
        "Clear Glass",
        "Navy Blue Mercury",
        "Lake Blue Mercury",
        "Ocean Blue",
        "Green Mercury",
        "Coffee Light",
        "Coffee Deep",
        "Golden Mercury",
        "Silver Mercury",
        "Solid Blue",
        "Solid Green",
        "Solid Black",
        "Frosted / Tinted"
    )

    val GLASS_THICKNESSES = listOf(
        "5 MM",
        "5.5 MM",
        "6 MM",
        "8 MM",
        "10 MM",
        "12 MM"
    )
}
