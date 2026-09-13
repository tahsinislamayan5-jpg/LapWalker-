package com.takwa.lapwalker.domain.usecase

import com.takwa.lapwalker.data.local.db.entity.ArmoryQuestEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import com.takwa.lapwalker.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class GamificationState(
    val rankProfile: UserRankProfileEntity? = null,
    val streakStatus: StreakStatusEntity? = null,
    val bounties: List<DailyBountyEntity> = emptyList(),
    val quests: List<ArmoryQuestEntity> = emptyList(),
    val badges: List<PerformanceBadgeEntity> = emptyList()
)

class GetGamificationStateUseCase(
    private val repository: GamificationRepository
) {
    operator fun invoke(): Flow<GamificationState> {
        return combine(
            repository.getRankProfile(),
            repository.getStreakStatus(),
            repository.getDailyBounties(),
            repository.getArmoryQuests(),
            repository.getAllBadges()
        ) { rank, streak, bounties, quests, badges ->
            GamificationState(
                rankProfile = rank,
                streakStatus = streak,
                bounties = bounties,
                quests = quests,
                badges = badges
            )
        }
    }
}
