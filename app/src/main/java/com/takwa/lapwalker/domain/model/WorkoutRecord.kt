package com.takwa.lapwalker.domain.model

data class WorkoutRecord(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val laps: Int,
    val distanceKm: Double,
    val durationSeconds: Long,
    val caloriesBurned: Double,
    val avgLapTimeSec: Double
)
