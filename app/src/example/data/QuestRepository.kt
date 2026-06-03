package com.example.data

import kotlinx.coroutines.flow.Flow

class QuestRepository(private val database: AppDatabase) {
    val playerState: Flow<PlayerStateEntity?> = database.playerDao().getPlayerState()
    val allQuests: Flow<List<QuestEntity>> = database.questDao().getAllQuests()

    suspend fun savePlayerState(player: PlayerStateEntity) {
        database.playerDao().insertPlayerState(player)
    }

    suspend fun seedQuestsIfEmpty() {
        // We will seed standard challenges on first run if database is empty
        val defaultQuests = listOf(
            QuestEntity(0, "💪 IRON PUSH", "Complete 100 Push-ups", 100, 0, "reps", 120, 15),
            QuestEntity(1, "🦵 SHADOW SQUATS", "Complete 100 Squats", 100, 0, "reps", 110, 14),
            QuestEntity(2, "🏃 MONARCH'S RUN", "Run 10 km", 10, 0, "km", 150, 18),
            QuestEntity(3, "⏱️ ETERNAL PLANK", "Hold Plank 600 sec (10 min total)", 600, 0, "sec", 130, 16),
            QuestEntity(4, "🤸 ARISE PULL-UPS", "Complete 100 Pull-ups", 100, 0, "reps", 140, 17),
            QuestEntity(5, "🔥 DRAGON LUNGES", "Complete 100 Lunges", 100, 0, "reps", 110, 14),
            QuestEntity(6, "🌀 BURST BURPEES", "Complete 100 Burpees", 100, 0, "reps", 160, 20)
        )
        database.questDao().insertAll(defaultQuests)
    }

    suspend fun updateQuestProgress(id: Int, progress: Int) {
        database.questDao().updateQuestProgress(id, progress)
    }

    suspend fun saveQuest(quest: QuestEntity) {
        database.questDao().insertQuest(quest)
    }

    suspend fun resetQuests() {
        database.questDao().resetAllQuests()
    }
}
