package io.github.mkxpz.rpgplayer.domain

import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameDirectoryDetectorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val detector = GameDirectoryDetector()

    @Test
    fun detectsVxAceGameFromScriptsAndLibrary() {
        val root = temp.newFolder("ace")
        root.resolve("Game.ini").writeText(
            """
            [Game]
            Title=Example Ace
            Library=RGSS301E.dll
            """.trimIndent(),
        )
        root.resolve("Data").mkdirs()
        root.resolve("Data/Scripts.rvdata2").writeText("scripts")

        val detection = detector.detect(root)

        assertEquals("Example Ace", detection.title)
        assertEquals(RpgMakerEngine.VX_ACE, detection.engine)
    }

    @Test
    fun acceptsEncryptedArchiveWithoutScripts() {
        val root = temp.newFolder("encrypted")
        root.resolve("Game.ini").writeText("Title=Encrypted\nLibrary=RGSS202E.dll")
        root.resolve("Game.rgss2a").writeText("archive")

        val detection = detector.detect(root)

        assertEquals(RpgMakerEngine.VX, detection.engine)
        assertTrue(detection.hasEncryptedArchive)
    }

    @Test
    fun detectsMvGameFromWebRoot() {
        val root = temp.newFolder("mv")
        root.resolve("index.html").writeText("<html></html>")
        root.resolve("js").mkdirs()
        root.resolve("js/rpg_core.js").writeText("/* mv */")
        root.resolve("data").mkdirs()
        root.resolve("data/System.json").writeText("""{"gameTitle":"Example MV"}""")

        val detection = detector.detect(root)

        assertEquals("Example MV", detection.title)
        assertEquals(RpgMakerEngine.MV, detection.engine)
    }

    @Test
    fun reportsMv3dPluginCompatibilityWarning() {
        val root = temp.newFolder("mv3d-warning")
        root.resolve("index.html").writeText("<html></html>")
        root.resolve("js").mkdirs()
        root.resolve("js/rpg_core.js").writeText("/* mv */")
        root.resolve("js/plugins").mkdirs()
        root.resolve("js/plugins/MV3D.js").writeText("/* mv3d */")
        root.resolve("data").mkdirs()
        root.resolve("data/System.json").writeText("""{"gameTitle":"Example MV3D"}""")

        val detection = detector.detect(root)

        assertEquals(RpgMakerEngine.MV, detection.engine)
        assertTrue(detection.warnings.any { "3D plugin" in it && "WebGL compatibility mode" in it })
    }

    @Test
    fun detectsMzGameFromWwwFolder() {
        val root = temp.newFolder("mz")
        root.resolve("www").mkdirs()
        root.resolve("www/index.html").writeText("<html></html>")
        root.resolve("www/js").mkdirs()
        root.resolve("www/js/rmmz_core.js").writeText("/* mz */")
        root.resolve("www/data").mkdirs()
        root.resolve("www/data/System.json").writeText("""{"gameTitle":"Example MZ","hasEncryptedImages":true}""")

        val detection = detector.detect(root)

        assertEquals("Example MZ", detection.title)
        assertEquals(RpgMakerEngine.MZ, detection.engine)
        assertTrue(detection.hasEncryptedArchive)
    }

    @Test
    fun rejectsFolderWithoutGameIni() {
        val root = temp.newFolder("not-game")

        assertFailsWith<GameValidationException> {
            detector.detect(root)
        }
    }

    private fun File.resolve(path: String): File = File(this, path)
}
