package com.takwa.lapwalker.data.repository

import com.takwa.lapwalker.data.local.db.dao.CalisthenicsDao
import com.takwa.lapwalker.data.local.db.dao.GamificationDao
import com.takwa.lapwalker.data.local.db.dao.WorkoutDao
import com.takwa.lapwalker.data.local.db.entity.ArmoryQuestEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import com.takwa.lapwalker.domain.model.WorkoutRecord
import com.takwa.lapwalker.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

class GamificationRepositoryImpl(
    private val gamificationDao: GamificationDao,
    private val workoutDao: WorkoutDao,
    private val calisthenicsDao: CalisthenicsDao
) : GamificationRepository {

    override fun getRankProfile(): Flow<UserRankProfileEntity?> =
        gamificationDao.getRankProfileFlow()

    override fun getStreakStatus(): Flow<StreakStatusEntity?> =
        gamificationDao.getStreakStatusFlow()

    override fun getDailyBounties(): Flow<List<DailyBountyEntity>> =
        gamificationDao.getBountiesForDay(getTodayEpochDay())

    override fun getArmoryQuests(): Flow<List<ArmoryQuestEntity>> =
        gamificationDao.getQuestsForDay(getTodayEpochDay())

    override fun getAllBadges(): Flow<List<PerformanceBadgeEntity>> =
        gamificationDao.getAllBadgesFlow()

    private fun getTodayEpochDay(): Long {
        val offset = TimeZone.getDefault().getOffset(System.currentTimeMillis())
        return (System.currentTimeMillis() + offset) / 86_400_000L
    }

    override suspend fun ensureInitialized() {
        val today = getTodayEpochDay()

        // 1. Initialize 10 Badges if not present
        val existingBadges = gamificationDao.getAllBadges()
        if (existingBadges.isEmpty()) {
            val defaultBadges = listOf(
                PerformanceBadgeEntity("TRUE_MILE", "True Mile", "Walk >= 1.609 km in a single session"),
                PerformanceBadgeEntity("NEGATIVE_SPLIT", "Negative Split", "Lap pace improved in second half"),
                PerformanceBadgeEntity("SPEED_DEMON", "Speed Demon", "Exceptional brisk lap walking pace"),
                PerformanceBadgeEntity("IRON_2K", "Iron 2K", "Single session distance >= 2.0 km"),
                PerformanceBadgeEntity("GARAGE_5K", "Garage 5K", "Single session distance >= 5.0 km"),
                PerformanceBadgeEntity("CENTURION_10K", "Centurion 10K", "Lifetime career walking distance >= 10.0 km"),
                PerformanceBadgeEntity("ODYSSEY_25K", "Odyssey 25K", "Lifetime career walking distance >= 25.0 km"),
                PerformanceBadgeEntity("TITAN_50K", "Titan 50K", "Lifetime career walking distance >= 50.0 km"),
                PerformanceBadgeEntity("CENTURY_100K", "Century 100K", "Lifetime career walking distance >= 100.0 km"),
                PerformanceBadgeEntity("SPARTAN_250K", "Spartan 250K", "Lifetime career walking distance >= 250.0 km")
            )
            gamificationDao.insertBadges(defaultBadges)
        }

        // 2. Initialize Rank Profile (with retroactive XP from existing workouts)
        var profile = gamificationDao.getRankProfile()
        if (profile == null) {
            val totalDistanceKm = workoutDao.getTotalDistanceKm() ?: 0.0
            val workouts = workoutDao.getRecentWorkouts(1000)
            val workoutCount = workouts.size

            // 10 XP per 100m (= 100 XP / km) + 50 XP per completed workout
            val retroactiveXp = ((totalDistanceKm * 1000.0) / 100.0).toInt() * 10 + (workoutCount * 50)
            val level = min(50, 1 + (retroactiveXp / 1000))
            val division = getDivisionForLevel(level)

            profile = UserRankProfileEntity(
                id = 1,
                totalXp = retroactiveXp,
                currentLevel = level,
                divisionName = division.first,
                divisionTier = division.second,
                isLevel50Apex = level >= 50
            )
            gamificationDao.saveRankProfile(profile)

            // Retroactively evaluate badges on historic data
            evaluateBadgesOnWorkouts(workouts, totalDistanceKm)
        }

        // 3. Initialize Streak Status
        var streak = gamificationDao.getStreakStatus()
        if (streak == null) {
            streak = StreakStatusEntity(
                id = 1,
                currentStreakDays = if ((workoutDao.getTotalLaps() ?: 0) > 0) 1 else 0,
                longestStreakDays = if ((workoutDao.getTotalLaps() ?: 0) > 0) 1 else 0,
                lastActiveDateEpochDay = today,
                flameTier = if ((workoutDao.getTotalLaps() ?: 0) > 0) "EMBER" else "EXTINGUISHED"
            )
            gamificationDao.saveStreakStatus(streak)
        }

        // 4. Seed Today's Bounties
        val bounties = gamificationDao.getBountiesForDayList(today)
        if (bounties.isEmpty()) {
            val todayBounties = listOf(
                DailyBountyEntity(
                    id = "bounty_${today}_500m",
                    title = "Garage Warmup",
                    description = "Walk at least 500 meters today",
                    xpReward = 50,
                    targetType = "DISTANCE_METERS",
                    targetValue = 500.0,
                    currentProgress = 0.0,
                    isCompleted = false,
                    dateEpochDay = today
                ),
                DailyBountyEntity(
                    id = "bounty_${today}_1km",
                    title = "Cadence Push",
                    description = "Complete 1.0 km in garage walking",
                    xpReward = 75,
                    targetType = "DISTANCE_METERS",
                    targetValue = 1000.0,
                    currentProgress = 0.0,
                    isCompleted = false,
                    dateEpochDay = today
                ),
                DailyBountyEntity(
                    id = "bounty_${today}_strength",
                    title = "Form Discipline",
                    description = "Complete at least 1 Calisthenics set",
                    xpReward = 60,
                    targetType = "CALISTHENICS_PRACTICE",
                    targetValue = 1.0,
                    currentProgress = 0.0,
                    isCompleted = false,
                    dateEpochDay = today
                )
            )
            gamificationDao.insertBounties(todayBounties)
        }

        // 5. Seed Today's Armory Quest
        val quests = gamificationDao.getQuestsForDayList(today)
        if (quests.isEmpty()) {
            val todayQuest = ArmoryQuestEntity(
                id = "quest_${today}_mobility",
                title = "Mobility Warm-up (3 min)",
                description = "Perform 3 minutes of dynamic leg swings & shoulder openers",
                rewardType = "REST_TOKEN",
                isCompleted = false,
                dateEpochDay = today
            )
            gamificationDao.insertQuests(listOf(todayQuest))
        }

        // 6. Check and unlock calisthenics steps for current level
        evaluateCalisthenicsUnlocks(profile.currentLevel)
    }

    override suspend fun onWorkoutCompleted(workout: WorkoutRecord) {
        val today = getTodayEpochDay()

        // 1. Calculate XP: 10 XP per 100m + 50 XP completion bonus
        val distanceMeters = workout.distanceKm * 1000.0
        val earnedXp = ((distanceMeters / 100.0).toInt() * 10) + 50
        addXp(earnedXp)

        // 2. Update Streak
        updateStreakOnActivity(today)

        // 3. Update Bounties
        val bounties = gamificationDao.getBountiesForDayList(today)
        for (bounty in bounties) {
            if (!bounty.isCompleted && bounty.targetType == "DISTANCE_METERS") {
                val newProgress = bounty.currentProgress + distanceMeters
                val isCompleted = newProgress >= bounty.targetValue
                gamificationDao.updateBounty(
                    bounty.copy(
                        currentProgress = newProgress,
                        isCompleted = isCompleted
                    )
                )
                if (isCompleted && !bounty.isCompleted) {
                    addXp(bounty.xpReward)
                }
            }
        }

        // 4. Evaluate Badges
        val allWorkouts = workoutDao.getRecentWorkouts(1000)
        val totalDistanceKm = workoutDao.getTotalDistanceKm() ?: 0.0
        evaluateBadgesOnWorkouts(allWorkouts, totalDistanceKm)

        // 5. Level 50 Apex weekly volume tracking
        val profile = gamificationDao.getRankProfile()
        if (profile != null && profile.isLevel50Apex) {
            val updatedApexDistance = profile.apexWeeklyDistanceMeters + distanceMeters
            gamificationDao.saveRankProfile(
                profile.copy(apexWeeklyDistanceMeters = updatedApexDistance)
            )
        }
    }

    override suspend fun onCalisthenicsCompleted(stepId: String, setsCompleted: Int, wasBenchmarkPassed: Boolean) {
        val today = getTodayEpochDay()

        // 30 XP per completed set + 100 XP mastery bonus
        val earnedXp = (setsCompleted * 30) + (if (wasBenchmarkPassed) 100 else 0)
        addXp(earnedXp)

        // Update streak
        updateStreakOnActivity(today)

        // Update Bounties
        val bounties = gamificationDao.getBountiesForDayList(today)
        for (bounty in bounties) {
            if (!bounty.isCompleted && bounty.targetType == "CALISTHENICS_PRACTICE") {
                val newProgress = bounty.currentProgress + setsCompleted
                val isCompleted = newProgress >= bounty.targetValue
                gamificationDao.updateBounty(
                    bounty.copy(
                        currentProgress = newProgress,
                        isCompleted = isCompleted
                    )
                )
                if (isCompleted && !bounty.isCompleted) {
                    addXp(bounty.xpReward)
                }
            }
        }

        // If benchmark passed, evaluate unlocks
        val currentLevel = gamificationDao.getRankProfile()?.currentLevel ?: 1
        evaluateCalisthenicsUnlocks(currentLevel)
    }

    override suspend fun completeArmoryQuest(questId: String) {
        val today = getTodayEpochDay()
        val quests = gamificationDao.getQuestsForDayList(today)
        val quest = quests.find { it.id == questId } ?: return
        if (quest.isCompleted) return

        gamificationDao.updateQuest(quest.copy(isCompleted = true))

        // Grant reward
        val streak = gamificationDao.getStreakStatus() ?: return
        if (quest.rewardType == "REST_TOKEN") {
            val newTokens = min(2, streak.restTokensAvailable + 1)
            gamificationDao.saveStreakStatus(streak.copy(restTokensAvailable = newTokens))
        } else if (quest.rewardType == "SHIELD") {
            val newShields = min(2, streak.freezeShieldsAvailable + 1)
            gamificationDao.saveStreakStatus(streak.copy(freezeShieldsAvailable = newShields))
        }
    }

    private suspend fun addXp(amount: Int) {
        if (amount <= 0) return
        val current = gamificationDao.getRankProfile() ?: UserRankProfileEntity()
        val newXp = current.totalXp + amount
        val newLevel = min(50, 1 + (newXp / 1000))
        val division = getDivisionForLevel(newLevel)

        val updated = current.copy(
            totalXp = newXp,
            currentLevel = newLevel,
            divisionName = division.first,
            divisionTier = division.second,
            isLevel50Apex = newLevel >= 50
        )
        gamificationDao.saveRankProfile(updated)

        if (newLevel > current.currentLevel) {
            evaluateCalisthenicsUnlocks(newLevel)
        }
    }

    private suspend fun updateStreakOnActivity(today: Long) {
        val current = gamificationDao.getStreakStatus() ?: StreakStatusEntity()
        if (current.lastActiveDateEpochDay == today) {
            return
        }

        val daysDiff = today - current.lastActiveDateEpochDay
        var newStreak = current.currentStreakDays
        var shields = current.freezeShieldsAvailable
        var tokens = current.restTokensAvailable

        if (current.lastActiveDateEpochDay == 0L || daysDiff == 1L) {
            newStreak += 1
        } else if (daysDiff > 1L) {
            // Missed days
            if (shields > 0) {
                shields -= 1
                newStreak += 1
            } else if (tokens > 0) {
                tokens -= 1
                newStreak += 1
            } else {
                newStreak = 1
            }
        }

        val longest = max(current.longestStreakDays, newStreak)
        val flameTier = when {
            newStreak >= 100 -> "SOLAR RADIANCE"
            newStreak >= 30 -> "COBALT BLAZE"
            newStreak >= 7 -> "VIBRANT AMBER"
            newStreak >= 1 -> "EMBER"
            else -> "EXTINGUISHED"
        }

        gamificationDao.saveStreakStatus(
            current.copy(
                currentStreakDays = newStreak,
                longestStreakDays = longest,
                lastActiveDateEpochDay = today,
                freezeShieldsAvailable = shields,
                restTokensAvailable = tokens,
                flameTier = flameTier
            )
        )
    }

    private suspend fun evaluateBadgesOnWorkouts(workouts: List<com.takwa.lapwalker.data.local.db.entity.WorkoutEntity>, totalDistanceKm: Double) {
        val now = System.currentTimeMillis()

        fun unlock(badgeId: String) {
            kotlinx.coroutines.runBlocking {
                val badge = gamificationDao.getAllBadges().find { it.id == badgeId }
                if (badge != null && !badge.isUnlocked) {
                    gamificationDao.saveBadge(badge.copy(isUnlocked = true, unlockedTimestamp = now))
                }
            }
        }

        if (workouts.any { it.distanceKm >= 1.609 }) unlock("TRUE_MILE")
        if (workouts.any { it.distanceKm >= 2.0 }) unlock("IRON_2K")
        if (workouts.any { it.distanceKm >= 5.0 }) unlock("GARAGE_5K")
        if (workouts.any { it.avgLapTimeSec in 10.0..35.0 }) unlock("SPEED_DEMON")
        if (workouts.size >= 2) unlock("NEGATIVE_SPLIT")

        if (totalDistanceKm >= 10.0) unlock("CENTURION_10K")
        if (totalDistanceKm >= 25.0) unlock("ODYSSEY_25K")
        if (totalDistanceKm >= 50.0) unlock("TITAN_50K")
        if (totalDistanceKm >= 100.0) unlock("CENTURY_100K")
        if (totalDistanceKm >= 250.0) unlock("SPARTAN_250K")
    }

    private suspend fun evaluateCalisthenicsUnlocks(userLevel: Int) {
        // Step requirement gates:
        // stepId -> Pair(requiredLevel, prerequisiteStepId)
        val gates = mapOf(
            "push_1" to Pair(1, null),
            "pull_1" to Pair(1, null),
            "legs_1" to Pair(1, null),
            "core_1" to Pair(1, null),
            "legs_2" to Pair(5, "legs_1"),
            "core_2" to Pair(10, "core_1"),
            "push_2" to Pair(15, "push_1"),
            "pull_2" to Pair(20, "pull_1"),
            "legs_3" to Pair(25, "legs_2"),
            "core_3" to Pair(30, "core_2"),
            "push_3" to Pair(35, "push_2"),
            "pull_3" to Pair(40, "pull_2"),
            "push_4" to Pair(45, "push_3"),
            "push_5" to Pair(49, "push_4")
        )

        for ((stepId, rule) in gates) {
            val reqLevel = rule.first
            val prereq = rule.second

            val currentProgress = calisthenicsDao.getProgressForStep(stepId) ?: CalisthenicsProgressEntity(stepId = stepId)
            if (currentProgress.isUnlocked) continue

            val levelMet = userLevel >= reqLevel
            val prereqMet = if (prereq == null) true else (calisthenicsDao.getProgressForStep(prereq)?.isMastered == true)

            if (levelMet && prereqMet) {
                calisthenicsDao.saveProgress(currentProgress.copy(isUnlocked = true))
            }
        }
    }

    private fun getDivisionForLevel(level: Int): Pair<String, Int> {
        return when (level) {
            in 1..4 -> Pair("RECRUIT", 1)
            in 5..9 -> Pair("IRON SCOUT", 2)
            in 10..14 -> Pair("IRON SPECIALIST", 3)
            in 15..19 -> Pair("STEEL OPERATIVE", 4)
            in 20..24 -> Pair("STEEL VANGUARD", 5)
            in 25..29 -> Pair("MITHRIL VETERAN", 6)
            in 30..34 -> Pair("MITHRIL SENTINEL", 7)
            in 35..39 -> Pair("ADAMANT JUGGERNAUT", 8)
            in 40..44 -> Pair("ADAMANT COMMANDER", 9)
            in 45..49 -> Pair("PRE-APEX GUARDIAN", 10)
            else -> Pair("APEX IMMORTAL", 10)
        }
    }
}
