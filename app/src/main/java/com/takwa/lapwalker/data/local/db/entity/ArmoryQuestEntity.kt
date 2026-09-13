package com.takwa.lapwalker.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "armory_quests")
data class ArmoryQuestEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val rewardType: String,
    val isCompleted: Boolean = false,
    val dateEpochDay: Long
)
