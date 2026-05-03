package io.github.mkxpz.rpgplayer.domain

import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.DeflaterOutputStream
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
    fun detectsRpgMaker2000And2003Game() {
        val root = temp.newFolder("2k3")
        root.resolve("RPG_RT.ldb").writeText("database")
        root.resolve("RPG_RT.lmt").writeText("maptree")
        root.resolve("RPG_RT.ini").writeText("GameTitle=Example 2K3")

        val detection = detector.detect(root)

        assertEquals("Example 2K3", detection.title)
        assertEquals(RpgMakerEngine.RPG_2000_2003, detection.engine)
    }

    @Test
    fun detectsWinApiUsageInsideCompressedRgssScripts() {
        val root = temp.newFolder("winapi")
        root.resolve("Game.ini").writeText("Title=WinAPI\nLibrary=RGSS301E.dll")
        root.resolve("Data").mkdirs()
        root.resolve("Data/Scripts.rvdata2").writeBytes(
            zlib("Win32API.new('user32.dll', 'GetAsyncKeyState', 'i', 'i')"),
        )

        val detection = detector.detect(root)

        assertEquals(RpgMakerEngine.VX_ACE, detection.engine)
        assertTrue(detection.hasWinApiUsage)
    }

    @Test
    fun detectsAndExtractsPackedVxAceExe() {
        val root = temp.newFolder("packed")
        val archive = byteArrayOf(
            'R'.code.toByte(),
            'G'.code.toByte(),
            'S'.code.toByte(),
            'S'.code.toByte(),
            'A'.code.toByte(),
            'D'.code.toByte(),
            0,
            3,
            1,
            2,
            3,
            4,
        )
        root.resolve("Game.exe").writeBytes(fakePackedExe(archive))
        root.resolve("Audio/BGM").mkdirs()
        root.resolve("Audio/BGM/theme.ogg").writeText("music")
        root.resolve("Fonts").mkdirs()
        root.resolve("Fonts/game.ttf").writeText("font")

        val detection = detector.detect(root)

        assertEquals("Packed Ace", detection.title)
        assertEquals(RpgMakerEngine.VX_ACE, detection.engine)
        assertTrue(detection.hasEncryptedArchive)
        assertEquals("Game.exe", detection.packedRgssExecutableName)

        val output = temp.newFolder("packed-output")
        val packed = PackedRgssGameExtractor.extract(root, output)

        assertEquals("Game.rgss3a", packed.archiveName)
        assertTrue(output.resolve("Game.ini").isFile)
        assertEquals(archive.toList(), output.resolve("Game.rgss3a").readBytes().toList())
        assertEquals("music", output.resolve("Audio/BGM/theme.ogg").readText())
        assertEquals("font", output.resolve("Fonts/game.ttf").readText())
    }

    @Test
    fun materializesPackedSystemWindowSkinWithCanonicalCase() {
        val root = temp.newFolder("packed-window")
        val windowSkin = "packed-window-skin".toByteArray()
        val archive = rgss3Archive(
            mapOf(
                "Graphics/System/window.png" to windowSkin,
                "Audio/BGM/theme.ogg" to "music".toByteArray(),
            ),
        )
        root.resolve("Game.exe").writeBytes(fakePackedExe(archive))

        val output = temp.newFolder("packed-window-output")
        PackedRgssGameExtractor.extract(root, output)

        val materialized = output.resolve("Graphics/System/Window.png")
        assertEquals(windowSkin.toList(), materialized.readBytes().toList())

        materialized.delete()
        assertEquals(1, PackedRgssGameExtractor.materializeCriticalAssets(output))
        assertEquals(windowSkin.toList(), materialized.readBytes().toList())
    }

    @Test
    fun extractsLegacyEnigmaVirtualAudioResources() {
        val root = temp.newFolder("packed-enigma-audio")
        val archive = byteArrayOf(
            'R'.code.toByte(),
            'G'.code.toByte(),
            'S'.code.toByte(),
            'S'.code.toByte(),
            'A'.code.toByte(),
            'D'.code.toByte(),
            0,
            3,
            1,
            2,
            3,
            4,
        )
        root.resolve("Game.exe").writeBytes(
            fakePackedExeWithLegacyEvb(
                archive = archive,
                files = mapOf(
                    "Audio/BGM/theme.ogg" to "virtual-bgm".toByteArray(),
                    "Audio/SE/click.wav" to "virtual-se".toByteArray(),
                    "Fonts/game.ttf" to "virtual-font".toByteArray(),
                    "Game.exe" to "ignored".toByteArray(),
                ),
            ),
        )

        val output = temp.newFolder("packed-enigma-output")
        PackedRgssGameExtractor.extract(root, output)

        assertEquals("virtual-bgm", output.resolve("Audio/BGM/theme.ogg").readText())
        assertEquals("virtual-se", output.resolve("Audio/SE/click.wav").readText())
        assertEquals("virtual-font", output.resolve("Fonts/game.ttf").readText())
        assertTrue(!output.resolve("Game.exe").exists())
    }

    @Test
    fun rejectsFolderWithoutGameIni() {
        val root = temp.newFolder("not-game")

        assertFailsWith<GameValidationException> {
            detector.detect(root)
        }
    }

    private fun File.resolve(path: String): File = File(this, path)

    private fun zlib(text: String): ByteArray {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output).use { it.write(text.toByteArray()) }
        return output.toByteArray()
    }

    private fun rgss3Archive(entries: Map<String, ByteArray>): ByteArray {
        val seed = 1L
        val base = ((seed * 9L) + 3L) and UINT32_MASK
        val magic = 0x12345678L
        val names = entries.keys.map { name -> name.replace('\\', '/').toByteArray(Charsets.UTF_8) }
        val tableSize = 8 + 4 + entries.keys.zip(names).sumOf { (_, name) -> 16 + name.size } + 4
        var dataOffset = tableSize.toLong()

        val output = ByteArrayOutputStream()
        output.write(byteArrayOf('R'.code.toByte(), 'G'.code.toByte(), 'S'.code.toByte(), 'S'.code.toByte(), 'A'.code.toByte(), 'D'.code.toByte(), 0, 3))
        output.writeUInt32(seed)

        entries.entries.zip(names).forEach { (entry, rawName) ->
            output.writeUInt32(dataOffset xor base)
            output.writeUInt32(entry.value.size.toLong() xor base)
            output.writeUInt32(magic xor base)
            output.writeUInt32(rawName.size.toLong() xor base)
            output.write(encryptRgss3Name(rawName, base))
            dataOffset += entry.value.size
        }
        output.writeUInt32(base)

        entries.values.forEach { value ->
            output.write(encryptRgss3Data(value, magic))
        }
        return output.toByteArray()
    }

    private fun encryptRgss3Name(name: ByteArray, base: Long): ByteArray {
        return ByteArray(name.size) { index ->
            val value = name[index].toInt() and 0xff
            val key = ((base shr (8 * (index % 4))) and 0xffL).toInt()
            (value xor key).toByte()
        }
    }

    private fun encryptRgss3Data(data: ByteArray, initialMagic: Long): ByteArray {
        val output = ByteArray(data.size)
        var magic = initialMagic and UINT32_MASK
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(4, data.size - offset)
            var value = 0L
            for (index in 0 until chunkSize) {
                value = value or ((data[offset + index].toLong() and 0xffL) shl (8 * index))
            }
            value = (value xor magic) and UINT32_MASK
            for (index in 0 until chunkSize) {
                output[offset + index] = ((value shr (8 * index)) and 0xffL).toByte()
            }
            if (chunkSize == 4) {
                magic = ((magic * 7L) + 3L) and UINT32_MASK
            }
            offset += chunkSize
        }
        return output
    }

    private fun ByteArrayOutputStream.writeUInt32(value: Long) {
        write(byteArrayOf(
            (value and 0xffL).toByte(),
            ((value shr 8) and 0xffL).toByte(),
            ((value shr 16) and 0xffL).toByte(),
            ((value shr 24) and 0xffL).toByte(),
        ))
    }

    private fun ByteArrayOutputStream.writeEvbHeader(size: Long, objectsCount: Int) {
        writeUInt32(size)
        write(ByteArray(8))
        writeUInt32(objectsCount.toLong())
    }

    private fun ByteArrayOutputStream.writeLegacyEvbName(name: String, type: Int) {
        write(name.toByteArray(Charsets.UTF_16LE))
        write(byteArrayOf(0, 0, type.toByte()))
    }

    private fun ByteArrayOutputStream.writeLegacyEvbFolder(name: String, children: List<TestEvbNode>) {
        val nameBytes = name.toByteArray(Charsets.UTF_16LE)
        writeEvbHeader(size = 40L + nameBytes.size, objectsCount = children.size)
        writeLegacyEvbName(name, 3)
        write(ByteArray(25))
        children.forEach { child ->
            if (child.children == null) {
                writeLegacyEvbFile(child.name, child.data)
            } else {
                writeLegacyEvbFolder(child.name, child.children)
            }
        }
    }

    private fun ByteArrayOutputStream.writeLegacyEvbFile(name: String, data: ByteArray) {
        val nameBytes = name.toByteArray(Charsets.UTF_16LE)
        writeEvbHeader(size = 64L + nameBytes.size, objectsCount = 0)
        writeLegacyEvbName(name, 2)
        write(ByteArray(2))
        writeUInt32(data.size.toLong())
        write(ByteArray(4 + 8 + 8 + 8 + 7))
        writeUInt32(data.size.toLong())
        write(ByteArray(4))
        write(data)
    }

    private fun fakePackedExeWithLegacyEvb(archive: ByteArray, files: Map<String, ByteArray>): ByteArray {
        return "MZ".toByteArray() +
            ByteArray(128) +
            legacyEvbVirtualFileSystem(files) +
            fakePackedExe(archive).drop(2 + 128).toByteArray()
    }

    private fun legacyEvbVirtualFileSystem(files: Map<String, ByteArray>): ByteArray {
        val root = TestEvbNode("%DEFAULT FOLDER%", mutableListOf())
        files.forEach { (path, data) ->
            val parts = path.replace('\\', '/').split('/').filter { it.isNotBlank() }
            var current = root
            parts.dropLast(1).forEach { part ->
                val children = current.children ?: error("not a folder")
                current = children.firstOrNull { it.name == part && it.children != null }
                    ?: TestEvbNode(part, mutableListOf()).also { children += it }
            }
            current.children?.add(TestEvbNode(parts.last(), null, data))
        }

        return ByteArrayOutputStream().apply {
            write(byteArrayOf('E'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), 0))
            write(ByteArray(60))
            writeEvbHeader(size = 0, objectsCount = 1)
            writeLegacyEvbFolder(root.name, root.children.orEmpty())
        }.toByteArray()
    }

    private fun fakePackedExe(archive: ByteArray): ByteArray {
        val ini = """
            [Game]
            RTP=
            Library=System\RGSS300.dll
            Scripts=Data\Scripts.rvdata2
            Title=Packed Ace
        """.trimIndent().replace("\n", "\r\n").plus("\r\n").toByteArray()
        val size = ByteArray(8)
        var archiveSize = archive.size.toLong()
        for (index in size.indices) {
            size[index] = (archiveSize and 0xff).toByte()
            archiveSize = archiveSize shr 8
        }
        return "MZ".toByteArray() + ByteArray(128) + ini + size + archive + byteArrayOf(9, 9, 9)
    }

    private companion object {
        private const val UINT32_MASK = 0xffffffffL
    }

    private data class TestEvbNode(
        val name: String,
        val children: MutableList<TestEvbNode>?,
        val data: ByteArray = ByteArray(0),
    )
}
