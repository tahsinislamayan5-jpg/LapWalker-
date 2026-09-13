package com.takwa.lapwalker.domain.repository

import com.takwa.lapwalker.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    val allWorkoutsFlow: Flow<List<WorkoutRecord>>
    suspend fun saveWorkout(record: WorkoutRecord): Long
    suspend fun getRecentWorkouts(limit: Int = 20): List<WorkoutRecord>
    suspend fun deleteWorkout(id: Long)
    suspend fun clearAllWorkouts()
    suspend fun seedHistoricWorkouts(workouts: List<WorkoutRecord>)
}
