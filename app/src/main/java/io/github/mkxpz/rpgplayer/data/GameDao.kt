package io.github.mkxpz.rpgplayer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayedAt DESC, addedAt DESC")
    fun observeGames(): Flow<List<GameEntry>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGame(id: String): GameEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: GameEntry)

    @Update
    suspend fun update(entry: GameEntry)

    @Delete
    suspend fun delete(entry: GameEntry)
}
