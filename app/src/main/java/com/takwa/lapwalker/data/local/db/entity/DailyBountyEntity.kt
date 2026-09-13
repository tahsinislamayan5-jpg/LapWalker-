package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_bounties")
data class DailyBountyEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val targetType: String,
    val targetValue: Double,
    val currentProgress: Double,
    val isCompleted: Boolean = false,
    val dateEpochDay: Long
)
