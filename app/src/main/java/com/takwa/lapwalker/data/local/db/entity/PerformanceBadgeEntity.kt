package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_badges")
data class PerformanceBadgeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long = 0L
)
