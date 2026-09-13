package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.takwa.lapwalker.domain.model.WorkoutRecord

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val laps: Int,
    val distanceKm: Double,
    val durationSeconds: Long,
    val caloriesBurned: Double,
    val avgLapTimeSec: Double
) {
    fun toDomain(): WorkoutRecord = WorkoutRecord(
        id = id,
        timestamp = timestamp,
        laps = laps,
        distanceKm = distanceKm,
        durationSeconds = durationSeconds,
        caloriesBurned = caloriesBurned,
        avgLapTimeSec = avgLapTimeSec
    )

    companion object {
        fun fromDomain(domain: WorkoutRecord): WorkoutEntity = WorkoutEntity(
            id = domain.id,
            timestamp = domain.timestamp,
            laps = domain.laps,
            distanceKm = domain.distanceKm,
            durationSeconds = domain.durationSeconds,
            caloriesBurned = domain.caloriesBurned,
            avgLapTimeSec = domain.avgLapTimeSec
        )
    }
}
