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
        assertEquals(60, json.getInt("fixedFramerate"))
        assertTrue(json.getBoolean("smoothScaling"))
        assertFalse(json.getBoolean("fixedAspectRatio"))
        assertEquals("/storage/emulated/0/GMGSx.sf2", json.getString("midiSoundFont"))
    }
}
