package io.github.mkxpz.rpgplayer.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.GameRepository
import io.github.mkxpz.rpgplayer.data.isHtml5
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameImportService(
    private val context: Context,
    private val repository: GameRepository,
) {
    suspend fun importTree(uri: Uri): GameEntry = withContext(Dispatchers.IO) {
        persistReadPermission(uri)
        val source = DocumentFile.fromTreeUri(context, uri)
            ?: throw GameValidationException("无法读取选择的文件夹")

        val detection = GameDirectoryDetector().detect(source, context.contentResolver)
        val folderName = "${detection.title.safeFileName()}-${UUID.randomUUID()}"
        val destination = File(context.filesDir, "games/$folderName")
        val tempDestination = File(context.cacheDir, "import-$folderName")

        tempDestination.deleteRecursively()
        destination.deleteRecursively()
        tempDestination.mkdirs()

        try {
            val html5Optimized = detection.engine.isHtml5()
            val copySource = if (html5Optimized) {
                Html5WebRootLocator.find(source)
                    ?: throw GameValidationException("没有找到真正的 MV/MZ Web 根目录")
            } else {
                source
            }

            copyDocumentTree(
                source = copySource,
                destination = tempDestination,
                html5Optimized = html5Optimized,
            )
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            repository.addCopiedGame(destination, uri)
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            destination.deleteRecursively()
            throw error
        }
    }

    suspend fun addDirectPath(path: String): GameEntry = repository.addDirectPath(path)

    private fun persistReadPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.recoverCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun copyDocumentTree(
        source: DocumentFile,
        destination: File,
        html5Optimized: Boolean = false,
    ) {
        if (!source.isDirectory) {
            throw GameValidationException("请选择游戏文件夹，而不是单个文件")
        }
        destination.mkdirs()
        source.listFiles().forEach { child ->
            val childName = child.name?.replace("/", "_") ?: return@forEach
            if (shouldSkipCopy(childName, html5Optimized)) return@forEach

            val target = File(destination, childName)
            when {
                child.isDirectory -> copyDocumentTree(child, target, html5Optimized)
                child.isFile -> copyDocumentFile(child, target)
            }
        }
    }

    private fun copyDocumentFile(source: DocumentFile, target: File) {
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output, COPY_BUFFER_SIZE)
            }
        } ?: throw GameValidationException("无法读取文件：${source.name}")
    }

    private fun copyDirectory(source: File, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles().orEmpty().forEach { child ->
                copyDirectory(child, File(destination, child.name))
            }
        } else {
            destination.parentFile?.mkdirs()
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            }
        }
    }

    private fun shouldSkipCopy(name: String, html5Optimized: Boolean): Boolean {
        val normalized = name.lowercase()
        if (normalized in commonSkipNames) return true
        return html5Optimized && normalized in html5DesktopRuntimeSkipNames
    }

    private companion object {
        private const val COPY_BUFFER_SIZE = 1024 * 1024

        private val commonSkipNames = setOf(
            ".ds_store",
            "thumbs.db",
            "desktop.ini",
        )

        private val html5DesktopRuntimeSkipNames = setOf(
            ".git",
            "node_modules",
            "game.exe",
            "nw.exe",
            "nw.dll",
            "node.dll",
            "ffmpeg.dll",
            "icudtl.dat",
            "d3dcompiler_47.dll",
            "libegl.dll",
            "libglesv2.dll",
            "notification_helper.exe",
            "resources.pak",
            "natives_blob.bin",
            "snapshot_blob.bin",
            "v8_context_snapshot.bin",
            "locales",
            "swiftshader",
        )
    }
}
