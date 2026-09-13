package com.takwa.lapwalker.domain.repository

import com.takwa.lapwalker.data.local.db.entity.ArmoryQuestEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import com.takwa.lapwalker.domain.model.WorkoutRecord
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {
    fun getRankProfile(): Flow<UserRankProfileEntity?>
    fun getStreakStatus(): Flow<StreakStatusEntity?>
    fun getDailyBounties(): Flow<List<DailyBountyEntity>>
    fun getArmoryQuests(): Flow<List<ArmoryQuestEntity>>
    fun getAllBadges(): Flow<List<PerformanceBadgeEntity>>

    suspend fun ensureInitialized()
    suspend fun onWorkoutCompleted(workout: WorkoutRecord)
    suspend fun onCalisthenicsCompleted(stepId: String, setsCompleted: Int, wasBenchmarkPassed: Boolean)
    suspend fun completeArmoryQuest(questId: String)
}
