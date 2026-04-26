package io.github.mkxpz.rpgplayer.data

import android.content.Context
import android.net.Uri
import io.github.mkxpz.rpgplayer.domain.GameDetection
import io.github.mkxpz.rpgplayer.domain.GameDirectoryDetector
import io.github.mkxpz.rpgplayer.domain.GameValidationException
import io.github.mkxpz.rpgplayer.domain.MkxpConfigWriter
import io.github.mkxpz.rpgplayer.domain.safeFileName
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GameRepository(
    private val context: Context,
    private val dao: GameDao,
    private val detector: GameDirectoryDetector,
    private val configWriter: MkxpConfigWriter,
    private val settingsRepository: SettingsRepository,
) {
    val games: Flow<List<GameEntry>> = dao.observeGames()

    suspend fun getGame(id: String): GameEntry? = dao.getGame(id)

    suspend fun addCopiedGame(copiedRoot: File, originalUri: Uri): GameEntry =
        withContext(Dispatchers.IO) {
            val detection = detector.detect(copiedRoot)
            val entry = createEntry(
                detection = detection,
                installedPath = copiedRoot.absolutePath,
                originalUri = originalUri.toString(),
                mode = StoredImportMode.COPIED_TO_LIBRARY,
            )
            writeConfig(entry)
            dao.insert(entry)
            entry
        }

    suspend fun addDirectPath(path: String): GameEntry = withContext(Dispatchers.IO) {
        val root = File(path.trim())
        val detection = detector.detect(root)
        val entry = createEntry(
            detection = detection,
            installedPath = root.absolutePath,
            originalUri = null,
            mode = StoredImportMode.DIRECT_EXTERNAL_PATH,
        )
        writeConfig(entry)
        dao.insert(entry)
        entry
    }

    suspend fun markLaunched(entry: GameEntry) {
        dao.update(
            entry.copy(
                lastPlayedAt = System.currentTimeMillis(),
                launchCount = entry.launchCount + 1,
            ),
        )
    }

    suspend fun delete(entry: GameEntry, removeFiles: Boolean) = withContext(Dispatchers.IO) {
        dao.delete(entry)
        if (removeFiles && entry.importMode == StoredImportMode.COPIED_TO_LIBRARY) {
            File(entry.installedPath).deleteRecursively()
        }
        File(entry.configPath).delete()
    }

    suspend fun writeConfig(entry: GameEntry): File {
        val settings = settingsRepository.settings.first()
        return configWriter.writeConfig(entry, settings, File(entry.configPath))
    }

    fun nextLibraryFolder(title: String, id: String): File {
        return File(context.filesDir, "games/${title.safeFileName()}-$id")
    }

    private fun createEntry(
        detection: GameDetection,
        installedPath: String,
        originalUri: String?,
        mode: StoredImportMode,
    ): GameEntry {
        if (installedPath.isBlank()) {
            throw GameValidationException("游戏路径为空")
        }
        val id = UUID.randomUUID().toString()
        val configPath = File(context.filesDir, "configs/$id/mkxp.json").absolutePath
        return GameEntry(
            id = id,
            title = detection.title,
            installedPath = installedPath,
            originalUri = originalUri,
            importMode = mode,
            engine = detection.engine,
            configPath = configPath,
            hasEncryptedArchive = detection.hasEncryptedArchive,
            addedAt = System.currentTimeMillis(),
        )
    }
}
