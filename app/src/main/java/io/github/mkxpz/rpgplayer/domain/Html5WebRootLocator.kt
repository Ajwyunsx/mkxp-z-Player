package io.github.mkxpz.rpgplayer.domain

import androidx.documentfile.provider.DocumentFile
import java.io.File

object Html5WebRootLocator {
    fun find(root: File): File? {
        if (root.resolve("index.html").isFile && root.hasHtml5Core()) return root
        val www = root.findChildIgnoreCase("www")
        if (www?.resolve("index.html")?.isFile == true && www.hasHtml5Core()) return www
        return null
    }

    fun find(root: DocumentFile): DocumentFile? {
        if (root.findChildIgnoreCase("index.html")?.isFile == true && root.hasHtml5Core()) return root
        val www = root.findChildIgnoreCase("www")
        if (www?.findChildIgnoreCase("index.html")?.isFile == true && www.hasHtml5Core()) return www
        return null
    }

    private fun File.hasHtml5Core(): Boolean {
        val jsDir = resolve("js")
        return jsDir.resolve("rpg_core.js").isFile ||
            jsDir.resolve("rpg_managers.js").isFile ||
            jsDir.resolve("rmmz_core.js").isFile ||
            jsDir.resolve("rmmz_managers.js").isFile
    }

    private fun DocumentFile.hasHtml5Core(): Boolean {
        val jsDir = findChildIgnoreCase("js") ?: return false
        return jsDir.findChildIgnoreCase("rpg_core.js")?.isFile == true ||
            jsDir.findChildIgnoreCase("rpg_managers.js")?.isFile == true ||
            jsDir.findChildIgnoreCase("rmmz_core.js")?.isFile == true ||
            jsDir.findChildIgnoreCase("rmmz_managers.js")?.isFile == true
    }
}

internal fun File.findChildIgnoreCase(name: String): File? =
    listFiles()?.firstOrNull { it.name.equals(name, ignoreCase = true) }

internal fun DocumentFile.findChildIgnoreCase(name: String): DocumentFile? =
    listFiles().firstOrNull { it.name.equals(name, ignoreCase = true) }
