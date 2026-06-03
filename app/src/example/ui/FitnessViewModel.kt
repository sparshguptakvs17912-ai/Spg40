package com.example.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerStateEntity
import com.example.data.QuestEntity
import com.example.data.QuestRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitnessViewModel(private val repository: QuestRepository) : ViewModel() {

    // Toast event flow for game UI notifications
    private val _notification = MutableSharedFlow<ToastMessage>(replay = 0)
    val notification: SharedFlow<ToastMessage> = _notification.asSharedFlow()

    // Default or reactive Player State
    val playerState: StateFlow<PlayerStateEntity?> = repository.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Reactive Quests List
    val allQuests: StateFlow<List<QuestEntity>> = repository.allQuests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Seed default quests if list is empty
            if (repository.allQuests.first().isEmpty()) {
                repository.seedQuestsIfEmpty()
            }
            // Seed a default player profile if none exists
            if (repository.playerState.first() == null) {
                repository.savePlayerState(PlayerStateEntity())
            }
        }
    }

    fun getExpNeededForLevel(level: Int): Int {
        return minOf(100 + (level - 1) * 45, 600)
    }

    private fun addExp(amount: Int, currentPlayer: PlayerStateEntity): PlayerStateEntity {
        var currentExp = currentPlayer.exp + amount
        var currentLevel = currentPlayer.level
        var shadowPoints = currentPlayer.shadowPoints
        var leveledUp = false

        var needed = getExpNeededForLevel(currentLevel)
        while (currentExp >= needed && currentLevel < 50) {
            currentExp -= needed
            currentLevel++
            shadowPoints += 8 // Bonus points on level up
            leveledUp = true
            needed = getExpNeededForLevel(currentLevel)
            showNotification("⭐ LEVEL UP! Now Level $currentLevel | +8 Shadow Points ⭐", isWarning = false)
        }

        if (currentExp < 0) currentExp = 0
        return currentPlayer.copy(
            level = currentLevel,
            exp = currentExp,
            shadowPoints = shadowPoints
        )
    }

    fun upgradeStat(statName: String) {
        viewModelScope.launch {
            val currentPlayer = repository.playerState.first() ?: PlayerStateEntity()
            if (currentPlayer.shadowPoints <= 0) {
                showNotification("❌ Not enough Shadow Points! Complete more 100-rep quests!", isWarning = true)
                return@launch
            }

            val updatedPlayer = when (statName.lowercase()) {
                "strength" -> currentPlayer.copy(
                    shadowPoints = currentPlayer.shadowPoints - 1,
                    strength = currentPlayer.strength + 1
                )
                "agility" -> currentPlayer.copy(
                    shadowPoints = currentPlayer.shadowPoints - 1,
                    agility = currentPlayer.agility + 1
                )
                "vitality" -> currentPlayer.copy(
                    shadowPoints = currentPlayer.shadowPoints - 1,
                    vitality = currentPlayer.vitality + 1
                )
                "intellect" -> currentPlayer.copy(
                    shadowPoints = currentPlayer.shadowPoints - 1,
                    intellect = currentPlayer.intellect + 1
                )
                else -> currentPlayer
            }

            repository.savePlayerState(updatedPlayer)
            showNotification("✨ ${statName.uppercase()} increased to ${getStatValue(updatedPlayer, statName)}!", isWarning = false)
        }
    }

    private fun getStatValue(player: PlayerStateEntity, statName: String): Int {
        return when (statName.lowercase()) {
            "strength" -> player.strength
            "agility" -> player.agility
            "vitality" -> player.vitality
            "intellect" -> player.intellect
            else -> 10
        }
    }

    fun addProgressToQuest(questId: Int, amount: Int) {
        viewModelScope.launch {
            val quests = repository.allQuests.first()
            val player = repository.playerState.first() ?: PlayerStateEntity()
            val quest = quests.find { it.id == questId } ?: return@launch

            if (quest.progress >= quest.requirement) {
                showNotification("⚠️ ${quest.name} already completed! Reset to continue.", isWarning = true)
                return@launch
            }

            val updatedProgress = minOf(quest.progress + amount, quest.requirement)
            val added = updatedProgress - quest.progress

            if (updatedProgress == quest.requirement && quest.progress < quest.requirement) {
                val updatedPlayer = addExp(quest.expReward, player).copy(
                    shadowPoints = player.shadowPoints + quest.pointsReward
                )
                repository.savePlayerState(updatedPlayer)
                showNotification("✅ QUEST COMPLETE: ${quest.name} | +${quest.expReward} EXP, +${quest.pointsReward} SP", isWarning = false)
            } else if (added > 0) {
                showNotification("🏋️ ${quest.name}: $updatedProgress/${quest.requirement} ${quest.unit} (+$added)", isWarning = false)
            }

            repository.saveQuest(quest.copy(progress = updatedProgress))
        }
    }

    fun addToAllQuests(amount: Int) {
        viewModelScope.launch {
            val quests = repository.allQuests.first()
            var player = repository.playerState.first() ?: PlayerStateEntity()
            var anyProgress = false

            for (quest in quests) {
                if (quest.progress < quest.requirement) {
                    val updatedProgress = minOf(quest.progress + amount, quest.requirement)
                    val added = updatedProgress - quest.progress
                    if (added > 0) {
                        anyProgress = true
                        if (updatedProgress == quest.requirement) {
                            player = addExp(quest.expReward, player).copy(
                                shadowPoints = player.shadowPoints + quest.pointsReward
                            )
                            showNotification("✅ ${quest.name} COMPLETED! (+${quest.pointsReward} SP)", isWarning = false)
                        }
                        repository.saveQuest(quest.copy(progress = updatedProgress))
                    }
                }
            }
            repository.savePlayerState(player)
            if (!anyProgress) {
                showNotification("All quests are already completed! Reset to continue training.", isWarning = true)
            } else {
                showNotification("💪 Added +$amount progress to active challenges!", isWarning = false)
            }
        }
    }

    fun completeAllQuests() {
        viewModelScope.launch {
            val quests = repository.allQuests.first()
            var player = repository.playerState.first() ?: PlayerStateEntity()
            var anyCompleted = false

            for (quest in quests) {
                if (quest.progress < quest.requirement) {
                    val remaining = quest.requirement - quest.progress
                    player = addExp(quest.expReward, player).copy(
                        shadowPoints = player.shadowPoints + quest.pointsReward
                    )
                    anyCompleted = true
                    repository.saveQuest(quest.copy(progress = quest.requirement))
                }
            }
            repository.savePlayerState(player)
            if (!anyCompleted) {
                showNotification("All quests already completed! Reset to start new challenge.", isWarning = true)
            } else {
                showNotification("🏆 ALL QUESTS COMPLETED! The Shadow Monarch rises! 🏆", isWarning = false)
            }
        }
    }

    fun resetQuests() {
        viewModelScope.launch {
            repository.resetQuests()
            showNotification("🔄 All 100-rep quests reset. Face the challenge anew!", isWarning = false)
        }
    }

    fun purchaseItem(itemId: String, cost: Int) {
        viewModelScope.launch {
            val player = repository.playerState.first() ?: PlayerStateEntity()
            if (player.shadowPoints < cost) {
                showNotification("❌ Need $cost Shadow Points! Complete more quests!", isWarning = true)
                return@launch
            }

            var updatedPlayer = player.copy(shadowPoints = player.shadowPoints - cost)

            when (itemId) {
                "expPotion" -> {
                    updatedPlayer = addExp(75, updatedPlayer)
                    showNotification("🪄 Potion Consumed: +75 EXP gained!", isWarning = false)
                }
                "spTome" -> {
                    updatedPlayer = updatedPlayer.copy(shadowPoints = updatedPlayer.shadowPoints + 5)
                    showNotification("📖 Shadow Tome deciphered! +5 Shadow Points acquired!", isWarning = false)
                }
                "strBoost" -> {
                    updatedPlayer = updatedPlayer.copy(
                        strength = updatedPlayer.strength + 3,
                        agility = updatedPlayer.agility + 3,
                        vitality = updatedPlayer.vitality + 3,
                        intellect = updatedPlayer.intellect + 3
                    )
                    showNotification("⚡ STRENGTH BOOST applied! All attributes increased by +3!", isWarning = false)
                }
                "doubleExp" -> {
                    updatedPlayer = addExp(150, updatedPlayer)
                    showNotification("✨ EXP Booster infused! +150 EXP gained. Power surging!", isWarning = false)
                }
                "resetStone" -> {
                    updatedPlayer = updatedPlayer.copy(shadowPoints = updatedPlayer.shadowPoints + 20)
                    showNotification("💎 Reset Stone shattered! +20 Shadow Points acquired!", isWarning = false)
                }
            }

            repository.savePlayerState(updatedPlayer)
        }
    }

    private fun showNotification(message: String, isWarning: Boolean) {
        viewModelScope.launch {
            _notification.emit(ToastMessage(message, isWarning, System.currentTimeMillis()))
        }
    }
}

data class ToastMessage(
    val text: String,
    val isWarning: Boolean,
    val timestamp: Long
)

class FitnessViewModelFactory(private val repository: QuestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitnessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
