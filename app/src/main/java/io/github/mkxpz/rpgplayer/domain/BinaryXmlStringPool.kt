package io.github.mkxpz.rpgplayer.domain

import java.io.ByteArrayOutputStream

object BinaryXmlStringPool {
    fun replaceStrings(xml: ByteArray, replacements: Map<String, String>): ByteArray {
        if (xml.size < 8 || replacements.isEmpty()) return xml
        val rootHeaderSize = readUInt16(xml, 2)
        var offset = rootHeaderSize
        while (offset + 8 <= xml.size) {
            val type = readUInt16(xml, offset)
            val size = readInt(xml, offset + 4)
            if (size <= 0 || offset + size > xml.size) break
            if (type == RES_STRING_POOL_TYPE) {
                val chunk = xml.copyOfRange(offset, offset + size)
                val patched = rebuildStringPool(chunk, replacements)
                if (patched.contentEquals(chunk)) return xml

                val output = ByteArray(xml.size - size + patched.size)
                System.arraycopy(xml, 0, output, 0, offset)
                System.arraycopy(patched, 0, output, offset, patched.size)
                System.arraycopy(xml, offset + size, output, offset + patched.size, xml.size - offset - size)
                if (readUInt16(output, 0) == RES_XML_TYPE) {
                    writeInt(output, 4, output.size)
                }
                return output
            }
            offset += size
        }
        return xml
    }

    fun patchVersionCode(xml: ByteArray, versionCode: Int): ByteArray {
        val strings = readXmlStringPool(xml) ?: return xml
        val output = xml.copyOf()
        val rootHeaderSize = readUInt16(output, 2)
        var offset = rootHeaderSize
        while (offset + 8 <= output.size) {
            val type = readUInt16(output, offset)
            val size = readInt(output, offset + 4)
            if (size <= 0 || offset + size > output.size) break
            if (type == RES_XML_START_ELEMENT_TYPE && size >= 36) {
                val attributeStart = readUInt16(output, offset + 24)
                val attributeSize = readUInt16(output, offset + 26).takeIf { it > 0 } ?: 20
                val attributeCount = readUInt16(output, offset + 28)
                val attributesOffset = offset + 16 + attributeStart
                for (index in 0 until attributeCount) {
                    val attributeOffset = attributesOffset + index * attributeSize
                    if (attributeOffset + 20 > offset + size) break
                    val nameIndex = readInt(output, attributeOffset + 4)
                    if (strings.getOrNull(nameIndex) == "versionCode") {
                        writeInt(output, attributeOffset + 8, -1)
                        writeUInt16(output, attributeOffset + 12, 8)
                        output[attributeOffset + 14] = 0
                        output[attributeOffset + 15] = TYPE_INT_DEC.toByte()
                        writeInt(output, attributeOffset + 16, versionCode)
                    }
                }
            }
            offset += size
        }
        return output
    }

    private fun readXmlStringPool(xml: ByteArray): List<String>? {
        if (xml.size < 8) return null
        val rootHeaderSize = readUInt16(xml, 2)
        var offset = rootHeaderSize
        while (offset + 8 <= xml.size) {
            val type = readUInt16(xml, offset)
            val size = readInt(xml, offset + 4)
            if (size <= 0 || offset + size > xml.size) return null
            if (type == RES_STRING_POOL_TYPE) {
                return decodeStringPool(xml.copyOfRange(offset, offset + size))
            }
            offset += size
        }
        return null
    }

    private fun rebuildStringPool(chunk: ByteArray, replacements: Map<String, String>): ByteArray {
        if (chunk.size < 28) return chunk
        val headerSize = readUInt16(chunk, 2)
        val stringCount = readInt(chunk, 8)
        val styleCount = readInt(chunk, 12)
        val flags = readInt(chunk, 16)
        val stringsStart = readInt(chunk, 20)
        val stylesStart = readInt(chunk, 24)
        if (headerSize <= 0 || stringsStart <= 0 || stringsStart > chunk.size) return chunk

        val isUtf8 = flags and UTF8_FLAG != 0
        val strings = decodeStringPool(chunk)
        if (strings.size != stringCount) return chunk

        val rebuiltStrings = ByteArrayOutputStream()
        val offsets = IntArray(stringCount)
        strings.forEachIndexed { index, value ->
            offsets[index] = rebuiltStrings.size()
            val next = replacements[value] ?: value
            if (isUtf8) {
                writeUtf8String(rebuiltStrings, next)
            } else {
                writeUtf16String(rebuiltStrings, next)
            }
        }
        padToFourBytes(rebuiltStrings)

        val styleOffsetsStart = headerSize + stringCount * 4
        val styleOffsetsSize = styleCount * 4
        val styleOffsets = if (styleCount > 0 && styleOffsetsStart + styleOffsetsSize <= chunk.size) {
            chunk.copyOfRange(styleOffsetsStart, styleOffsetsStart + styleOffsetsSize)
        } else {
            ByteArray(0)
        }
        val styleData = if (styleCount > 0 && stylesStart > 0 && stylesStart <= chunk.size) {
            chunk.copyOfRange(stylesStart, chunk.size)
        } else {
            ByteArray(0)
        }

        val newStringsStart = headerSize + stringCount * 4 + styleOffsets.size
        val newStylesStart = if (styleData.isNotEmpty()) newStringsStart + rebuiltStrings.size() else 0
        val newSize = newStringsStart + rebuiltStrings.size() + styleData.size
        val output = ByteArrayOutputStream(newSize)
        val header = chunk.copyOfRange(0, headerSize)
        writeInt(header, 4, newSize)
        writeInt(header, 20, newStringsStart)
        writeInt(header, 24, newStylesStart)
        output.write(header)
        offsets.forEach { writeInt(output, it) }
        output.write(styleOffsets)
        output.write(rebuiltStrings.toByteArray())
        output.write(styleData)
        return output.toByteArray()
    }

