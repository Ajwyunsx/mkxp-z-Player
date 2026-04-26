package io.github.mkxpz.rpgplayer.domain

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class Html5WebRootLocatorTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun findsWwwFolderWhenWindowsExportRootIsSelected() {
        val root = temp.newFolder("windows-export")
        root.resolve("Game.exe").writeText("desktop runtime")
        root.resolve("www").mkdirs()
        root.resolve("www/index.html").writeText("<html></html>")
        root.resolve("www/js").mkdirs()
        root.resolve("www/js/rpg_core.js").writeText("/* mv */")

        val webRoot = Html5WebRootLocator.find(root)

        assertEquals(root.resolve("www").canonicalFile, webRoot?.canonicalFile)
    }

    @Test
    fun rejectsPlainIndexWithoutRpgMakerCore() {
        val root = temp.newFolder("plain-web")
        root.resolve("index.html").writeText("<html></html>")

        assertNull(Html5WebRootLocator.find(root))
    }
}
