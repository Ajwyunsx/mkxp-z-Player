package io.github.mkxpz.rpgplayer.domain

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

data class GameDetection(
    val title: String,
    val engine: RpgMakerEngine,
    val hasEncryptedArchive: Boolean,
    val hasWinApiUsage: Boolean = false,
    val warnings: List<String> = emptyList(),
    val packedRgssExecutableName: String? = null,
)

class GameValidationException(message: String) : IllegalArgumentException(message)

class GameDirectoryDetector {
    fun detect(root: File): GameDetection {
        if (!root.isDirectory) {
            throw GameValidationException("不是有效的游戏文件夹")
        }

        if (root.hasEasyRpgMarkers()) {
            return detectEasyRpg(root)
        }

        val gameIni = root.resolve("Game.ini")
        if (gameIni.isFile) {
            return detectRgss(root, gameIni)
        }

        PackedRgssGameExtractor.inspect(root)?.let { packed ->
            return detectPackedRgss(packed, root.name)
        }

        return detectHtml5(root)
    }

    private fun detectRgss(root: File, gameIni: File): GameDetection {
        val dataDir = root.resolve("Data")
        val engine = detectEngine(
            library = parseLibrary(gameIni.readText()),
            scriptsRxdata = dataDir.resolve("Scripts.rxdata").isFile,
            scriptsRvdata = dataDir.resolve("Scripts.rvdata").isFile,
            scriptsRvdata2 = dataDir.resolve("Scripts.rvdata2").isFile,
        )
        val hasScripts = dataDir.resolve("Scripts.rxdata").isFile ||
            dataDir.resolve("Scripts.rvdata").isFile ||
            dataDir.resolve("Scripts.rvdata2").isFile
        val hasArchive = root.resolve("Game.rgssad").isFile ||
            root.resolve("Game.rgss2a").isFile ||
            root.resolve("Game.rgss3a").isFile
        val hasWinApiUsage = WinApiScriptScanner.scan(root)

        if (!hasScripts && !hasArchive) {
            throw GameValidationException("没有找到 Scripts 数据或 RGSS 加密包")
        }

        return GameDetection(
            title = parseTitle(gameIni.readText()).ifBlank { root.name },
            engine = engine,
            hasEncryptedArchive = hasArchive,
            hasWinApiUsage = hasWinApiUsage,
            warnings = buildList {
                if (hasWinApiUsage) add("Detected WinAPI usage; compatibility preload will be enabled when the setting is on")
                if (engine == RpgMakerEngine.UNKNOWN) add("无法从 Game.ini 或 Scripts 文件判断 RGSS 版本，将交给 mkxp-z 自动识别")
                if (hasArchive) add("检测到 RGSS 加密包")
            },
        )
    }

    private fun detectEasyRpg(root: File): GameDetection {
        val iniText = root.findIgnoreCase("RPG_RT.ini")
            ?.takeIf(File::isFile)
            ?.let { runCatching { it.readText() }.getOrNull() }
            .orEmpty()
        return GameDetection(
            title = parseIniValue(iniText, "GameTitle")
                .ifBlank { parseIniValue(iniText, "Title") }
                .ifBlank { root.name },
            engine = RpgMakerEngine.RPG_2000_2003,
            hasEncryptedArchive = false,
            warnings = listOf("RPG Maker 2000/2003 game detected; EasyRPG launch path will be used"),
        )
    }

