package com.example.util

import com.example.model.CalculationResult
import com.example.model.HardwareCalculationResult
import com.example.model.HardwareItem
import com.example.model.MeasurementSet
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object ThaiFabricationMath {
    private const val FIVE_FEET_CM = 152.4
    private const val SIX_FEET_CM = 182.88
    private const val FOUR_FEET_CM = 121.92
    private const val FORTY_TWO_IN_CM = 106.68

    private const val FIVE_FEET_IN = 60.0
    private const val SIX_FEET_IN = 72.0
    private const val FOUR_FEET_IN = 48.0
    private const val FORTY_TWO_IN = 42.0

    private const val CM_TO_INCH = 0.393701

    fun formatInchesToSuta(value: Double, alreadyInInches: Boolean = false): String {
        if (value <= 0.0) return "0.0"
        val totalInches = if (alreadyInInches) value else value * CM_TO_INCH
        var intPart = floor(totalInches).toInt()
        var suta = ((totalInches - intPart) * 8.0).roundToInt()
        if (suta >= 8) {
            intPart += 1
            suta = 0
        }
        return "$intPart.$suta"
    }

    private fun Double.format1Dp(): String {
        return String.format(Locale.US, "%.1f", this)
    }

    fun calculateItem(set: MeasurementSet): CalculationResult? {
        val h = set.height.trim().toDoubleOrNull() ?: 0.0
        val l1 = set.length1.trim().toDoubleOrNull() ?: 0.0
        val l2 = set.length2.trim().toDoubleOrNull() ?: 0.0

        if (h <= 0.0 || l1 <= 0.0) return null

        val isInchMode = set.isInchMode
        val qualifiesPartition = if (isInchMode) {
            h >= SIX_FEET_IN && l1 >= FOUR_FEET_IN
        } else {
            h >= SIX_FEET_CM && l1 >= FOUR_FEET_CM
        }

        val qualifiesFanLight = !isInchMode && h < 61.0 && l1 < 61.0
        val qualifiesShutterCM = h >= FIVE_FEET_CM && l1 < FORTY_TWO_IN_CM
        val qualifiesShutterIN = h >= FIVE_FEET_IN && l1 < FORTY_TWO_IN
        val qualifiesOneShutterBase = !qualifiesPartition && (qualifiesShutterCM || qualifiesShutterIN)

        val isFanLight = qualifiesFanLight && !set.isOverridden
        val isPartition = qualifiesPartition
        val isOneShutter = qualifiesOneShutterBase && !set.isShutterOverridden

        val finalOuterSide: String
        val finalOuterTop: String
        val finalOuterBottom: String
        val finalLockVal: String
        val finalShutterTopBottom: String
        val gH: String
        val gL: String
        var netSectionHeight = ""
        var netSectionLength = ""

        if (isOneShutter) {
            if (isInchMode) {
                finalOuterSide = formatInchesToSuta(h, alreadyInInches = true)
                finalOuterTop = formatInchesToSuta(l1 * 2.0, alreadyInInches = true)
                finalOuterBottom = finalOuterTop
                finalLockVal = formatInchesToSuta(h - (2.7 / 2.54), alreadyInInches = true)
                finalShutterTopBottom = formatInchesToSuta(l1 - 4.0, alreadyInInches = true)
                gH = formatInchesToSuta(h - (2.7 / 2.54) - (8.0 / 2.54), alreadyInInches = true)
                gL = formatInchesToSuta(l1 - 4.0 + (1.3 / 2.54), alreadyInInches = true)
            } else {
                finalOuterSide = h.format1Dp()
                finalOuterTop = (l1 * 2.0).format1Dp()
                finalOuterBottom = finalOuterTop
                finalLockVal = (h - 2.7).format1Dp()
                finalShutterTopBottom = (l1 - 10.16).format1Dp()
                gH = formatInchesToSuta(h - 2.7 - 8.0)
                gL = formatInchesToSuta(l1 - 10.16 + 1.3)
            }
        } else {
            val maxLength = max(l1, l2)
            finalOuterSide = h.format1Dp()
            finalOuterTop = (l1 - 1.3).format1Dp()
            finalOuterBottom = if (l2 > 0.0) (l2 - 1.3).format1Dp() else finalOuterTop
            finalLockVal = if (isFanLight) (h - 2.0).format1Dp() else (h - 2.7).format1Dp()
            finalShutterTopBottom = ((maxLength - 15.0) / 2.0).format1Dp()

            val lockValNum = finalLockVal.toDoubleOrNull() ?: (h - 2.7)
            val shutterTopBottomNum = finalShutterTopBottom.toDoubleOrNull() ?: ((maxLength - 15.0) / 2.0)

            gH = formatInchesToSuta(lockValNum - 8.0)
            gL = formatInchesToSuta(shutterTopBottomNum + 1.3)

            netSectionHeight = (h - 3.0).format1Dp()
            netSectionLength = (shutterTopBottomNum + 8.5).format1Dp()
        }

        val finalLabel = when {
            isFanLight -> "Fan Light"
            isPartition -> "Partition"
            isOneShutter -> "1 Shutter ${if (isInchMode) "IN" else "CM"}"
            else -> "2 Shutter"
        }

        return CalculationResult(
            id = set.id,
            label = finalLabel,
            outerSide = finalOuterSide,
            outerTop = finalOuterTop,
            outerBottom = finalOuterBottom,
            lockVal = finalLockVal,
            shutterTopBottom = finalShutterTopBottom,
            glassHeightInch = gH,
            glassLengthInch = gL,
            netSectionHeight = netSectionHeight,
            netSectionLength = netSectionLength,
            isSmallFrame = isFanLight,
            isOneShutter = isOneShutter,
            isPartition = isPartition,
            isInchMode = isInchMode,
            rawHeight = h,
            rawLength1 = l1,
            rawLength2 = l2
        )
    }

    /**
     * Estimates total glass square footage for the given results.
     */
    fun calculateTotalGlassAreaSqFt(results: List<CalculationResult>): Double {
        var totalSqFt = 0.0
        for (res in results) {
            val hParts = Base8Math.toParts(res.glassHeightInch)
            val lParts = Base8Math.toParts(res.glassLengthInch)
            val hInches = hParts / 8.0
            val lInches = lParts / 8.0
            val glassMultiplier = if (res.isOneShutter) 1 else 2
            val sqInches = hInches * lInches * glassMultiplier
            totalSqFt += sqInches / 144.0
        }
        return totalSqFt
    }

    private fun formatFeetInch(totalInches: Double): String {
        if (totalInches <= 0.0) return "0 feet"
        var ft = floor(totalInches / 12.0).toInt()
        var inRem = kotlin.math.round(totalInches % 12.0).toInt()
        if (inRem >= 12) {
            ft += 1
            inRem = 0
        }
        return if (inRem == 0) "$ft feet" else "$ft feet $inRem inch"
    }

    fun calculateHardware(results: List<CalculationResult>, thaiSize: String = "4"): HardwareCalculationResult {
        if (results.isEmpty()) {
            return HardwareCalculationResult(
                items = emptyList(),
                slidingLock = "0 pcs",
                slidingWheelRegular = "0 pcs",
                slidingWheelGaton = "0 pcs",
                slidingWheelDoubt = "0 pcs",
                slidingRubber = "0 feet",
                screws1_5 = "0 pcs",
                kotScrews = "0 pcs",
                mohiar = "0 feet",
                royalPlugs = "0 pcs",
                mosquitoNetRubber = "0 feet",
                mosquitoNetAngel = "0 pcs",
                mosquitoNetHandle = "0 pcs",
                mosquitoNetWheel = "0 pcs",
                mosquitoNet = "0 feet"
            )
        }

        val is4Inch = thaiSize != "3"
        val totalWindows = results.size
        var nonFanlightCount = 0
        var fanlightCount = 0
        var partitionCount = 0
        var oneShutterCount = 0
        var standardCount = 0
        var gatonCount = 0

        var totalRubberInches = 0.0
        var total1_5Screws = 0
        var totalMohiarInches = 0.0
        var fanlightMohiarInches = 0.0
        var totalNetRubberInches = 0.0

        val rollHeights = listOf(4.0, 4.5, 5.0, 6.0, 7.0)
        val netBuckets = mutableMapOf<Double, Double>()
        rollHeights.forEach { netBuckets[it] = 0.0 }

        results.forEach { item ->
            val isFL = item.isSmallFrame
            val isPart = item.isPartition
            val isOne = item.isOneShutter
            val isInch = item.isInchMode

            if (isFL) fanlightCount++ else nonFanlightCount++
            if (isPart) {
                partitionCount++
            } else if (isOne) {
                oneShutterCount++
            } else if (!isFL) {
                val h = if (item.rawHeight > 0) item.rawHeight else item.outerSide.toDoubleOrNull() ?: 0.0
                val l1 = if (item.rawLength1 > 0) item.rawLength1 else item.outerTop.toDoubleOrNull() ?: 0.0
                val l2 = if (item.rawLength2 > 0) item.rawLength2 else item.outerBottom.toDoubleOrNull() ?: 0.0
                val maxL = max(l1, l2)
                val sqFt = if (isInch) ((h * maxL) / 144.0) else ((h * maxL) / 929.0304)
                val isOversized = item.isGaton ?: (sqFt > 22.5)
                if (isOversized) gatonCount++ else standardCount++
            }

            val gH = item.glassHeightInch.toDoubleOrNull() ?: 0.0
            val gL = item.glassLengthInch.toDoubleOrNull() ?: 0.0
            val roundH = kotlin.math.round(gH).toInt()
            val roundL = kotlin.math.round(gL).toInt()
            val glassPerimeterMult = if (isOne) 2 else 4
            totalRubberInches += (roundH + roundL) * glassPerimeterMult

            val windowScrews = when {
                isFL -> 8
                isPart -> 34
                isOne -> 16
                else -> 27
            }
            total1_5Screws += windowScrews

            val stb = item.shutterTopBottom.toDoubleOrNull() ?: 0.0
            val os = item.outerSide.toDoubleOrNull() ?: 0.0
            val windowMohiarInches = if (isInch) {
                (stb * 4.0) + (os * 2.0)
            } else {
                ((stb * 4.0) + (os * 2.0)) / 2.54
            }
            totalMohiarInches += windowMohiarInches
            if (isFL) fanlightMohiarInches += windowMohiarInches

            if (!isFL) {
                val nsH = item.netSectionHeight.toDoubleOrNull() ?: 0.0
                val nsL = item.netSectionLength.toDoubleOrNull() ?: 0.0
                if (nsH > 0.0 && nsL > 0.0) {
                    totalNetRubberInches += if (isInch) ((nsH + nsL) * 2.0) else (((nsH + nsL) * 2.0) / 2.54)
                }

                val osInches = if (isInch) os else (os / 2.54)
                var closestRoll = rollHeights[0]
                var minDiff = Double.MAX_VALUE
                rollHeights.forEach { rh ->
                    val diff = kotlin.math.abs(osInches - (rh * 12.0))
                    if (diff < minDiff) {
                        minDiff = diff
                        closestRoll = rh
                    }
                }

                val rawL1 = if (item.rawLength1 > 0) item.rawLength1 else item.outerTop.toDoubleOrNull() ?: 0.0
                val rawL2 = if (item.rawLength2 > 0) item.rawLength2 else item.outerBottom.toDoubleOrNull() ?: 0.0
                val maxL = max(rawL1, rawL2)
                val netItemLenInches = if (isInch) {
                    (maxL / 2.0) + 2.0
                } else {
                    ((maxL / 2.0) / 2.54) + 2.0
                }
                netBuckets[closestRoll] = (netBuckets[closestRoll] ?: 0.0) + netItemLenInches
            }
        }

        val slidingLock = "${nonFanlightCount * 2} pcs"
        val slidingWheelRegular = "${standardCount * 4} pcs"
        val slidingWheelGaton = "${gatonCount * 4} pcs"
        val slidingWheelDoubt = "${partitionCount * 4} pcs"
        val slidingRubber = formatFeetInch(totalRubberInches)
        val screws1_5 = "$total1_5Screws pcs"
        val kotScrews = "${totalWindows * 8} pcs"
        var mohiar = formatFeetInch(totalMohiarInches)
        if (fanlightCount > 0 && fanlightMohiarInches > 0) {
            mohiar += " (For Fanlight: ${formatFeetInch(fanlightMohiarInches)})"
        }
        val royalPlugs = "${max(0, total1_5Screws - (totalWindows * 8))} pcs"
        val mosquitoNetRubber = if (is4Inch) {
            if (totalNetRubberInches > 0) formatFeetInch(totalNetRubberInches) else "0 feet"
        } else "N/A"
        val mosquitoNetAngel = if (is4Inch) "${totalWindows * 4} pcs" else "N/A"
        val mosquitoNetHandle = if (is4Inch) "$totalWindows pcs" else "N/A"
        val mosquitoNetWheel = if (is4Inch) "${nonFanlightCount * 2} pcs" else "N/A"

        val netParts = mutableListOf<String>()
        rollHeights.forEach { rh ->
            val len = netBuckets[rh] ?: 0.0
            if (len > 0) {
                val rhLabel = if (rh % 1.0 == 0.0) "${rh.toInt()}'" else "$rh'"
                netParts.add("$rhLabel Height: ${formatFeetInch(len)}")
            }
        }
        val mosquitoNetStr = if (is4Inch) {
            if (netParts.isNotEmpty()) netParts.joinToString(" | ") else "0 feet"
        } else "N/A"

        fun isItemValid(s: String): Boolean {
            val trimmed = s.trim()
            return !(trimmed == "0 pcs" || trimmed == "0 feet" || trimmed == "0" || trimmed.isEmpty() || trimmed.startsWith("N/A"))
        }

        val candidateItems = listOf(
            HardwareItem("Sliding Lock", slidingLock, "Locks", ""),
            HardwareItem("Sliding Wheel (Regular)", slidingWheelRegular, "Wheels", ""),
            HardwareItem("Sliding Wheel (Gaton)", slidingWheelGaton, "Wheels", ""),
            HardwareItem("Sliding Wheel (Doubt Wheel)", slidingWheelDoubt, "Wheels", ""),
            HardwareItem("Sliding Rubber", slidingRubber, "Rubber", ""),
            HardwareItem("1.5 Inch Screw", screws1_5, "Fasteners", ""),
            HardwareItem("Kot Screw", kotScrews, "Fasteners", ""),
            HardwareItem("Mohiar", mohiar, "Weatherstrip", ""),
            HardwareItem("Royal Plug", royalPlugs, "Fasteners", ""),
            HardwareItem("Mosquito Net Rubber", mosquitoNetRubber, "4\" Frame", ""),
            HardwareItem("Mosquito Net Angel", mosquitoNetAngel, "4\" Frame", ""),
            HardwareItem("Mosquito Net Handle (3 Inch)", mosquitoNetHandle, "4\" Frame", ""),
            HardwareItem("Mosquito Net Wheel", mosquitoNetWheel, "4\" Frame", ""),
            HardwareItem("Mosquito Net", mosquitoNetStr, "4\" Frame", "", isMosquitoNet = true)
        )

        val items = candidateItems.filter { isItemValid(it.value) }

        return HardwareCalculationResult(
            items = items,
            slidingLock = slidingLock,
            slidingWheelRegular = slidingWheelRegular,
            slidingWheelGaton = slidingWheelGaton,
            slidingWheelDoubt = slidingWheelDoubt,
            slidingRubber = slidingRubber,
            screws1_5 = screws1_5,
            kotScrews = kotScrews,
            mohiar = mohiar,
            royalPlugs = royalPlugs,
            mosquitoNetRubber = mosquitoNetRubber,
            mosquitoNetAngel = mosquitoNetAngel,
            mosquitoNetHandle = mosquitoNetHandle,
            mosquitoNetWheel = mosquitoNetWheel,
            mosquitoNet = mosquitoNetStr
        )
    }
}
