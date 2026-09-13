package com.takwa.lapwalker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takwa.lapwalker.data.local.db.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity): Long

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentWorkouts(limit: Int): List<WorkoutEntity>

    @Query("SELECT SUM(distanceKm) FROM workouts")
    suspend fun getTotalDistanceKm(): Double?

    @Query("SELECT SUM(laps) FROM workouts")
    suspend fun getTotalLaps(): Int?

    @Query("SELECT COUNT(*) FROM workouts WHERE timestamp = :timestamp")
    suspend fun countByTimestamp(timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("DELETE FROM workouts")
    suspend fun clearAllWorkouts()
}
