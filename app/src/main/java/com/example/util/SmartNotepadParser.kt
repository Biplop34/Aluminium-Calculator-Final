package com.example.util

import com.example.model.MeasurementSet

object SmartNotepadParser {
    /**
     * Parses freeform or multi-line workshop notepad measurements into structured MeasurementSets.
     * Supports formats like:
     * - "206.1 x 181.9"
     * - "206/1 x 181/9"
     * - "206-1 x 181-9"
     * - "#202*1 x 181*9"
     * - "136.7 x 149.7/150"
     * - "21/2 x 46 - 45/5"
     * - "1. 120 x 150"
     * - "55.4 * 48.2 - 47.6"
     */
    fun parse(text: String): List<MeasurementSet> {
        if (text.isBlank()) return emptyList()

        val lines = text.lines()
        val parsedSets = mutableListOf<MeasurementSet>()

        for (line in lines) {
            // Strip invisible Unicode formatting/bidi chars
            var s = line.replace(Regex("""[\u200B-\u200F\u202A-\u202E\uFEFF]"""), "").trim()
            if (s.isBlank()) continue

            // 1. Strip leading bullets/symbols
            s = s.replace(Regex("""^[#•\*\-+|~]+\s*"""), "")
            // 2. Strip labeled serial prefixes: item 1, no. 1, sl 1, gap 1, ser 1, etc.
            s = s.replace(Regex("""^(?:item|no\.?|sl\.?|gap|ser|serial)\s*\d{1,3}[\s\/:.)\-]*""", RegexOption.IGNORE_CASE), "")
            // 3. Strip bracketed serials: (1), [1]
            s = s.replace(Regex("""^(?:\(\d{1,3}\)|\[\d{1,3}\])[\s\/:.)\-]*"""), "")
            // 4. Strip serial number followed by delimiter + space: e.g. "1. ", "1) ", "1: ", "1 - ", "1 | "
            s = s.replace(Regex("""^\d{1,3}\s*[\.):|\-]\s+"""), "")
            // 5. Strip serial with slash: e.g. "1 / 177.8", "2/ 178", or "2/178"
            s = s.replace(Regex("""^\d{1,3}\s*/\s*(?=\d{2,}(?:[./\-*]\d+)?\s*[xX×*])"""), "")
            s = s.replace(Regex("""^\d{1,3}\s+/\s*"""), "")
            s = s.replace(Regex("""^\d{1,3}/\s+(?=\d{2,})"""), "")
            // 6. Strip serial followed only by space: e.g. "1 177.8*165"
            s = s.replace(Regex("""^\d{1,2}\s+(?=\d{2,}(?:[./\-*]\d+)?\s*[xX×*])""", RegexOption.IGNORE_CASE), "")
            // 7. Strip any remaining leading symbols
            s = s.replace(Regex("""^[#•\*\-+|~]+\s*"""), "").trim()
            if (s.isBlank()) continue

            // Dimension separator:
            // Priority: [xX×]
            // Or 'by'
            // Or '*' (surrounded by spaces or separating two numbers)
            val parts: List<String> = when {
                s.contains(Regex("""[xX×]""")) -> s.split(Regex("""\s*[xX×]\s*"""), limit = 2)
                s.contains(Regex("""\s+by\s+""", RegexOption.IGNORE_CASE)) -> s.split(Regex("""\s+by\s+""", RegexOption.IGNORE_CASE), limit = 2)
                s.contains(Regex("""\s+\*\s+""")) -> s.split(Regex("""\s+\*\s+"""), limit = 2)
                s.contains("*") -> {
                    val m = Regex("""^(\d+(?:[./\-*]\d{1,2})?)\s*\*\s*(\d{2,}.*)$""").matchEntire(s)
                    if (m != null) {
                        listOf(m.groupValues[1], m.groupValues[2])
                    } else {
                        val starIdx = s.indexOf("*")
                        listOf(s.substring(0, starIdx), s.substring(starIdx + 1))
                    }
                }
                else -> continue
            }

            if (parts.size >= 2) {
                val rawH = parts[0].trim()
                val rawL = parts[1].trim()

                val height = parseSingleNumber(rawH)
                val (length1, length2) = parseLengths(rawL)

                if (height.isNotBlank() && length1.isNotBlank()) {
                    parsedSets.add(
                        MeasurementSet(
                            height = height,
                            length1 = length1,
                            length2 = length2
                        )
                    )
                }
            }
        }
        return parsedSets
    }

