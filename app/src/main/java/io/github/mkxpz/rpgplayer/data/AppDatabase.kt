package io.github.mkxpz.rpgplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [GameEntry::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}

class AppTypeConverters {
    @TypeConverter
    fun importModeToString(value: StoredImportMode): String = value.name

    @TypeConverter
    fun stringToImportMode(value: String): StoredImportMode = StoredImportMode.valueOf(value)

    @TypeConverter
    fun engineToString(value: RpgMakerEngine): String = value.name

    @TypeConverter
    fun stringToEngine(value: String): RpgMakerEngine = RpgMakerEngine.valueOf(value)
}
