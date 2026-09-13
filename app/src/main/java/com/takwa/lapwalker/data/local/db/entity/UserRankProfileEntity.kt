package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_rank_profile")
data class UserRankProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val divisionName: String = "RECRUIT",
    val divisionTier: Int = 1,
    val isLevel50Apex: Boolean = false,
    val apexWeeklyDistanceMeters: Double = 0.0,
    val apexWeekStartEpoch: Long = 0L,
    val apexDailyDutyCompleted: Boolean = false,
    val apexBountyCompleted: Boolean = false
)
