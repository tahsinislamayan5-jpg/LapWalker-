package com.takwa.lapwalker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.takwa.lapwalker.data.local.db.dao.CalisthenicsDao
import com.takwa.lapwalker.data.local.db.dao.WorkoutDao
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsAttemptEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import com.takwa.lapwalker.data.local.db.entity.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        CalisthenicsProgressEntity::class,
        CalisthenicsAttemptEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun calisthenicsDao(): CalisthenicsDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
    }
}
