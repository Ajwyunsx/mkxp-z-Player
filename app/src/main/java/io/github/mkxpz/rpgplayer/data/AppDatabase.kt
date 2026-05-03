package io.github.mkxpz.rpgplayer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameEntry::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN customIconPath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN hasWinApiUsage INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
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
