package com.example.domain

import kotlin.math.roundToInt

object ChestEstimate {
    const val MIN_COUNTED_MATCHES = 10

    data class Result(
        val careerMatches: Int,
        val countedMatches: Int,
        val missingMatches: Int,
        val average: Double,
        val chests: Int,
    )

    fun fromCareer(view: TrackerView, careerMatches: Int): Result? {
        val counted = view.matches.size
        if (counted < MIN_COUNTED_MATCHES || careerMatches <= counted) return null
        val average = view.countedChests.toDouble() / counted
        val missing = careerMatches - counted
        return Result(
            careerMatches = careerMatches,
            countedMatches = counted,
            missingMatches = missing,
            average = average,
            chests = (missing * average).roundToInt(),
        )
    }

    fun parseNumber(text: String): Int? {
        val digits = text.filter { it.isDigit() || it == '-' }
        return digits.toIntOrNull()
    }
}
