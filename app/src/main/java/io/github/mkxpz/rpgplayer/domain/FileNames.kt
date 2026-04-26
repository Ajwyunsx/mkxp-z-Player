package io.github.mkxpz.rpgplayer.domain

internal fun String.safeFileName(): String {
    val cleaned = trim()
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), " ")
        .take(80)
    return cleaned.ifBlank { "RPG Maker Game" }
}
