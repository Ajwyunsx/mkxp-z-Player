package io.github.mkxpz.rpgplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntry(
    @PrimaryKey val id: String,
    val title: String,
    val installedPath: String,
    val originalUri: String?,
    val importMode: StoredImportMode,
    val engine: RpgMakerEngine,
    val configPath: String,
    val hasEncryptedArchive: Boolean,
    val addedAt: Long,
    val lastPlayedAt: Long? = null,
    val launchCount: Int = 0,
)

enum class StoredImportMode {
    COPIED_TO_LIBRARY,
    DIRECT_EXTERNAL_PATH,
}

enum class RpgMakerEngine(val rgssVersion: Int, val displayName: String) {
    XP(1, "RPG Maker XP"),
    VX(2, "RPG Maker VX"),
    VX_ACE(3, "RPG Maker VX Ace"),
    MV(0, "RPG Maker MV"),
    MZ(0, "RPG Maker MZ"),
    UNKNOWN(0, "Auto detect"),
}

fun RpgMakerEngine.isRgss(): Boolean = this == RpgMakerEngine.XP ||
    this == RpgMakerEngine.VX ||
    this == RpgMakerEngine.VX_ACE ||
    this == RpgMakerEngine.UNKNOWN

fun RpgMakerEngine.isHtml5(): Boolean = this == RpgMakerEngine.MV ||
    this == RpgMakerEngine.MZ
