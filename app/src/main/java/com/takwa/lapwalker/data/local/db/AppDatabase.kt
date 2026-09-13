package com.takwa.lapwalker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.takwa.lapwalker.data.local.db.dao.WorkoutDao
import com.takwa.lapwalker.data.local.db.entity.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
