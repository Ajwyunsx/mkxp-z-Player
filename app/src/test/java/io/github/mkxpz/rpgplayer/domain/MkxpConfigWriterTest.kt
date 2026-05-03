package io.github.mkxpz.rpgplayer.domain

import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import io.github.mkxpz.rpgplayer.data.StoredImportMode
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MkxpConfigWriterTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun writesMkxpConfigForGameAndSettings() {
        val output = temp.newFile("mkxp.json")
        val entry = GameEntry(
            id = "game-1",
            title = "Example",
            installedPath = "/storage/emulated/0/Games/Example",
            originalUri = null,
            importMode = StoredImportMode.DIRECT_EXTERNAL_PATH,
            engine = RpgMakerEngine.VX_ACE,
            configPath = output.absolutePath,
            hasEncryptedArchive = false,
            addedAt = 1L,
        )
        val settings = LauncherSettings(
            fixedFramerate = 60,
            smoothScaling = true,
            keepAspectRatio = false,
            soundFontPath = "/storage/emulated/0/GMGSx.sf2",
            debugLaunch = true,
        )

        MkxpConfigWriter().writeConfig(entry, settings, output)

        val json = JSONObject(output.readText())
        assertEquals("/storage/emulated/0/Games/Example", json.getString("gameFolder"))
        assertEquals(3, json.getInt("rgssVersion"))
        assertTrue(json.getBoolean("useScriptNames"))
        assertEquals(60, json.getInt("fixedFramerate"))
        assertTrue(json.getBoolean("smoothScaling"))
        assertFalse(json.getBoolean("fixedAspectRatio"))
        assertEquals("/storage/emulated/0/GMGSx.sf2", json.getString("midiSoundFont"))
    }

    @Test
    fun writesWinApiCompatibilityPreloadForRgssWhenEnabled() {
        val output = temp.newFile("mkxp-winapi.json")
        val entry = GameEntry(
            id = "game-winapi",
            title = "WinAPI Example",
            installedPath = "/storage/emulated/0/Games/WinAPI",
            originalUri = null,
            importMode = StoredImportMode.DIRECT_EXTERNAL_PATH,
            engine = RpgMakerEngine.VX,
            configPath = output.absolutePath,
            hasEncryptedArchive = false,
            hasWinApiUsage = true,
            addedAt = 1L,
        )

        MkxpConfigWriter().writeConfig(entry, LauncherSettings(), output)

        val json = JSONObject(output.readText())
        val preloadScripts = json.getJSONArray("preloadScript")
        val scripts = (0 until preloadScripts.length())
            .map { java.io.File(preloadScripts.getString(it)) }
        assertTrue(scripts.any { it.isFile && it.readText().contains("WinAPI = Win32API") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("RUBY_CLASSIC_COMPATIBILITY") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("NativeWin32API") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("call_windows_api") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("Android.display_metrics") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("remember_android_window_rect") })
        assertFalse(scripts.any { it.isFile && it.readText().contains("Graphics.resize_screen") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("ANDROID_STORAGE_COMPATIBILITY") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("tktk_bitmap") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("safe_create_placeholder_png") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("kernel32") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("rtlmovememory") })
        assertTrue(scripts.any { it.isFile && it.readText().contains("copyfilea") })
        assertFalse(scripts.any { it.isFile && it.readText().contains("location.include?") })
    }

    @Test
    fun usesGameBundledSoundFontWhenSettingIsBlank() {
        val gameRoot = temp.newFolder("soundfont-game")
        gameRoot.resolve("Audio").mkdirs()
        val soundFont = gameRoot.resolve("Audio/game.sf2")
        soundFont.writeText("sf2")
        val output = temp.newFile("mkxp-soundfont.json")
        val entry = GameEntry(
            id = "game-soundfont",
            title = "SoundFont Example",
            installedPath = gameRoot.absolutePath,
            originalUri = null,
            importMode = StoredImportMode.COPIED_TO_LIBRARY,
            engine = RpgMakerEngine.VX,
            configPath = output.absolutePath,
            hasEncryptedArchive = true,
            addedAt = 1L,
        )

        MkxpConfigWriter().writeConfig(entry, LauncherSettings(soundFontPath = ""), output)

        val json = JSONObject(output.readText())
        assertEquals(soundFont.absolutePath, json.getString("midiSoundFont"))
    }

    @Test
    fun writesVxAceRtpPathForEncryptedAceGames() {
        val output = temp.newFile("mkxp-vxace-rtp.json")
        val rtp = temp.newFolder("rtp-vxace")
        rtp.resolve("Audio").mkdirs()
        rtp.resolve("Graphics").mkdirs()
        val entry = GameEntry(
            id = "game-vxace-rtp",
            title = "Packed Ace",
            installedPath = "/storage/emulated/0/Games/PackedAce",
            originalUri = null,
            importMode = StoredImportMode.COPIED_TO_LIBRARY,
            engine = RpgMakerEngine.VX_ACE,
            configPath = output.absolutePath,
            hasEncryptedArchive = true,
            addedAt = 1L,
        )

        MkxpConfigWriter().writeConfig(entry, LauncherSettings(vxAceRtpPath = rtp.absolutePath), output)

        val json = JSONObject(output.readText())
        assertEquals(rtp.absolutePath, json.getJSONArray("RTP").getString(0))
    }

    @Test
    fun fallsBackToVxRtpForAceWhenAceRtpIsBlank() {
        val output = temp.newFile("mkxp-vxace-vx-rtp-fallback.json")
        val rtp = temp.newFolder("rtp-vx")
        rtp.resolve("Audio").mkdirs()
        rtp.resolve("Graphics").mkdirs()
        val entry = GameEntry(
            id = "game-vxace-vx-rtp",
            title = "Packed Ace",
            installedPath = "/storage/emulated/0/Games/PackedAce",
            originalUri = null,
            importMode = StoredImportMode.COPIED_TO_LIBRARY,
            engine = RpgMakerEngine.VX_ACE,
            configPath = output.absolutePath,
            hasEncryptedArchive = true,
            addedAt = 1L,
        )

        MkxpConfigWriter().writeConfig(entry, LauncherSettings(vxRtpPath = rtp.absolutePath), output)

        val json = JSONObject(output.readText())
        assertEquals(rtp.absolutePath, json.getJSONArray("RTP").getString(0))
    }

    @Test
    fun discoversAppPrivateVxAceRtpWhenSettingIsBlank() {
        val filesDir = temp.newFolder("files")
        val outputDir = filesDir.resolve("configs/game-auto-rtp").apply { mkdirs() }
        val output = outputDir.resolve("mkxp.json")
        val rtp = filesDir.resolve("rtp/vxace").apply {
            resolve("Audio").mkdirs()
            resolve("Graphics/System").mkdirs()
        }
        val entry = GameEntry(
            id = "game-auto-rtp",
            title = "Packed Ace",
            installedPath = "/storage/emulated/0/Games/PackedAce",
            originalUri = null,
            importMode = StoredImportMode.COPIED_TO_LIBRARY,
            engine = RpgMakerEngine.VX_ACE,
            configPath = output.absolutePath,
            hasEncryptedArchive = true,
            addedAt = 1L,
        )

        MkxpConfigWriter().writeConfig(entry, LauncherSettings(), output)

        val json = JSONObject(output.readText())
        assertEquals(rtp.absolutePath, json.getJSONArray("RTP").getString(0))
    }
}