    private fun detectHtml5(root: File): GameDetection {
        val webRoot = Html5WebRootLocator.find(root)
            ?: throw GameValidationException("没有找到 Game.ini 或 MV/MZ 的 index.html")

        val jsDir = webRoot.resolve("js")
        val systemJson = webRoot.resolve("data/System.json")
        val engine = when {
            jsDir.resolve("rmmz_core.js").isFile -> RpgMakerEngine.MZ
            jsDir.resolve("rpg_core.js").isFile -> RpgMakerEngine.MV
            jsDir.resolve("rmmz_managers.js").isFile -> RpgMakerEngine.MZ
            jsDir.resolve("rpg_managers.js").isFile -> RpgMakerEngine.MV
            else -> throw GameValidationException("没有找到 RPG Maker MV/MZ 的 js 核心文件")
        }

        val system = systemJson.takeIf(File::isFile)
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
        val encrypted = system?.optBoolean("hasEncryptedImages", false) == true ||
            system?.optBoolean("hasEncryptedAudio", false) == true
        val threeDPluginNames = Html5CompatibilityDetector.detectThreeDPluginNames(webRoot)

        return GameDetection(
            title = system?.optString("gameTitle")?.takeIf(String::isNotBlank)
                ?: root.packageTitle()
                ?: root.name,
            engine = engine,
            hasEncryptedArchive = encrypted,
            warnings = buildList {
                add("${engine.displayName} 将使用 Android WebView 运行，不走 mkxp-z RGSS 核心")
                if (encrypted) add("检测到 MV/MZ 加密资源")
                if (threeDPluginNames.isNotEmpty()) {
                    add("Detected 3D plugin (${threeDPluginNames.joinToString()}); WebGL compatibility mode will be enabled automatically")
                }
            },
        )
    }

    fun detect(root: DocumentFile, contentResolver: ContentResolver): GameDetection {
        if (root.hasEasyRpgMarkers()) {
            return detectEasyRpg(root, contentResolver)
        }

        val gameIni = root.findFileIgnoreCase("Game.ini")
        if (gameIni != null) {
            return detectRgss(root, gameIni, contentResolver)
        }

        PackedRgssGameExtractor.inspect(root, contentResolver)?.let { packed ->
            return detectPackedRgss(packed, root.name ?: "Packed RPG Maker Game")
        }

        return detectHtml5(root, contentResolver)
    }

    private fun detectPackedRgss(packed: PackedRgssGame, fallbackTitle: String): GameDetection {
        return GameDetection(
            title = packed.title.ifBlank { fallbackTitle },
            engine = packed.engine,
            hasEncryptedArchive = true,
            warnings = listOf(
                "Detected Enigma-packed ${packed.engine.displayName}; ${packed.executableName} will be unpacked into ${packed.archiveName} before launch",
            ),
            packedRgssExecutableName = packed.executableName,
        )
    }

    private fun detectEasyRpg(root: DocumentFile, contentResolver: ContentResolver): GameDetection {
        val iniText = root.findFileIgnoreCase("RPG_RT.ini")
            ?.let { readDocumentText(it, contentResolver) }
            .orEmpty()
        return GameDetection(
            title = parseIniValue(iniText, "GameTitle")
                .ifBlank { parseIniValue(iniText, "Title") }
                .ifBlank { root.name ?: "RPG Maker 2000/2003 Game" },
            engine = RpgMakerEngine.RPG_2000_2003,
            hasEncryptedArchive = false,
            warnings = listOf("RPG Maker 2000/2003 game detected; EasyRPG launch path will be used"),
        )
    }

