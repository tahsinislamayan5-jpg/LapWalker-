package com.takwa.lapwalker.data.repository

import com.takwa.lapwalker.data.local.db.dao.WorkoutDao
import com.takwa.lapwalker.data.local.db.entity.WorkoutEntity
import com.takwa.lapwalker.domain.model.WorkoutRecord
import com.takwa.lapwalker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override val allWorkoutsFlow: Flow<List<WorkoutRecord>> =
        workoutDao.getAllWorkoutsFlow().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun saveWorkout(record: WorkoutRecord): Long =
        workoutDao.insert(WorkoutEntity.fromDomain(record))

    override suspend fun getRecentWorkouts(limit: Int): List<WorkoutRecord> =
        workoutDao.getRecentWorkouts(limit).map { it.toDomain() }

    override suspend fun deleteWorkout(id: Long) {
        workoutDao.deleteWorkout(id)
    }

    override suspend fun clearAllWorkouts() {
        workoutDao.clearAllWorkouts()
    }

    override suspend fun seedHistoricWorkouts(workouts: List<WorkoutRecord>) {
        for (w in workouts) {
            if (workoutDao.countByTimestamp(w.timestamp) == 0) {
                workoutDao.insert(WorkoutEntity.fromDomain(w.copy(id = 0)))
            }
        }
    }
}
