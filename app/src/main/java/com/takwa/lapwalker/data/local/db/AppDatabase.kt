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
}
