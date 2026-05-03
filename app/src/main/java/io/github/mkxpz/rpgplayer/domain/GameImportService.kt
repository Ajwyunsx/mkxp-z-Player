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
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameImportService(
    private val context: Context,
    private val repository: GameRepository,
) {
    suspend fun installBundledGameIfPresent(): GameEntry? = withContext(Dispatchers.IO) {
        val manifest = readBundledGameManifest() ?: return@withContext null
        val originalUri = bundledOriginalUri(manifest)
        repository.getByOriginalUri(originalUri)?.takeIf { File(it.installedPath).isDirectory }?.let {
            return@withContext it
        }

        val title = manifest.optString("title").takeIf(String::isNotBlank) ?: "Bundled RPG Maker Game"
        val token = "${manifest.optString("sourceGameId")}-${manifest.optLong("createdAt")}".safeFileName()
            .ifBlank { "bundled" }
        val destination = File(context.filesDir, "games/${title.safeFileName()}-$token")
        val tempDestination = File(context.cacheDir, "bundled-game-$token")

        tempDestination.deleteRecursively()
        destination.deleteRecursively()
        tempDestination.mkdirs()

        try {
            copyAssetTree(BUNDLED_GAME_FILES_ASSET, tempDestination)
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            repository.addCopiedGame(destination, Uri.parse(originalUri))
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            destination.deleteRecursively()
            throw error
        }
    }

    suspend fun exportStandaloneApk(entry: GameEntry, options: StandaloneApkOptions): File =
        StandaloneApkExporter(context).export(entry, options)

    fun installStandaloneApk(apk: File) {
        StandaloneApkExporter(context).install(apk)
    }

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
            if (detection.packedRgssExecutableName != null) {
                PackedRgssGameExtractor.extract(source, context.contentResolver, tempDestination)
                copyPackedSaveFolders(source, tempDestination)
            } else {
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
            }
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

    suspend fun addDirectPath(path: String): GameEntry = withContext(Dispatchers.IO) {
        val root = File(path.trim())
        val packed = PackedRgssGameExtractor.inspect(root)
        if (packed == null) {
            repository.addDirectPath(path)
        } else {
            importPackedLocalDirectory(root, packed)
        }
    }

    suspend fun importVxRtpTree(uri: Uri): String = importRtpTree(uri, "rtp/vx")

    suspend fun importVxAceRtpTree(uri: Uri): String = importRtpTree(uri, "rtp/vxace")

    private suspend fun importRtpTree(uri: Uri, destinationPath: String): String = withContext(Dispatchers.IO) {
        persistReadPermission(uri)
        val source = DocumentFile.fromTreeUri(context, uri)
            ?: throw GameValidationException("无法读取选择的 VX RTP 文件夹")
        val rtpRoot = findVxRtpRoot(source)
            ?: throw GameValidationException("没有找到 VX RTP 目录，请选择包含 Audio 和 Graphics 的 RPGVX 文件夹")

        val destination = File(context.filesDir, destinationPath)
        val tempDestination = File(context.cacheDir, "${destinationPath.replace('/', '-')}-import")

        tempDestination.deleteRecursively()
        tempDestination.mkdirs()

        try {
            copyDocumentTree(
                source = rtpRoot,
                destination = tempDestination,
                html5Optimized = false,
            )
            destination.deleteRecursively()
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            destination.absolutePath
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            destination.deleteRecursively()
            throw error
        }
    }

    suspend fun downloadVxRtp(): String = withContext(Dispatchers.IO) {
        val cacheRoot = File(context.cacheDir, "rtp-vx-download")
        val zipFile = File(cacheRoot, "vx_rtp102e.zip")
        val extractRoot = File(cacheRoot, "extracted")
        val tempDestination = File(context.cacheDir, "rtp-vx-install")
        val destination = File(context.filesDir, "rtp/vx")

        cacheRoot.deleteRecursively()
        tempDestination.deleteRecursively()
        cacheRoot.mkdirs()
        extractRoot.mkdirs()
        tempDestination.mkdirs()

        try {
            downloadToFile(VX_RTP_SOURCES, zipFile, VX_RTP_SHA256)
            extractZip(zipFile, extractRoot)
            val rtpRoot = findVxRtpRoot(extractRoot)
                ?: extractVxRtpInstaller(extractRoot, File(cacheRoot, "inno-extracted"), "VX RTP")
            copyDirectory(rtpRoot, tempDestination)
            destination.deleteRecursively()
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            destination.absolutePath
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            throw error
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    suspend fun downloadVxAceRtp(): String = withContext(Dispatchers.IO) {
        val cacheRoot = File(context.cacheDir, "rtp-vxace-download")
        val zipFile = File(cacheRoot, "RPGVXAce_RTP.zip")
        val extractRoot = File(cacheRoot, "extracted")
        val tempDestination = File(context.cacheDir, "rtp-vxace-install")
        val destination = File(context.filesDir, "rtp/vxace")

        cacheRoot.deleteRecursively()
        tempDestination.deleteRecursively()
        cacheRoot.mkdirs()
        extractRoot.mkdirs()
        tempDestination.mkdirs()

        try {
            downloadToFile(VX_ACE_RTP_SOURCES, zipFile, null)
            extractZip(zipFile, extractRoot)
            val rtpRoot = findVxRtpRoot(extractRoot)
                ?: extractVxRtpInstaller(extractRoot, File(cacheRoot, "inno-extracted"), "VX Ace RTP")
            copyDirectory(rtpRoot, tempDestination)
            destination.deleteRecursively()
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            destination.absolutePath
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            throw error
        } finally {
            cacheRoot.deleteRecursively()
        }
    }

    private suspend fun extractVxRtpInstaller(extractRoot: File, destination: File, label: String): File {
        val setupExe = findFileRecursive(extractRoot, "Setup.exe")
            ?: throw vxRtpArchiveException(extractRoot, label)
        extractInnoInstallerWithSplitFallback(setupExe, destination, label)
        return findVxRtpRoot(destination)
            ?: throw GameValidationException("$label 安装器解包后没有找到 Audio 和 Graphics 文件夹")
    }

    private suspend fun extractInnoInstallerWithSplitFallback(setupExe: File, destination: File, label: String) {
        val extractor = InnoSetupExtractor(context, label)
        val setupError = runCatching {
            extractor.extract(setupExe, destination)
        }.exceptionOrNull()
        if (setupError == null || findVxRtpRoot(destination) != null) {
            return
        }

        val splitDataFiles = setupExe.parentFile
            ?.listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.name.matches(Regex("(?i)setup-\\d+\\.bin")) }
            .sortedBy { file -> file.name.lowercase(Locale.US) }

        var splitError: Throwable? = null
        for (dataFile in splitDataFiles) {
            destination.deleteRecursively()
            destination.mkdirs()
            splitError = runCatching {
                extractor.extract(dataFile, destination)
            }.exceptionOrNull()
            if (splitError == null || findVxRtpRoot(destination) != null) {
                return
            }
        }

        throw splitError ?: setupError
    }

    private fun vxRtpArchiveException(extractRoot: File, label: String): GameValidationException {
        val setupExe = findFileRecursive(extractRoot, "Setup.exe")
        return if (setupExe != null) {
            GameValidationException(
                "已找到 $label 安装器 Setup.exe，但自动解包失败，请重试自动安装。",
            )
        } else {
            GameValidationException("下载到的 $label 压缩包没有包含 Audio 和 Graphics 文件夹")
        }
    }

    private fun findFileRecursive(root: File, name: String): File? {
        return root.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals(name, ignoreCase = true) }
    }

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

    private fun findVxRtpRoot(source: DocumentFile, depth: Int = 0): DocumentFile? {
        if (!source.isDirectory) return null
        val children = source.listFiles()
        val names = children.mapNotNull { it.name?.lowercase() }.toSet()
        if ("audio" in names && "graphics" in names) {
            return source
        }
        if (depth >= 3) {
            return null
        }
        children
            .filter { it.isDirectory }
            .forEach { child ->
                findVxRtpRoot(child, depth + 1)?.let { return it }
        }
        return null
    }

    private fun findVxRtpRoot(source: File, depth: Int = 0): File? {
        if (!source.isDirectory) return null
        val children = source.listFiles().orEmpty()
        val names = children.map { it.name.lowercase() }.toSet()
        if ("audio" in names && "graphics" in names) {
            return source
        }
        if (depth >= 5) {
            return null
        }
        children
            .filter { it.isDirectory }
            .forEach { child ->
                findVxRtpRoot(child, depth + 1)?.let { return it }
            }
        return null
    }

    private fun downloadToFile(sources: List<String>, target: File, expectedSha256: String?) {
        val errors = mutableListOf<String>()
        val partial = File(target.parentFile, "${target.name}.part")
        target.parentFile?.mkdirs()
        target.delete()
        partial.delete()

        for (source in sources) {
            try {
                downloadSingleSource(source, partial)
                expectedSha256?.let { verifySha256(partial, it) }
                if (!partial.renameTo(target)) {
                    partial.copyTo(target, overwrite = true)
                    partial.delete()
                }
                return
            } catch (error: Throwable) {
                partial.delete()
                errors += "${source.removePrefix("https://").removePrefix("http://")}: ${friendlyDownloadError(error)}"
            }
        }

        throw GameValidationException(
            "VX RTP 自动下载失败：${errors.joinToString("；")}。请检查网络后重试，或手动导入已解压的 RPGVX RTP 文件夹。",
        )
    }

    private fun downloadSingleSource(url: String, target: File) {
        target.parentFile?.mkdirs()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw GameValidationException("VX RTP download failed: HTTP $responseCode")
            }
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifySha256(file: File, expectedSha256: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { byte ->
            String.format(Locale.US, "%02x", byte.toInt() and 0xff)
        }
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            throw GameValidationException("VX RTP download checksum mismatch")
        }
    }

    private fun friendlyDownloadError(error: Throwable): String {
        val certError = generateSequence(error) { it.cause }
            .any { it is SSLHandshakeException || it.javaClass.name.contains("CertPathValidatorException") }
        if (certError) {
            return "证书校验失败，已尝试备用下载源"
        }
        return error.message ?: error::class.java.simpleName
    }

    private fun extractZip(zipFile: File, destination: File) {
        destination.mkdirs()
        val canonicalDestination = destination.canonicalFile
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(destination, entry.name).canonicalFile
                if (!target.path.startsWith(canonicalDestination.path + File.separator)) {
                    throw GameValidationException("Unsafe path in VX RTP archive: ${entry.name}")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        zip.copyTo(output, COPY_BUFFER_SIZE)
                    }
                }
                zip.closeEntry()
            }
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

    private suspend fun importPackedLocalDirectory(root: File, packed: PackedRgssGame): GameEntry {
        val folderName = "${packed.title.safeFileName()}-${UUID.randomUUID()}"
        val destination = File(context.filesDir, "games/$folderName")
        val tempDestination = File(context.cacheDir, "import-$folderName")

        tempDestination.deleteRecursively()
        destination.deleteRecursively()
        tempDestination.mkdirs()

        try {
            PackedRgssGameExtractor.extract(root, tempDestination)
            if (!tempDestination.renameTo(destination)) {
                copyDirectory(tempDestination, destination)
                tempDestination.deleteRecursively()
            }
            return repository.addCopiedGame(destination, Uri.fromFile(root))
        } catch (error: Throwable) {
            tempDestination.deleteRecursively()
            destination.deleteRecursively()
            throw error
        }
    }

    private fun copyPackedSaveFolders(source: DocumentFile, destination: File) {
        source.listFiles()
            .filter { it.isDirectory && it.name.equals("Saves", ignoreCase = true) }
            .forEach { saves ->
                copyDocumentTree(
                    source = saves,
                    destination = File(destination, saves.name ?: "Saves"),
                    html5Optimized = false,
                )
            }
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

    private fun readBundledGameManifest(): org.json.JSONObject? {
        return try {
            context.assets.open(BUNDLED_GAME_MANIFEST_ASSET).bufferedReader().use { reader ->
                org.json.JSONObject(reader.readText())
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun bundledOriginalUri(manifest: org.json.JSONObject): String {
        val sourceId = manifest.optString("sourceGameId").ifBlank { "game" }
        val createdAt = manifest.optLong("createdAt", 0L)
        return "asset://bundled_game/$sourceId/$createdAt"
    }

    private fun copyAssetTree(assetPath: String, destination: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            }
            return
        }

        destination.mkdirs()
        children.forEach { child ->
            copyAssetTree("$assetPath/$child", File(destination, child))
        }
    }

    private fun shouldSkipCopy(name: String, html5Optimized: Boolean): Boolean {
        val normalized = name.lowercase()
        if (normalized in commonSkipNames) return true
        return html5Optimized && normalized in html5DesktopRuntimeSkipNames
    }

    private companion object {
        private val VX_RTP_SOURCES = listOf(
            "https://dl.komodo.jp/rpgmakerweb/run-time-packages/vx_rtp102e.zip",
            "http://dl.komodo.jp/rpgmakerweb/run-time-packages/vx_rtp102e.zip",
        )
        private val VX_ACE_RTP_SOURCES = listOf(
            "https://dl.komodo.jp/rpgmakerweb/run-time-packages/RPGVXAce_RTP.zip",
            "http://dl.komodo.jp/rpgmakerweb/run-time-packages/RPGVXAce_RTP.zip",
        )
        private const val VX_RTP_SHA256 = "8c82c02c876391d9585934454a629748d71b421c4957ada1dff8dc4b013ce403"
        private const val COPY_BUFFER_SIZE = 1024 * 1024
        private const val BUNDLED_GAME_MANIFEST_ASSET = "bundled_game/manifest.json"
        private const val BUNDLED_GAME_FILES_ASSET = "bundled_game/files"

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
