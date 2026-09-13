package com.takwa.lapwalker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.takwa.lapwalker.data.local.db.dao.CalisthenicsDao
import com.takwa.lapwalker.data.local.db.dao.GamificationDao
import com.takwa.lapwalker.data.local.db.dao.WorkoutDao
import com.takwa.lapwalker.data.local.db.entity.ArmoryQuestEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsAttemptEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import com.takwa.lapwalker.data.local.db.entity.DailyBountyEntity
import com.takwa.lapwalker.data.local.db.entity.PerformanceBadgeEntity
import com.takwa.lapwalker.data.local.db.entity.StreakStatusEntity
import com.takwa.lapwalker.data.local.db.entity.UserRankProfileEntity
import com.takwa.lapwalker.data.local.db.entity.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        CalisthenicsProgressEntity::class,
        CalisthenicsAttemptEntity::class,
        UserRankProfileEntity::class,
        StreakStatusEntity::class,
        DailyBountyEntity::class,
        ArmoryQuestEntity::class,
        PerformanceBadgeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun calisthenicsDao(): CalisthenicsDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calisthenics_progress` (
                        `stepId` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `isMastered` INTEGER NOT NULL,
                        `completedAttemptsCount` INTEGER NOT NULL,
                        `bestRepsOrSeconds` INTEGER NOT NULL,
                        `lastAttemptTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`stepId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calisthenics_attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stepId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `setsCompleted` INTEGER NOT NULL,
                        `targetSets` INTEGER NOT NULL,
                        `repsOrSecondsAchieved` INTEGER NOT NULL,
                        `wasBenchmarkPassed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_rank_profile` (
                        `id` INTEGER NOT NULL,
                        `totalXp` INTEGER NOT NULL,
                        `currentLevel` INTEGER NOT NULL,
                        `divisionName` TEXT NOT NULL,
                        `divisionTier` INTEGER NOT NULL,
                        `isLevel50Apex` INTEGER NOT NULL,
                        `apexWeeklyDistanceMeters` REAL NOT NULL,
                        `apexWeekStartEpoch` INTEGER NOT NULL,
                        `apexDailyDutyCompleted` INTEGER NOT NULL,
                        `apexBountyCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `streak_status` (
                        `id` INTEGER NOT NULL,
                        `currentStreakDays` INTEGER NOT NULL,
                        `longestStreakDays` INTEGER NOT NULL,
                        `lastActiveDateEpochDay` INTEGER NOT NULL,
                        `freezeShieldsAvailable` INTEGER NOT NULL,
                        `restTokensAvailable` INTEGER NOT NULL,
                        `isPhoenixEmberActive` INTEGER NOT NULL,
                        `flameTier` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_bounties` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `currentProgress` REAL NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `armory_quests` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `rewardType` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `performance_badges` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `unlockedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
