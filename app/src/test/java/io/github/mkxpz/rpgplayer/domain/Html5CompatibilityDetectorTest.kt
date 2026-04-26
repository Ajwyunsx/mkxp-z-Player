package io.github.mkxpz.rpgplayer.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class Html5CompatibilityDetectorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val detector = Html5CompatibilityDetector()

    @Test
    fun detectsMv3dPluginFile() {
        val root = temp.newFolder("mv3d")
        root.resolve("index.html").writeText("<html></html>")
        root.resolve("js").mkdirs()
        root.resolve("js/rpg_core.js").writeText("/* mv */")
        root.resolve("js/plugins").mkdirs()
        root.resolve("js/plugins/MV3D.js").writeText("/* mv3d */")

        val compatibility = detector.detect(root)

        assertTrue(compatibility.hasThreeDPlugins)
        assertEquals(listOf("MV3D"), compatibility.threeDPluginNames)
    }

    @Test
    fun detectsMode7FromPluginsJsInWwwFolder() {
        val root = temp.newFolder("mode7")
        root.resolve("www").mkdirs()
        root.resolve("www/index.html").writeText("<html></html>")
        root.resolve("www/js").mkdirs()
        root.resolve("www/js/rpg_core.js").writeText("/* mv */")
        root.resolve("www/js/plugins.js").writeText(
            """var ${'$'}plugins = [{"name":"UltraMode7","status":true}];""",
        )

        val compatibility = detector.detect(root)

        assertTrue(compatibility.hasThreeDPlugins)
        assertEquals(listOf("UltraMode7"), compatibility.threeDPluginNames)
    }
}
