package io.github.mkxpz.rpgplayer.domain

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import io.github.mkxpz.rpgplayer.data.RpgMakerEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class PackedRgssGame(
    val executableName: String,
    val title: String,
    val engine: RpgMakerEngine,
    val archiveName: String,
    val archiveOffset: Long,
    val archiveLength: Long?,
    val iniText: String,
)

object PackedRgssGameExtractor {
    private data class Rgss3Entry(
        val name: String,
        val offset: Long,
        val size: Long,
        val magic: Long,
    )

    private data class EvbHeader(
        val size: Long,
        val objectsCount: Int,
    )

    private data class EvbNamedNode(
        val name: String,
        val type: Int,
    )

    private data class EvbVirtualFile(
        val path: String,
        val originalSize: Long,
        val storedSize: Long,
        val offset: Long,
    )

    private val rgssadPrefix = byteArrayOf('R'.code.toByte(), 'G'.code.toByte(), 'S'.code.toByte(), 'S'.code.toByte(), 'A'.code.toByte(), 'D'.code.toByte(), 0)
    private val evbMagic = byteArrayOf('E'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), 0)
    private val gameSection = "[Game]".toByteArray(Charsets.US_ASCII)
    private val crlf = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
    private val shiftJis: Charset = Charset.forName("Shift_JIS")

    fun inspect(root: File): PackedRgssGame? {
        if (!root.isDirectory) return null
        return root.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("exe", ignoreCase = true) }
            .sortedWith(compareBy<File> { if (it.name.equals("Game.exe", ignoreCase = true)) 0 else 1 }.thenBy { it.name.lowercase(Locale.US) })
            .firstNotNullOfOrNull { executable ->
                runCatching {
                    executable.inputStream().buffered().use { input ->
                        inspectStream(
                            input = input,
                            executableName = executable.name,
                            fallbackTitle = root.name,
                            fileLength = executable.length(),
                        )
                    }
                }.getOrNull()
            }
    }

    fun inspect(root: DocumentFile, contentResolver: ContentResolver): PackedRgssGame? {
        if (!root.isDirectory) return null
        return root.listFiles()
            .filter { it.isFile && it.name?.endsWith(".exe", ignoreCase = true) == true }
            .sortedWith(compareBy<DocumentFile> { if (it.name.equals("Game.exe", ignoreCase = true)) 0 else 1 }.thenBy { it.name?.lowercase(Locale.US).orEmpty() })
            .firstNotNullOfOrNull { executable ->
                runCatching {
                    contentResolver.openInputStream(executable.uri)?.buffered()?.use { input ->
                        inspectStream(
                            input = input,
                            executableName = executable.name ?: "Game.exe",
                            fallbackTitle = root.name ?: "Packed RPG Maker Game",
                            fileLength = executable.length().takeIf { it > 0L },
                        )
                    }
                }.getOrNull()
            }
    }

    fun extract(root: File, destination: File): PackedRgssGame {
        val packed = inspect(root)
            ?: throw GameValidationException("没有找到可解包的 VX/VX Ace 打包 exe")
        val executable = root.listFiles()
            .orEmpty()
            .firstOrNull { it.isFile && it.name.equals(packed.executableName, ignoreCase = true) }
            ?: throw GameValidationException("找不到打包 exe：${packed.executableName}")

        destination.mkdirs()
        writeGameIni(packed, destination)
        executable.inputStream().buffered().use { input ->
            copyArchive(input, packed, File(destination, packed.archiveName))
        }
        copyPackedExternalResources(root, destination)
        extractEmbeddedVirtualResources(executable, destination)
        materializeCriticalAssets(destination)
        return packed
    }

    fun extract(root: DocumentFile, contentResolver: ContentResolver, destination: File): PackedRgssGame {
        val packed = inspect(root, contentResolver)
            ?: throw GameValidationException("没有找到可解包的 VX/VX Ace 打包 exe")
        val executable = root.listFiles()
            .firstOrNull { it.isFile && it.name.equals(packed.executableName, ignoreCase = true) }
            ?: throw GameValidationException("找不到打包 exe：${packed.executableName}")

        destination.mkdirs()
        writeGameIni(packed, destination)
        contentResolver.openInputStream(executable.uri)?.buffered()?.use { input ->
            copyArchive(input, packed, File(destination, packed.archiveName))
        } ?: throw GameValidationException("无法读取打包 exe：${packed.executableName}")
        copyPackedExternalResources(root, contentResolver, destination)
        extractEmbeddedVirtualResources(executable, contentResolver, destination)
        materializeCriticalAssets(destination)
        return packed
    }

    fun materializeCriticalAssets(root: File): Int {
        if (!root.isDirectory) return 0
        val archive = root.listFiles()
            .orEmpty()
            .firstOrNull { it.isFile && it.extension.equals("rgss3a", ignoreCase = true) }
            ?: return 0
        val marker = File(root, CRITICAL_ASSETS_MARKER)
        if (isCriticalAssetsMarkerValid(marker, archive, root)) {
            return 0
        }

        return runCatching {
            val assets = materializeCriticalRgss3Assets(archive, root)
            writeCriticalAssetsMarker(marker, archive, assets)
            assets.size
        }.getOrDefault(0)
    }

    fun extractEmbeddedVirtualResources(executable: File, destination: File): Int {
        if (!executable.isFile) return 0
        val extracted = runCatching {
            extractLegacyEvbVirtualResources(executable, destination)
        }.getOrDefault(0)
        if (extracted > 0) {
            File(destination, EVB_EXTRACTION_MARKER).writeText(extracted.toString())
        }
        return extracted
    }

    fun extractEmbeddedVirtualResources(
        executable: DocumentFile,
        contentResolver: ContentResolver,
        destination: File,
    ): Int {
        val temporaryExecutable = File(destination, ".packed-source.tmp")
        temporaryExecutable.delete()
        return try {
            contentResolver.openInputStream(executable.uri)?.buffered()?.use { input ->
                FileOutputStream(temporaryExecutable).use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            } ?: return 0
            extractEmbeddedVirtualResources(temporaryExecutable, destination)
        } finally {
            temporaryExecutable.delete()
        }
    }

    private fun inspectStream(
        input: InputStream,
        executableName: String,
        fallbackTitle: String,
        fileLength: Long?,
    ): PackedRgssGame? {
        val buffer = ByteArray(SCAN_BUFFER_SIZE)
        var tail = ByteArray(0)
        var offset = 0L

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue

            val combined = ByteArray(tail.size + read)
            System.arraycopy(tail, 0, combined, 0, tail.size)
            System.arraycopy(buffer, 0, combined, tail.size, read)
            val combinedBaseOffset = offset - tail.size
            val markerIndex = indexOfRgssad(combined)

            if (markerIndex >= 0) {
                val version = combined[markerIndex + rgssadPrefix.size].toInt() and 0xff
                val archiveOffset = combinedBaseOffset + markerIndex
                val archiveLength = readPackedFileLength(combined, markerIndex, fileLength, archiveOffset)
                val iniText = extractIniText(combined, markerIndex, fallbackTitle, version)
                val engine = detectEngine(iniText, version)
                val archiveName = archiveNameFor(version, engine)
                val title = parseIniValue(iniText, "Title").ifBlank { fallbackTitle }

                return PackedRgssGame(
                    executableName = executableName,
                    title = title,
                    engine = engine,
                    archiveName = archiveName,
                    archiveOffset = archiveOffset,
                    archiveLength = archiveLength,
                    iniText = normalizeIni(iniText, title, version),
                )
            }

            offset += read
            tail = combined.copyOfRange(max(0, combined.size - TAIL_SIZE), combined.size)
        }

        return null
    }

    private fun indexOfRgssad(bytes: ByteArray): Int {
        var index = 0
        while (index <= bytes.size - rgssadPrefix.size - 1) {
            if (matches(bytes, rgssadPrefix, index)) {
                val version = bytes[index + rgssadPrefix.size].toInt() and 0xff
                if (version in 1..3) {
                    return index
                }
            }
            index += 1
        }
        return -1
    }

    private fun readPackedFileLength(
        bytes: ByteArray,
        markerIndex: Int,
        fileLength: Long?,
        archiveOffset: Long,
    ): Long? {
        if (markerIndex < 8) return null

        var value = 0L
        for (i in 0 until 8) {
            value = value or ((bytes[markerIndex - 8 + i].toLong() and 0xffL) shl (8 * i))
        }

        if (value <= rgssadPrefix.size) return null
        if (fileLength != null && value > fileLength - archiveOffset) return null
        return value
    }

    private fun extractIniText(bytes: ByteArray, markerIndex: Int, fallbackTitle: String, version: Int): String {
        val windowStart = max(0, markerIndex - INI_WINDOW_SIZE)
        val gameIndex = lastIndexOf(bytes, gameSection, windowStart, markerIndex)
        if (gameIndex < 0) {
            return defaultIni(fallbackTitle, version)
        }

        val lastLineBreak = lastIndexOf(bytes, crlf, gameIndex, markerIndex)
        val end = if (lastLineBreak >= 0) lastLineBreak + crlf.size else markerIndex
        val iniBytes = bytes.copyOfRange(gameIndex, min(end, markerIndex))
        val decoded = decodeIni(iniBytes)
        return if ("Library=" in decoded && "Scripts=" in decoded) {
            decoded
        } else {
            defaultIni(fallbackTitle, version)
        }
    }

    private fun decodeIni(bytes: ByteArray): String {
        val utf8 = bytes.toString(Charsets.UTF_8)
        val sjis = runCatching { bytes.toString(shiftJis) }.getOrDefault(utf8)
        return if ('\uFFFD' in utf8 && '\uFFFD' !in sjis) sjis else utf8
    }

    private fun normalizeIni(iniText: String, fallbackTitle: String, version: Int): String {
        val rtp = parseIniValue(iniText, "RTP")
        val library = parseIniValue(iniText, "Library").ifBlank { defaultLibraryFor(version) }
        val scripts = parseIniValue(iniText, "Scripts").ifBlank { defaultScriptsFor(version) }
        val title = parseIniValue(iniText, "Title").ifBlank { fallbackTitle }
        return buildString {
            appendLine("[Game]")
            append("RTP=").appendLine(rtp)
            append("Library=").appendLine(library)
            append("Scripts=").appendLine(scripts)
            append("Title=").appendLine(title)
        }
    }

    private fun defaultIni(title: String, version: Int): String = buildString {
        appendLine("[Game]")
        appendLine("RTP=")
        append("Library=").appendLine(defaultLibraryFor(version))
        append("Scripts=").appendLine(defaultScriptsFor(version))
        append("Title=").appendLine(title)
    }

    private fun detectEngine(iniText: String, version: Int): RpgMakerEngine {
        val library = parseIniValue(iniText, "Library").uppercase(Locale.US)
        val scripts = parseIniValue(iniText, "Scripts").uppercase(Locale.US)
        return when {
            version == 3 || "RGSS3" in library || scripts.endsWith(".RVDATA2") -> RpgMakerEngine.VX_ACE
            version == 2 || "RGSS2" in library || scripts.endsWith(".RVDATA") -> RpgMakerEngine.VX
            else -> RpgMakerEngine.XP
        }
    }

    private fun archiveNameFor(version: Int, engine: RpgMakerEngine): String = when {
        version == 3 || engine == RpgMakerEngine.VX_ACE -> "Game.rgss3a"
        version == 2 || engine == RpgMakerEngine.VX -> "Game.rgss2a"
        else -> "Game.rgssad"
    }

    private fun defaultLibraryFor(version: Int): String = when (version) {
        3 -> "System\\RGSS300.dll"
        2 -> "RGSS202E.dll"
        else -> "RGSS102E.dll"
    }

    private fun defaultScriptsFor(version: Int): String = when (version) {
        3 -> "Data\\Scripts.rvdata2"
        2 -> "Data\\Scripts.rvdata"
        else -> "Data\\Scripts.rxdata"
    }

    private fun parseIniValue(iniText: String, key: String): String {
        val prefix = "$key="
        return iniText.lineSequence()
            .firstOrNull { it.trimStart().startsWith(prefix, ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            .orEmpty()
    }

    private fun writeGameIni(packed: PackedRgssGame, destination: File) {
        File(destination, "Game.ini").writeText(packed.iniText, Charsets.UTF_8)
    }

    private fun extractLegacyEvbVirtualResources(executable: File, destination: File): Int {
        var extracted = 0
        RandomAccessFile(executable, "r").use { file ->
            val magicOffset = findBytes(file, evbMagic) ?: return 0
            file.seek(magicOffset)
            val signature = ByteArray(evbMagic.size)
            file.readFully(signature)
            if (!signature.contentEquals(evbMagic)) return 0
            file.skipFully(EVB_PACK_HEADER_PADDING)

            val main = readEvbHeader(file)
            extracted += extractLegacyEvbChildren(
                file = file,
                parentPath = "",
                objectCount = main.objectsCount,
                destination = destination,
            )
        }
        return extracted
    }

    private fun extractLegacyEvbChildren(
        file: RandomAccessFile,
        parentPath: String,
        objectCount: Int,
        destination: File,
    ): Int {
        var extracted = 0
        repeat(objectCount.coerceAtLeast(0)) {
            val nodeStart = file.filePointer
            val header = readEvbHeader(file)
            val named = readEvbNamedNode(file)
            val name = if (named.name == EVB_DEFAULT_FOLDER) "" else named.name
            if (!isSafeEvbName(name)) {
                file.seek(nodeStart + header.size + 4L)
                return@repeat
            }
            val path = joinVirtualPath(parentPath, name)

            when (named.type) {
                EVB_NODE_FILE -> {
                    val optionalOffset = nodeStart + header.size + 4L - EVB_OPTIONAL_PE_FILE_NODE_SIZE
                    if (optionalOffset < file.filePointer) {
                        file.seek(nodeStart + header.size + 4L)
                        return@repeat
                    }
                    file.seek(optionalOffset)
                    val virtualFile = readLegacyEvbFileNode(file, path)
                    if (shouldExtractEvbVirtualFile(virtualFile.path)) {
                        extracted += copyLegacyEvbFile(file, virtualFile, destination)
                    }
                    file.seek(virtualFile.offset + virtualFile.storedSize)
                }
                EVB_NODE_FOLDER -> {
                    file.seek(nodeStart + header.size + 4L)
                    extracted += extractLegacyEvbChildren(
                        file = file,
                        parentPath = path,
                        objectCount = header.objectsCount,
                        destination = destination,
                    )
                }
                else -> {
                    file.seek(nodeStart + header.size + 4L)
                }
            }
        }
        return extracted
    }

    private fun readEvbHeader(file: RandomAccessFile): EvbHeader {
        val size = file.readUInt32()
        file.skipFully(8)
        val objectCount = file.readUInt32().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return EvbHeader(size, objectCount)
    }

    private fun readEvbNamedNode(file: RandomAccessFile): EvbNamedNode {
        val nameBytes = ByteArrayOutputStream()
        while (true) {
            val low = file.read()
            val high = file.read()
            if (low < 0 || high < 0) {
                throw GameValidationException("EVB virtual file table is incomplete")
            }
            if (low == 0 && high == 0) break
            nameBytes.write(low)
            nameBytes.write(high)
            if (nameBytes.size() > MAX_EVB_NAME_BYTES) {
                throw GameValidationException("EVB virtual file name is too long")
            }
        }
        val type = file.read()
        if (type < 0) {
            throw GameValidationException("EVB virtual node type is missing")
        }
        return EvbNamedNode(nameBytes.toByteArray().toString(Charsets.UTF_16LE), type)
    }

    private fun readLegacyEvbFileNode(file: RandomAccessFile, path: String): EvbVirtualFile {
        file.skipFully(2)
        val originalSize = file.readUInt32()
        file.skipFully(4 + 8 + 8 + 8 + 7)
        val storedSize = file.readUInt32()
        file.skipFully(4)
        return EvbVirtualFile(
            path = path,
            originalSize = originalSize,
            storedSize = storedSize,
            offset = file.filePointer,
        )
    }

    private fun copyLegacyEvbFile(file: RandomAccessFile, virtualFile: EvbVirtualFile, destination: File): Int {
        if (virtualFile.originalSize != virtualFile.storedSize) {
            return 0
        }
        if (virtualFile.storedSize <= 0L || virtualFile.storedSize > MAX_EVB_RESOURCE_SIZE) {
            return 0
        }
        if (virtualFile.offset + virtualFile.storedSize > file.length()) {
            return 0
        }

        val target = File(destination, virtualFile.path).canonicalFile
        val canonicalDestination = destination.canonicalFile
        if (!target.path.startsWith(canonicalDestination.path + File.separator)) {
            return 0
        }

        target.parentFile?.mkdirs()
        file.seek(virtualFile.offset)
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var remaining = virtualFile.storedSize
            while (remaining > 0L) {
                val read = file.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
        return 1
    }

    private fun shouldExtractEvbVirtualFile(path: String): Boolean {
        val normalized = path.replace('\\', '/').lowercase(Locale.US)
        return normalized.startsWith("audio/") ||
            normalized.startsWith("fonts/") ||
            normalized.startsWith("system/")
    }

    private fun isSafeEvbName(name: String): Boolean {
        return name != "." &&
            name != ".." &&
            '/' !in name &&
            '\\' !in name &&
            ':' !in name
    }

    private fun joinVirtualPath(parent: String, child: String): String {
        return when {
            parent.isBlank() -> child
            child.isBlank() -> parent
            else -> "$parent/$child"
        }
    }

    private fun findBytes(file: RandomAccessFile, pattern: ByteArray): Long? {
        file.seek(0)
        val buffer = ByteArray(SCAN_BUFFER_SIZE)
        var fileOffset = 0L
        var tail = ByteArray(0)

        while (true) {
            val read = file.read(buffer)
            if (read < 0) return null
            if (read == 0) continue

            val combined = ByteArray(tail.size + read)
            System.arraycopy(tail, 0, combined, 0, tail.size)
            System.arraycopy(buffer, 0, combined, tail.size, read)
            val index = indexOf(combined, pattern)
            if (index >= 0) {
                return fileOffset - tail.size + index
            }

            fileOffset += read
            tail = combined.copyOfRange(max(0, combined.size - pattern.size + 1), combined.size)
        }
    }

    private fun indexOf(bytes: ByteArray, pattern: ByteArray): Int {
        var index = 0
        while (index <= bytes.size - pattern.size) {
            if (matches(bytes, pattern, index)) return index
            index += 1
        }
        return -1
    }

    private fun copyArchive(input: InputStream, packed: PackedRgssGame, target: File) {
        target.parentFile?.mkdirs()
        skipFully(input, packed.archiveOffset)
        FileOutputStream(target).use { output ->
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            var remaining = packed.archiveLength
            while (remaining == null || remaining > 0L) {
                val maxRead = remaining?.let { min(buffer.size.toLong(), it).toInt() } ?: buffer.size
                val read = input.read(buffer, 0, maxRead)
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining = remaining?.minus(read)
            }
        }
    }

    private fun materializeCriticalRgss3Assets(archive: File, destination: File): List<String> {
        val extracted = mutableListOf<String>()
        RandomAccessFile(archive, "r").use { file ->
            val header = ByteArray(8)
            if (file.length() < header.size + 4L) return emptyList()
            file.readFully(header)
            if (!header.copyOfRange(0, 7).contentEquals(rgssadPrefix) || header[7].toInt() != 3) return emptyList()

            val baseMagic = ((file.readUInt32() * 9L) + 3L) and UINT32_MASK
            while (true) {
                val offset = file.readUInt32Xor(baseMagic)
                if (offset == 0L) break
                val size = file.readUInt32Xor(baseMagic)
                val magic = file.readUInt32Xor(baseMagic)
                val nameLength = file.readUInt32Xor(baseMagic)

                if (nameLength <= 0L || nameLength > MAX_RGSS3_NAME_LENGTH) return extracted
                val rawName = ByteArray(nameLength.toInt())
                file.readFully(rawName)
                val name = decodeRgss3Name(rawName, baseMagic)
                val targetPath = canonicalCriticalAssetPath(name) ?: continue
                if (size <= 0L || size > MAX_CRITICAL_ASSET_SIZE || offset + size > file.length()) continue

                val tablePosition = file.filePointer
                val bytes = readRgss3Entry(file, Rgss3Entry(name, offset, size, magic))
                val target = File(destination, targetPath)
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
                extracted += targetPath
                file.seek(tablePosition)
            }
        }
        return extracted
    }

    private fun isCriticalAssetsMarkerValid(marker: File, archive: File, destination: File): Boolean {
        if (!marker.isFile) return false
        val lines = runCatching { marker.readLines() }.getOrDefault(emptyList())
        if (!lines.contains("archive=${archive.name}")) return false
        if (!lines.contains("length=${archive.length()}")) return false
        if (!lines.contains("modified=${archive.lastModified()}")) return false

        return lines.asSequence()
            .filter { it.startsWith("path=") }
            .map { it.removePrefix("path=") }
            .all { relativePath -> File(destination, relativePath).isFile }
    }

    private fun writeCriticalAssetsMarker(marker: File, archive: File, assets: List<String>) {
        marker.writeText(
            buildString {
                append("archive=").appendLine(archive.name)
                append("length=").appendLine(archive.length())
                append("modified=").appendLine(archive.lastModified())
                append("count=").appendLine(assets.size)
                assets.forEach { path -> append("path=").appendLine(path) }
            },
        )
    }

    private fun RandomAccessFile.readUInt32(): Long {
        val b0 = read()
        val b1 = read()
        val b2 = read()
        val b3 = read()
        if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
            throw GameValidationException("RGSS3 archive table is incomplete")
        }
        return ((b0.toLong() and 0xffL) or
            ((b1.toLong() and 0xffL) shl 8) or
            ((b2.toLong() and 0xffL) shl 16) or
            ((b3.toLong() and 0xffL) shl 24)) and UINT32_MASK
    }

    private fun RandomAccessFile.readUInt32Xor(baseMagic: Long): Long {
        return (readUInt32() xor baseMagic) and UINT32_MASK
    }

    private fun RandomAccessFile.skipFully(length: Long) {
        if (length <= 0L) return
        val target = filePointer + length
        if (target > this.length()) {
            throw GameValidationException("Packed executable data is incomplete")
        }
        seek(target)
    }

    private fun decodeRgss3Name(rawName: ByteArray, baseMagic: Long): String {
        for (index in rawName.indices) {
            val key = ((baseMagic shr (8 * (index % 4))) and 0xffL).toInt()
            rawName[index] = (rawName[index].toInt() xor key).toByte()
            if (rawName[index].toInt() == '\\'.code) {
                rawName[index] = '/'.code.toByte()
            }
        }
        return rawName.toString(Charsets.UTF_8)
    }

    private fun canonicalCriticalAssetPath(name: String): String? {
        val normalized = name.replace('\\', '/')
        val lower = normalized.lowercase(Locale.US)
        if (!lower.startsWith("graphics/system/")) return null

        val basename = normalized.substringAfterLast('/')
        return when (lower.substringAfterLast('/')) {
            "window.png" -> "Graphics/System/Window.png"
            "iconset.png" -> "Graphics/System/IconSet.png"
            "balloon.png" -> "Graphics/System/Balloon.png"
            "shadow.png" -> "Graphics/System/Shadow.png"
            else -> "Graphics/System/$basename"
        }
    }

    private fun readRgss3Entry(file: RandomAccessFile, entry: Rgss3Entry): ByteArray {
        val encrypted = ByteArray(entry.size.toInt())
        file.seek(entry.offset)
        file.readFully(encrypted)

        val output = ByteArray(encrypted.size)
        var magic = entry.magic and UINT32_MASK
        var offset = 0
        while (offset < encrypted.size) {
            val chunkSize = min(4, encrypted.size - offset)
            var value = 0L
            for (index in 0 until chunkSize) {
                value = value or ((encrypted[offset + index].toLong() and 0xffL) shl (8 * index))
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

    private fun skipFully(input: InputStream, bytesToSkip: Long) {
        var remaining = bytesToSkip
        val scratch = ByteArray(COPY_BUFFER_SIZE)
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(scratch, 0, min(scratch.size.toLong(), remaining).toInt())
                if (read < 0) {
                    throw GameValidationException("打包 exe 数据不完整，无法定位 RGSS 加密包")
                }
                remaining -= read
            }
        }
    }

    private fun copyPackedExternalResources(source: File, destination: File) {
        source.listFiles().orEmpty()
            .filter { file -> shouldCopyPackedExternalResource(file.name, file.isDirectory) }
            .forEach { file ->
                file.copyRecursively(File(destination, file.name), overwrite = true)
            }
    }

    private fun copyPackedExternalResources(
        source: DocumentFile,
        contentResolver: ContentResolver,
        destination: File,
    ) {
        source.listFiles()
            .filter { file -> shouldCopyPackedExternalResource(file.name.orEmpty(), file.isDirectory) }
            .forEach { file ->
                val name = file.name?.replace("/", "_") ?: return@forEach
                copyDocumentResource(file, contentResolver, File(destination, name))
            }
    }

    private fun copyDocumentResource(source: DocumentFile, contentResolver: ContentResolver, destination: File) {
        if (source.isDirectory) {
            destination.mkdirs()
            source.listFiles().forEach { child ->
                val childName = child.name?.replace("/", "_") ?: return@forEach
                copyDocumentResource(child, contentResolver, File(destination, childName))
            }
            return
        }

        if (source.isFile) {
            destination.parentFile?.mkdirs()
            contentResolver.openInputStream(source.uri)?.buffered()?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            } ?: throw GameValidationException("Unable to read packed resource: ${source.name}")
        }
    }

    private fun shouldCopyPackedExternalResource(name: String, isDirectory: Boolean): Boolean {
        val normalized = name.lowercase(Locale.US)
        if (isDirectory) {
            return normalized in PACKED_EXTERNAL_DIRECTORIES
        }
        return normalized in PACKED_EXTERNAL_FILES ||
            normalized.endsWith(".sf2") ||
            normalized.endsWith(".sf3") ||
            normalized.endsWith(".ttf") ||
            normalized.endsWith(".otf")
    }

    private fun matches(bytes: ByteArray, pattern: ByteArray, offset: Int): Boolean {
        if (offset < 0 || offset + pattern.size > bytes.size) return false
        for (index in pattern.indices) {
            if (bytes[offset + index] != pattern[index]) return false
        }
        return true
    }

    private fun lastIndexOf(bytes: ByteArray, pattern: ByteArray, start: Int, endExclusive: Int): Int {
        var index = min(endExclusive - pattern.size, bytes.size - pattern.size)
        while (index >= start) {
            if (matches(bytes, pattern, index)) return index
            index -= 1
        }
        return -1
    }

    private const val SCAN_BUFFER_SIZE = 64 * 1024
    private const val TAIL_SIZE = 64 * 1024
    private const val INI_WINDOW_SIZE = 16 * 1024
    private const val COPY_BUFFER_SIZE = 1024 * 1024
    private const val MAX_RGSS3_NAME_LENGTH = 4096L
    private const val MAX_CRITICAL_ASSET_SIZE = 8L * 1024L * 1024L
    private const val MAX_EVB_NAME_BYTES = 8192
    private const val MAX_EVB_RESOURCE_SIZE = 256L * 1024L * 1024L
    private const val EVB_PACK_HEADER_PADDING = 60L
    private const val EVB_OPTIONAL_PE_FILE_NODE_SIZE = 49L
    private const val EVB_DEFAULT_FOLDER = "%DEFAULT FOLDER%"
    private const val EVB_NODE_FILE = 2
    private const val EVB_NODE_FOLDER = 3
    private const val CRITICAL_ASSETS_MARKER = ".mkxpz-critical-assets"
    const val EVB_EXTRACTION_MARKER = ".packed-audio-extracted"
    private const val UINT32_MASK = 0xffffffffL

    private val PACKED_EXTERNAL_DIRECTORIES = setOf(
        "audio",
        "fonts",
        "movies",
        "graphics",
        "data",
        "saves",
        "save",
    )
    private val PACKED_EXTERNAL_FILES = setOf(
        "game.ico",
        "icon.png",
    )
}
