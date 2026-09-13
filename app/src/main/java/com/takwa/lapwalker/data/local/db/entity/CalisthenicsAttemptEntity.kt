package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calisthenics_attempts")
data class CalisthenicsAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stepId: String,
    val timestamp: Long,
    val setsCompleted: Int,
    val targetSets: Int,
    val repsOrSecondsAchieved: Int,
    val wasBenchmarkPassed: Boolean
)