    private fun detectRgss(
        root: DocumentFile,
        gameIni: DocumentFile,
        contentResolver: ContentResolver,
    ): GameDetection {
        val dataDir = root.findFileIgnoreCase("Data")
        val scriptsRxdata = dataDir?.findFileIgnoreCase("Scripts.rxdata")?.isFile == true
        val scriptsRvdata = dataDir?.findFileIgnoreCase("Scripts.rvdata")?.isFile == true
        val scriptsRvdata2 = dataDir?.findFileIgnoreCase("Scripts.rvdata2")?.isFile == true
        val hasArchive = root.findFileIgnoreCase("Game.rgssad")?.isFile == true ||
            root.findFileIgnoreCase("Game.rgss2a")?.isFile == true ||
            root.findFileIgnoreCase("Game.rgss3a")?.isFile == true

        if (!scriptsRxdata && !scriptsRvdata && !scriptsRvdata2 && !hasArchive) {
            throw GameValidationException("没有找到 Scripts 数据或 RGSS 加密包")
        }

        val iniText = runCatching {
            contentResolver.openInputStream(gameIni.uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()

        return GameDetection(
            title = parseTitle(iniText).ifBlank { root.name ?: "RPG Maker Game" },
            engine = detectEngine(parseLibrary(iniText), scriptsRxdata, scriptsRvdata, scriptsRvdata2),
            hasEncryptedArchive = hasArchive,
            warnings = buildList {
                if (hasArchive) add("检测到 RGSS 加密包")
            },
        )
    }

    private fun detectHtml5(root: DocumentFile, contentResolver: ContentResolver): GameDetection {
        val webRoot = Html5WebRootLocator.find(root)
            ?: throw GameValidationException("没有找到 Game.ini 或 MV/MZ 的 index.html")

        val jsDir = webRoot.findFileIgnoreCase("js")
        val engine = when {
            jsDir?.findFileIgnoreCase("rmmz_core.js")?.isFile == true -> RpgMakerEngine.MZ
            jsDir?.findFileIgnoreCase("rpg_core.js")?.isFile == true -> RpgMakerEngine.MV
            jsDir?.findFileIgnoreCase("rmmz_managers.js")?.isFile == true -> RpgMakerEngine.MZ
            jsDir?.findFileIgnoreCase("rpg_managers.js")?.isFile == true -> RpgMakerEngine.MV
            else -> throw GameValidationException("没有找到 RPG Maker MV/MZ 的 js 核心文件")
        }

        val systemJson = webRoot.findFileIgnoreCase("data")
            ?.findFileIgnoreCase("System.json")
        val system = systemJson
            ?.let { readDocumentText(it, contentResolver) }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
        val encrypted = system?.optBoolean("hasEncryptedImages", false) == true ||
            system?.optBoolean("hasEncryptedAudio", false) == true
        val threeDPluginNames = Html5CompatibilityDetector.detectThreeDPluginNames(webRoot, contentResolver)

        return GameDetection(
            title = system?.optString("gameTitle")?.takeIf(String::isNotBlank)
                ?: root.packageTitle(contentResolver)
                ?: root.name
                ?: "RPG Maker Game",
            engine = engine,
            hasEncryptedArchive = encrypted,
            warnings = buildList {
                add("${engine.displayName} 将使用 Android WebView 运行，不走 mkxp-z RGSS 核心")
                if (encrypted) add("检测到 MV/MZ 加密资源")
                if (threeDPluginNames.isNotEmpty()) {
                    add("Detected 3D plugin (${threeDPluginNames.joinToString()}); WebGL compatibility mode will be enabled automatically")
                }
            },
        )
    }

    private fun detectEngine(
        library: String,
        scriptsRxdata: Boolean,
        scriptsRvdata: Boolean,
        scriptsRvdata2: Boolean,
    ): RpgMakerEngine {
        val normalized = library.uppercase()
        return when {
            "RGSS3" in normalized || scriptsRvdata2 -> RpgMakerEngine.VX_ACE
            "RGSS2" in normalized || scriptsRvdata -> RpgMakerEngine.VX
            "RGSS1" in normalized || scriptsRxdata -> RpgMakerEngine.XP
            else -> RpgMakerEngine.UNKNOWN
        }
    }

    private fun parseTitle(iniText: String): String =
        parseIniValue(iniText, "Title")

    private fun parseLibrary(iniText: String): String =
        parseIniValue(iniText, "Library")

    private fun parseIniValue(iniText: String, key: String): String {
        val prefix = "$key="
        return iniText.lineSequence()
            .firstOrNull { it.trimStart().startsWith(prefix, ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }
}

private fun DocumentFile.findFileIgnoreCase(name: String): DocumentFile? =
    listFiles().firstOrNull { it.name.equals(name, ignoreCase = true) }

private fun File.findIgnoreCase(name: String): File? =
    listFiles()?.firstOrNull { it.name.equals(name, ignoreCase = true) }

private fun File.hasEasyRpgMarkers(): Boolean =
    findIgnoreCase("RPG_RT.ldb")?.isFile == true &&
        findIgnoreCase("RPG_RT.lmt")?.isFile == true

private fun DocumentFile.hasEasyRpgMarkers(): Boolean =
    findFileIgnoreCase("RPG_RT.ldb")?.isFile == true &&
        findFileIgnoreCase("RPG_RT.lmt")?.isFile == true

private fun File.webRoot(): File? {
    if (resolve("index.html").isFile) return this
    val www = findIgnoreCase("www")
    if (www?.resolve("index.html")?.isFile == true) return www
    return null
}

private fun DocumentFile.webRoot(): DocumentFile? {
    if (findFileIgnoreCase("index.html")?.isFile == true) return this
    val www = findFileIgnoreCase("www")
    if (www?.findFileIgnoreCase("index.html")?.isFile == true) return www
    return null
}

private fun File.packageTitle(): String? {
    val packageJson = resolve("package.json").takeIf(File::isFile)
        ?: findIgnoreCase("www")?.resolve("package.json")?.takeIf(File::isFile)
    return packageJson?.let {
        runCatching {
            val json = JSONObject(it.readText())
            json.optString("name").takeIf(String::isNotBlank)
                ?: json.optJSONObject("window")?.optString("title")?.takeIf(String::isNotBlank)
        }.getOrNull()
    }
}

private fun DocumentFile.packageTitle(contentResolver: ContentResolver): String? {
    val packageJson = findFileIgnoreCase("package.json")
        ?: findFileIgnoreCase("www")?.findFileIgnoreCase("package.json")
    return packageJson?.let {
        runCatching {
            val json = JSONObject(readDocumentText(it, contentResolver))
            json.optString("name").takeIf(String::isNotBlank)
                ?: json.optJSONObject("window")?.optString("title")?.takeIf(String::isNotBlank)
        }.getOrNull()
    }
}

private fun readDocumentText(file: DocumentFile, contentResolver: ContentResolver): String =
    contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()

private object WinApiScriptScanner {
    private const val MAX_FILE_BYTES = 64L * 1024L * 1024L
    private const val MAX_INFLATED_BYTES = 8 * 1024 * 1024

    private val candidateExtensions = setOf("rb", "txt", "ini", "rxdata", "rvdata", "rvdata2")
    private val skipDirectories = setOf(".git", "audio", "graphics", "movies", "node_modules", "www")
    private val patterns = listOf(
        "Win32API",
        "WinAPI",
        "DL.dlopen",
        "user32.dll",
        "kernel32.dll",
        "gdi32.dll",
        "shell32.dll",
        "FindWindow",
        "GetAsyncKeyState",
        "GetPrivateProfileString",
        "WritePrivateProfileString",
    )

    fun scan(root: File): Boolean = runCatching {
        root.walkTopDown()
            .onEnter { dir -> dir.name.lowercase() !in skipDirectories }
            .filter { file -> file.isFile && isCandidate(file) && file.length() in 1..MAX_FILE_BYTES }
            .any(::scanFile)
    }.getOrDefault(false)

    private fun isCandidate(file: File): Boolean {
        return file.name.equals("Game.ini", ignoreCase = true) ||
            file.extension.lowercase() in candidateExtensions
    }

    private fun scanFile(file: File): Boolean {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        return containsPattern(bytes) || scanDeflatedStreams(bytes)
    }

    private fun scanDeflatedStreams(bytes: ByteArray): Boolean {
        var index = 0
        while (index < bytes.size - 2) {
            if (looksLikeZlibHeader(bytes, index) && inflateContainsPattern(bytes, index)) {
                return true
            }
            index += 1
        }
        return false
    }

    private fun inflateContainsPattern(bytes: ByteArray, offset: Int): Boolean {
        val inflater = Inflater()
        return try {
            inflater.setInput(bytes, offset, bytes.size - offset)
            val buffer = ByteArray(8192)
            val output = ByteArrayOutputStream()
            var total = 0
            while (!inflater.finished() && !inflater.needsInput() && total < MAX_INFLATED_BYTES) {
                val read = inflater.inflate(buffer)
                if (read <= 0) {
                    break
                }
                output.write(buffer, 0, read)
                total += read
            }
            total > 0 && containsPattern(output.toByteArray())
        } catch (_: Throwable) {
            false
        } finally {
            inflater.end()
        }
    }

    private fun looksLikeZlibHeader(bytes: ByteArray, offset: Int): Boolean {
        val cmf = bytes[offset].toInt() and 0xff
        val flg = bytes[offset + 1].toInt() and 0xff
        return cmf and 0x0f == 8 && ((cmf shl 8) + flg) % 31 == 0
    }

    private fun containsPattern(bytes: ByteArray): Boolean {
        val text = bytes.toString(Charsets.ISO_8859_1)
        return patterns.any { pattern -> text.contains(pattern, ignoreCase = true) }
    }
}
