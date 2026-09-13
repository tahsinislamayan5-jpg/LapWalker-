package com.takwa.lapwalker.data.catalog

import com.takwa.lapwalker.domain.model.WorkoutRecord

object HistoricWorkoutCatalog {
    val initialHistory: List<WorkoutRecord> = listOf(
        // 1. Sun, Sep 13 • 6:00 AM (Today: 10 laps of 516 ft = 5160 ft = 1.57 km)
        WorkoutRecord(
            id = 1L,
            timestamp = 1789257600000L,
            laps = 10,
            distanceKm = 1.57,
            durationSeconds = 18L * 60L + 15L, // 18m 15s
            caloriesBurned = 83.0,
            avgLapTimeSec = 109.5
        ),
        // 2. Sat, Sep 12 • 11:40 PM: 1.73 km, 11 laps, 24m 39s, 97 kcal
        WorkoutRecord(
            id = 2L,
            timestamp = 1789234800000L,
            laps = 11,
            distanceKm = 1.73,
            durationSeconds = 24L * 60L + 39L,
            caloriesBurned = 97.0,
            avgLapTimeSec = 134.45
        ),
        // 3. Sat, Sep 12 • 11:14 PM: 1.42 km, 9 laps, 12m 54s, 80 kcal
        WorkoutRecord(
            id = 3L,
            timestamp = 1789233240000L,
            laps = 9,
            distanceKm = 1.42,
            durationSeconds = 12L * 60L + 54L,
            caloriesBurned = 80.0,
            avgLapTimeSec = 86.0
        ),
        // 4. Sat, Sep 12 • 1:28 PM: 0.66 km, 15 laps, 9m 04s, 37 kcal
        WorkoutRecord(
            id = 4L,
            timestamp = 1789198080000L,
            laps = 15,
            distanceKm = 0.66,
            durationSeconds = 9L * 60L + 4L,
            caloriesBurned = 37.0,
            avgLapTimeSec = 36.27
        ),
        // 5. Sat, Sep 12 • 1:17 PM: 0.61 km, 14 laps, 8m 43s, 35 kcal
        WorkoutRecord(
            id = 5L,
            timestamp = 1789197420000L,
            laps = 14,
            distanceKm = 0.61,
            durationSeconds = 8L * 60L + 43L,
            caloriesBurned = 35.0,
            avgLapTimeSec = 37.36
        ),
        // 6. Sat, Sep 12 • 10:33 AM: 0.47 km, 3 laps, 8m 36s, 25 kcal
        WorkoutRecord(
            id = 6L,
            timestamp = 1789187580000L,
            laps = 3,
            distanceKm = 0.47,
            durationSeconds = 8L * 60L + 36L,
            caloriesBurned = 25.0,
            avgLapTimeSec = 172.0
        ),
        // 7. Fri, Sep 11 • 11:52 PM: 1.04 km, 63 laps, 22m 39s, 58 kcal
        WorkoutRecord(
            id = 7L,
            timestamp = 1789149120000L,
            laps = 63,
            distanceKm = 1.04,
            durationSeconds = 22L * 60L + 39L,
            caloriesBurned = 58.0,
            avgLapTimeSec = 21.57
        )
    )
}
