package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_status")
data class StreakStatusEntity(
    @PrimaryKey
    val id: Int = 1,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastActiveDateEpochDay: Long = 0L,
    val freezeShieldsAvailable: Int = 0,
    val restTokensAvailable: Int = 0,
    val isPhoenixEmberActive: Boolean = false,
    val flameTier: String = "EMBER"
)
