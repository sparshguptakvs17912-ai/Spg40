package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_state WHERE id = 1 LIMIT 1")
    fun getPlayerState(): Flow<PlayerStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerState(player: PlayerStateEntity)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY id ASC")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quests: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity)

    @Query("UPDATE quests SET progress = :progress WHERE id = :id")
    suspend fun updateQuestProgress(id: Int, progress: Int)

    @Query("UPDATE quests SET progress = 0")
    suspend fun resetAllQuests()
}

@Database(entities = [PlayerStateEntity::class, QuestEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arise_fitness_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