    private fun decodeStringPool(chunk: ByteArray): List<String> {
        if (chunk.size < 28) return emptyList()
        val headerSize = readUInt16(chunk, 2)
        val stringCount = readInt(chunk, 8)
        val flags = readInt(chunk, 16)
        val stringsStart = readInt(chunk, 20)
        if (stringCount < 0 || headerSize + stringCount * 4 > chunk.size || stringsStart > chunk.size) {
            return emptyList()
        }
        val isUtf8 = flags and UTF8_FLAG != 0
        return List(stringCount) { index ->
            val stringOffset = readInt(chunk, headerSize + index * 4)
            val absolute = stringsStart + stringOffset
            if (absolute !in chunk.indices) {
                ""
            } else if (isUtf8) {
                readUtf8String(chunk, absolute)
            } else {
                readUtf16String(chunk, absolute)
            }
        }
    }

    private fun readUtf8String(bytes: ByteArray, offset: Int): String {
        val first = readLength8(bytes, offset)
        val second = readLength8(bytes, first.next)
        val start = second.next
        val end = (start + second.length).coerceAtMost(bytes.size)
        return bytes.copyOfRange(start, end).toString(Charsets.UTF_8)
    }

    private fun readUtf16String(bytes: ByteArray, offset: Int): String {
        val length = readLength16(bytes, offset)
        val start = length.next
        val byteCount = length.length * 2
        val end = (start + byteCount).coerceAtMost(bytes.size)
        return bytes.copyOfRange(start, end).toString(Charsets.UTF_16LE)
    }

    private fun writeUtf8String(output: ByteArrayOutputStream, value: String) {
        val utf8 = value.toByteArray(Charsets.UTF_8)
        writeLength8(output, value.length)
        writeLength8(output, utf8.size)
        output.write(utf8)
        output.write(0)
    }

    private fun writeUtf16String(output: ByteArrayOutputStream, value: String) {
        val utf16 = value.toByteArray(Charsets.UTF_16LE)
        writeLength16(output, value.length)
        output.write(utf16)
        output.write(0)
        output.write(0)
    }

    private fun readLength8(bytes: ByteArray, offset: Int): LengthValue {
        val first = bytes.getOrNull(offset)?.toInt()?.and(0xff) ?: return LengthValue(0, offset)
        return if (first and 0x80 == 0) {
            LengthValue(first, offset + 1)
        } else {
            val second = bytes.getOrNull(offset + 1)?.toInt()?.and(0xff) ?: 0
            LengthValue(((first and 0x7f) shl 8) or second, offset + 2)
        }
    }

    private fun readLength16(bytes: ByteArray, offset: Int): LengthValue {
        val first = readUInt16(bytes, offset)
        return if (first and 0x8000 == 0) {
            LengthValue(first, offset + 2)
        } else {
            val second = readUInt16(bytes, offset + 2)
            LengthValue(((first and 0x7fff) shl 16) or second, offset + 4)
        }
    }

    private fun writeLength8(output: ByteArrayOutputStream, length: Int) {
        if (length <= 0x7f) {
            output.write(length)
        } else {
            output.write((length shr 8) or 0x80)
            output.write(length and 0xff)
        }
    }

    private fun writeLength16(output: ByteArrayOutputStream, length: Int) {
        if (length <= 0x7fff) {
            writeUInt16(output, length)
        } else {
            writeUInt16(output, (length shr 16) or 0x8000)
            writeUInt16(output, length and 0xffff)
        }
    }

    private fun padToFourBytes(output: ByteArrayOutputStream) {
        while (output.size() % 4 != 0) {
            output.write(0)
        }
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int {
        if (offset + 1 >= bytes.size) return 0
        return (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        return (bytes[offset].toInt() and 0xff) or
            ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or
            ((bytes[offset + 3].toInt() and 0xff) shl 24)
    }

    private fun writeUInt16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xff).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xff).toByte()
    }

    private fun writeUInt16(output: ByteArrayOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
    }

    private fun writeInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xff).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xff).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xff).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xff).toByte()
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value ushr 8) and 0xff)
        output.write((value ushr 16) and 0xff)
        output.write((value ushr 24) and 0xff)
    }

    private data class LengthValue(
        val length: Int,
        val next: Int,
    )

    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_TYPE = 0x0003
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val UTF8_FLAG = 0x00000100
    private const val TYPE_INT_DEC = 0x10
}
