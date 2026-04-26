package io.github.mkxpz.rpgplayer.domain

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import org.json.JSONObject
import java.io.File

data class GameDetection(
    val title: String,
    val engine: RpgMakerEngine,
    val hasEncryptedArchive: Boolean,
    val warnings: List<String> = emptyList(),
)

class GameValidationException(message: String) : IllegalArgumentException(message)

class GameDirectoryDetector {
    fun detect(root: File): GameDetection {
        if (!root.isDirectory) {
            throw GameValidationException("不是有效的游戏文件夹")
        }

        val gameIni = root.resolve("Game.ini")
        if (gameIni.isFile) {
            return detectRgss(root, gameIni)
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

        if (!hasScripts && !hasArchive) {
            throw GameValidationException("没有找到 Scripts 数据或 RGSS 加密包")
        }

        return GameDetection(
            title = parseTitle(gameIni.readText()).ifBlank { root.name },
            engine = engine,
            hasEncryptedArchive = hasArchive,
            warnings = buildList {
                if (engine == RpgMakerEngine.UNKNOWN) add("无法从 Game.ini 或 Scripts 文件判断 RGSS 版本，将交给 mkxp-z 自动识别")
                if (hasArchive) add("检测到 RGSS 加密包")
            },
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
        val gameIni = root.findFileIgnoreCase("Game.ini")
        if (gameIni != null) {
            return detectRgss(root, gameIni, contentResolver)
        }

        return detectHtml5(root, contentResolver)
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
