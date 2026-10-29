package com.example.util

import kotlin.math.abs
import kotlin.math.roundToInt

object Base8Math {
    /**
     * Converts a string formatted as "X.Y" or "X" into total 1/8-th units (parts).
     * Where 1 whole = 8 parts.
     */
    fun toParts(value: String): Int {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed == ".") return 0
        val isNegative = trimmed.startsWith("-")
        val clean = if (isNegative) trimmed.substring(1) else trimmed

        val parts = clean.split(".")
        val wholes = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val suta = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val total = (wholes * 8) + suta
        return if (isNegative) -total else total
    }

    /**
     * Converts total parts back into "X.Y" format (e.g. 284 parts = 35.4 inches).
     */
    fun fromParts(totalParts: Int): String {
        val isNegative = totalParts < 0
        val absParts = abs(totalParts)
        val wholes = absParts / 8
        val suta = absParts % 8
        val prefix = if (isNegative) "-" else ""
        return "$prefix$wholes.$suta"
    }

    /**
     * Executes base-8 arithmetic operation between two part quantities.
     */
    fun calculate(op: String, a: Int, b: Int): Int {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> ((a.toDouble() * b.toDouble()) / 8.0).roundToInt()
            "/" -> if (b == 0) 0 else ((a.toDouble() * 8.0) / b.toDouble()).roundToInt()
            else -> b
        }
    }

    /**
     * Normalizes a string representation if user types something like "0.8" or "12.11"
     */
    fun normalizeInput(raw: String): String {
        if (!raw.contains(".")) return raw
        val parts = raw.split(".")
        val suta = parts.getOrNull(1)?.toIntOrNull() ?: return raw
        if (suta >= 8) {
            return fromParts(toParts(raw))
        }
        return raw
    }

    /**
     * Suta to Millimeters table mapping (1 inch = 25.4 mm, 1 suta = 3.175 mm).
     */
    data class SutaConversion(
        val suta: Int,
        val fraction: String,
        val inchDecimal: Double,
        val mm: Double
    )

    val CONVERSION_TABLE = listOf(
        SutaConversion(1, "1/8\"", 0.125, 3.175),
        SutaConversion(2, "1/4\"", 0.250, 6.350),
        SutaConversion(3, "3/8\"", 0.375, 9.525),
        SutaConversion(4, "1/2\"", 0.500, 12.700),
        SutaConversion(5, "5/8\"", 0.625, 15.875),
        SutaConversion(6, "3/4\"", 0.750, 19.050),
        SutaConversion(7, "7/8\"", 0.875, 22.225),
        SutaConversion(8, "1\"", 1.000, 25.400)
    )
}
