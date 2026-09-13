package com.takwa.lapwalker.domain.repository

import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import kotlinx.coroutines.flow.Flow

interface CalisthenicsRepository {
    fun getAllProgress(): Flow<List<CalisthenicsProgressEntity>>
    suspend fun getProgressForStep(stepId: String): CalisthenicsProgressEntity?
    suspend fun ensureDefaultStepsUnlocked()
    suspend fun recordAttempt(
        stepId: String,
        setsCompleted: Int,
        targetSets: Int,
        repsOrSecondsAchieved: Int,
        wasBenchmarkPassed: Boolean
    )
}
