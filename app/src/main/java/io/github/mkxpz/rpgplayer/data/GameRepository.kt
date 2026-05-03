package io.github.mkxpz.rpgplayer.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.github.mkxpz.rpgplayer.domain.GameDetection
import io.github.mkxpz.rpgplayer.domain.GameDirectoryDetector
import io.github.mkxpz.rpgplayer.domain.GameValidationException
import io.github.mkxpz.rpgplayer.domain.MkxpConfigWriter
import io.github.mkxpz.rpgplayer.domain.PackedRgssGameExtractor
import io.github.mkxpz.rpgplayer.domain.safeFileName
import java.io.File
import java.util.Locale
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

    suspend fun getByOriginalUri(originalUri: String): GameEntry? = dao.getByOriginalUri(originalUri)

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
        if (detection.packedRgssExecutableName != null) {
            throw GameValidationException("打包 exe 游戏需要先解包到播放器库，请使用添加游戏复制导入")
        }
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
        deleteManagedIcon(entry.customIconPath)
    }

    suspend fun writeConfig(entry: GameEntry): File = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        repairCopiedPackedAudio(entry)
        configWriter.writeConfig(entry, settings, File(entry.configPath))
    }

    suspend fun updateMetadata(
        entry: GameEntry,
        title: String,
        iconUri: Uri?,
        clearIcon: Boolean,
    ): GameEntry = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            throw GameValidationException("游戏名称不能为空")
        }

        val nextIconPath = when {
            clearIcon -> {
                deleteManagedIcon(entry.customIconPath)
                null
            }
            iconUri != null -> {
                deleteManagedIcon(entry.customIconPath)
                copyIconToLibrary(entry.id, iconUri).absolutePath
            }
            else -> entry.customIconPath
        }

        val updated = entry.copy(
            title = trimmedTitle,
            customIconPath = nextIconPath,
        )
        writeConfig(updated)
        dao.update(updated)
        updated
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
            hasWinApiUsage = detection.hasWinApiUsage,
            addedAt = System.currentTimeMillis(),
        )
    }

    private fun copyIconToLibrary(gameId: String, iconUri: Uri): File {
        val iconDir = File(context.filesDir, "game-icons").apply { mkdirs() }
        val output = File(iconDir, "$gameId.icon")
        context.contentResolver.openInputStream(iconUri).use { input ->
            requireNotNull(input) { "无法读取图标文件" }
            output.outputStream().use { outputStream ->
                input.copyTo(outputStream)
            }
        }
        return output
    }

    private fun deleteManagedIcon(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }
        val iconDir = File(context.filesDir, "game-icons")
        val iconFile = File(path)
        if (iconFile.parentFile?.absolutePath == iconDir.absolutePath) {
            iconFile.delete()
        }
    }

    private fun repairCopiedPackedAudio(entry: GameEntry) {
        if (
            !entry.hasEncryptedArchive ||
            entry.importMode != StoredImportMode.COPIED_TO_LIBRARY ||
            !entry.engine.isRgss()
        ) {
            return
        }
        val installedRoot = File(entry.installedPath)
        if (!installedRoot.isDirectory) {
            return
        }
        val extractionMarker = File(installedRoot, PackedRgssGameExtractor.EVB_EXTRACTION_MARKER)
        if (extractionMarker.isFile && hasBgmFiles(installedRoot)) {
            return
        }

        val original = entry.originalUri?.takeIf { it.isNotBlank() } ?: return
        val uri = runCatching { Uri.parse(original) }.getOrNull() ?: return
        runCatching {
            when (uri.scheme?.lowercase(Locale.US)) {
                ContentResolver.SCHEME_FILE -> {
                    val sourceRoot = uri.path?.let(::File) ?: return@runCatching
                    val executable = findPackedExecutable(sourceRoot) ?: return@runCatching
                    PackedRgssGameExtractor.extractEmbeddedVirtualResources(executable, installedRoot)
                }
                ContentResolver.SCHEME_CONTENT -> {
                    val sourceRoot = DocumentFile.fromTreeUri(context, uri) ?: return@runCatching
                    val executable = findPackedExecutable(sourceRoot) ?: return@runCatching
                    PackedRgssGameExtractor.extractEmbeddedVirtualResources(
                        executable = executable,
                        contentResolver = context.contentResolver,
                        destination = installedRoot,
                    )
                }
                null -> {
                    val sourceRoot = File(original)
                    val executable = findPackedExecutable(sourceRoot) ?: return@runCatching
                    PackedRgssGameExtractor.extractEmbeddedVirtualResources(executable, installedRoot)
                }
            }
        }
    }

    private fun hasBgmFiles(root: File): Boolean {
        val audio = findChildDirectory(root, "Audio") ?: return false
        val bgm = findChildDirectory(audio, "BGM") ?: return false
        return bgm.walkTopDown().any { file ->
            file.isFile &&
                file.length() > 0L &&
                file.extension.lowercase(Locale.US) in supportedAudioExtensions
        }
    }

    private fun findChildDirectory(root: File, name: String): File? {
        return root.listFiles()
            .orEmpty()
            .firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }
    }

    private fun findPackedExecutable(root: File): File? {
        if (!root.isDirectory) return null
        return root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("exe", ignoreCase = true) }
            .sortedWith(
                compareBy<File> { if (it.name.equals("Game.exe", ignoreCase = true)) 0 else 1 }
                    .thenByDescending { it.length() },
            )
            .firstOrNull()
    }

    private fun findPackedExecutable(root: DocumentFile): DocumentFile? {
        if (!root.isDirectory) return null
        return root.listFiles()
            .filter { it.isFile && it.name?.endsWith(".exe", ignoreCase = true) == true }
            .sortedWith(
                compareBy<DocumentFile> { if (it.name.equals("Game.exe", ignoreCase = true)) 0 else 1 }
                    .thenByDescending { it.length() },
            )
            .firstOrNull()
    }

    private companion object {
        private val supportedAudioExtensions = setOf("ogg", "oga", "mp3", "mp2", "mp1", "wav", "mid", "midi")
    }
}
