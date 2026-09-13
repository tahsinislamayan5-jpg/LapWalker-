package com.takwa.lapwalker.data.repository

import com.takwa.lapwalker.data.catalog.CalisthenicsCatalog
import com.takwa.lapwalker.data.local.db.dao.CalisthenicsDao
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsAttemptEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import com.takwa.lapwalker.domain.repository.CalisthenicsRepository
import com.takwa.lapwalker.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class CalisthenicsRepositoryImpl(
    private val dao: CalisthenicsDao,
    private val gamificationRepository: GamificationRepository
) : CalisthenicsRepository {
    override fun getAllProgress(): Flow<List<CalisthenicsProgressEntity>> = dao.getAllProgress()

    override suspend fun getProgressForStep(stepId: String): CalisthenicsProgressEntity? =
        dao.getProgressForStep(stepId)

    override suspend fun ensureDefaultStepsUnlocked() {
        val defaults = listOf(
            CalisthenicsProgressEntity(stepId = "push_1", isUnlocked = true),
            CalisthenicsProgressEntity(stepId = "pull_1", isUnlocked = true),
            CalisthenicsProgressEntity(stepId = "legs_1", isUnlocked = true),
            CalisthenicsProgressEntity(stepId = "core_1", isUnlocked = true)
        )
        dao.insertInitialProgress(defaults)
    }

    override suspend fun recordAttempt(
        stepId: String,
        setsCompleted: Int,
        targetSets: Int,
        repsOrSecondsAchieved: Int,
        wasBenchmarkPassed: Boolean
    ) {
        val now = System.currentTimeMillis()
        dao.logAttempt(
            CalisthenicsAttemptEntity(
                stepId = stepId,
                timestamp = now,
                setsCompleted = setsCompleted,
                targetSets = targetSets,
                repsOrSecondsAchieved = repsOrSecondsAchieved,
                wasBenchmarkPassed = wasBenchmarkPassed
            )
        )

        val current = dao.getProgressForStep(stepId) ?: CalisthenicsProgressEntity(stepId = stepId, isUnlocked = true)
        val updated = current.copy(
            completedAttemptsCount = current.completedAttemptsCount + 1,
            bestRepsOrSeconds = max(current.bestRepsOrSeconds, repsOrSecondsAchieved),
            lastAttemptTimestamp = now,
            isMastered = current.isMastered || wasBenchmarkPassed
        )
        dao.saveProgress(updated)

        // Award Calisthenics XP, streak & evaluate level-gated unlocks
        gamificationRepository.onCalisthenicsCompleted(stepId, setsCompleted, wasBenchmarkPassed)
    }
}
