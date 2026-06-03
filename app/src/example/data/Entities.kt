package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity that holds the single-player's Solo Leveling training state.
 * Always has [id] = 1 for simple single-user persistence.
 */
@Entity(tableName = "player_state")
data class PlayerStateEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val shadowPoints: Int = 0,
    val strength: Int = 10,
    val agility: Int = 10,
    val vitality: Int = 10,
    val intellect: Int = 10
)

/**
 * Entity representing a daily challenge/quest.
 */
@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val requirement: Int,
    val progress: Int,
    val unit: String,
    val expReward: Int,
    val pointsReward: Int
)
