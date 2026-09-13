package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calisthenics_progress")
data class CalisthenicsProgressEntity(
    @PrimaryKey val stepId: String,
    val isUnlocked: Boolean = false,
    val isMastered: Boolean = false,
    val completedAttemptsCount: Int = 0,
    val bestRepsOrSeconds: Int = 0,
    val lastAttemptTimestamp: Long = 0L
)