    private fun parseSingleNumber(valStr: String): String {
        val s = valStr.replace(Regex("""[\u200B-\u200F\u202A-\u202E\uFEFF]"""), "").trim()
        val m = Regex("""^(\d+)\s*[./\-*]\s*(\d+)$""").matchEntire(s)
        if (m != null) {
            return "${m.groupValues[1]}.${m.groupValues[2]}"
        }
        val mInt = Regex("""^(\d+)$""").matchEntire(s)
        if (mInt != null) {
            return mInt.groupValues[1]
        }
        return s.replace(Regex("""[^\d.]"""), "")
    }

    private fun parseLengths(rawL: String): Pair<String, String> {
        val s = rawL.replace(Regex("""[\u200B-\u200F\u202A-\u202E\uFEFF]"""), "").trim()

        if (s.contains(" - ")) {
            val p = s.split(" - ", limit = 2)
            return Pair(parseSingleNumber(p[0]), parseSingleNumber(p[1]))
        }
        if (s.contains(" / ")) {
            val p = s.split(" / ", limit = 2)
            return Pair(parseSingleNumber(p[0]), parseSingleNumber(p[1]))
        }
        if (s.contains(",")) {
            val p = s.split(",", limit = 2)
            return Pair(parseSingleNumber(p[0]), parseSingleNumber(p[1]))
        }

        // Integer - full measurement (e.g. 46-45.5 or 46-45/5 or 46-45)
        val mDashDual = Regex("""^(\d+)\s*-\s*(\d{2,}\s*[./\-*]\s*\d+|\d{2,})$""").matchEntire(s)
        if (mDashDual != null) {
            return Pair(parseSingleNumber(mDashDual.groupValues[1]), parseSingleNumber(mDashDual.groupValues[2]))
        }

        // Decimal separator before slash: e.g. 149.7/150 or 149-7/150 or 149*7/150 or 149.7/150.2 or 166/165.6
        val mSlashDec = Regex("""^(\d+\s*[.\-*]\s*\d+)\s*/\s*(\d+(?:\s*[./\-*]\s*\d+)?)$""").matchEntire(s)
        if (mSlashDec != null) {
            return Pair(parseSingleNumber(mSlashDec.groupValues[1]), parseSingleNumber(mSlashDec.groupValues[2]))
        }

        // Whole/integer measurement followed by slash and second measurement: e.g. 166/165.6 or 149/150
        val mSlFirstWhole = Regex("""^(\d{2,})\s*/\s*(\d{2,}\s*[./\-*]\s*\d+|\d{2,})$""").matchEntire(s)
        if (mSlFirstWhole != null) {
            return Pair(parseSingleNumber(mSlFirstWhole.groupValues[1]), parseSingleNumber(mSlFirstWhole.groupValues[2]))
        }

        // Slash in first measurement then slash before second: e.g. 149/7/150
        val mTripleSlash = Regex("""^(\d+/\d)\s*/\s*(\d+(?:[./\-*]\d+)?)$""").matchEntire(s)
        if (mTripleSlash != null) {
            return Pair(parseSingleNumber(mTripleSlash.groupValues[1]), parseSingleNumber(mTripleSlash.groupValues[2]))
        }

        // Integer / multi-digit integer (e.g. 149/150)
        val mIntSlash = Regex("""^(\d+)\s*/\s*(\d{2,})$""").matchEntire(s)
        if (mIntSlash != null) {
            return Pair(mIntSlash.groupValues[1], mIntSlash.groupValues[2])
        }

        // Single measurement: e.g. 181.9, 181/9, 181-9, 181*9, 181
        return Pair(parseSingleNumber(s), "")
    }
}
