package com.takwa.lapwalker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.takwa.lapwalker.data.local.db.entity.ArmoryQuestEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    // User Rank Profile
    @Query("SELECT * FROM user_rank_profile WHERE id = 1")
    fun getRankProfileFlow(): Flow<UserRankProfileEntity?>

    @Query("SELECT * FROM user_rank_profile WHERE id = 1")
    suspend fun getRankProfile(): UserRankProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRankProfile(profile: UserRankProfileEntity)

    // Streak Status
    @Query("SELECT * FROM streak_status WHERE id = 1")
    fun getStreakStatusFlow(): Flow<StreakStatusEntity?>

    @Query("SELECT * FROM streak_status WHERE id = 1")
    suspend fun getStreakStatus(): StreakStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStreakStatus(status: StreakStatusEntity)

    // Daily Bounties
    @Query("SELECT * FROM daily_bounties WHERE dateEpochDay = :epochDay")
    fun getBountiesForDay(epochDay: Long): Flow<List<DailyBountyEntity>>

    @Query("SELECT * FROM daily_bounties WHERE dateEpochDay = :epochDay")
    suspend fun getBountiesForDayList(epochDay: Long): List<DailyBountyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBounties(bounties: List<DailyBountyEntity>)

    @Update
    suspend fun updateBounty(bounty: DailyBountyEntity)

    // Armory Quests
    @Query("SELECT * FROM armory_quests WHERE dateEpochDay = :epochDay")
    fun getQuestsForDay(epochDay: Long): Flow<List<ArmoryQuestEntity>>

    @Query("SELECT * FROM armory_quests WHERE dateEpochDay = :epochDay")
    suspend fun getQuestsForDayList(epochDay: Long): List<ArmoryQuestEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuests(quests: List<ArmoryQuestEntity>)

    @Update
    suspend fun updateQuest(quest: ArmoryQuestEntity)

    // Performance Badges
    @Query("SELECT * FROM performance_badges")
    fun getAllBadgesFlow(): Flow<List<PerformanceBadgeEntity>>

    @Query("SELECT * FROM performance_badges")
    suspend fun getAllBadges(): List<PerformanceBadgeEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<PerformanceBadgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBadge(badge: PerformanceBadgeEntity)
}
