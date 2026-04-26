package io.github.mkxpz.rpgplayer.domain

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class Html5Compatibility(
    val threeDPluginNames: List<String> = emptyList(),
) {
    val hasThreeDPlugins: Boolean
        get() = threeDPluginNames.isNotEmpty()
}

class Html5CompatibilityDetector {
    fun detect(root: File): Html5Compatibility {
        val webRoot = Html5WebRootLocator.find(root) ?: return Html5Compatibility()
        return Html5Compatibility(detectThreeDPluginNames(webRoot))
    }

    companion object {
        fun detectThreeDPluginNames(webRoot: File): List<String> {
            val names = linkedSetOf<String>()
            val pluginsDir = webRoot.resolve("js").findChildIgnoreCase("plugins")
            pluginsDir?.listFiles()
                ?.filter { it.isFile && it.extension.equals("js", ignoreCase = true) }
                ?.forEach { file ->
                    displayThreeDPluginName(file.nameWithoutExtension)?.let(names::add)
                }

            webRoot.resolve("js").findChildIgnoreCase("plugins.js")
                ?.takeIf(File::isFile)
                ?.let { runCatching { it.readText() }.getOrNull() }
                ?.let { text -> names += detectThreeDPluginNamesInText(text) }

            webRoot.resolve("index.html")
                .takeIf(File::isFile)
                ?.let { runCatching { it.readText() }.getOrNull() }
                ?.let { text -> names += detectThreeDPluginNamesInText(text) }

            return names.toList()
        }

        fun detectThreeDPluginNames(
            webRoot: DocumentFile,
            contentResolver: ContentResolver,
        ): List<String> {
            val names = linkedSetOf<String>()
            val jsDir = webRoot.findChildIgnoreCase("js")
            val pluginsDir = jsDir?.findChildIgnoreCase("plugins")
            pluginsDir?.listFiles()
                ?.filter { it.isFile && it.name?.endsWith(".js", ignoreCase = true) == true }
                ?.forEach { file ->
                    displayThreeDPluginName(file.name.orEmpty().substringBeforeLast("."))?.let(names::add)
                }

            jsDir?.findChildIgnoreCase("plugins.js")
                ?.takeIf(DocumentFile::isFile)
                ?.let { readDocumentTextSafely(it, contentResolver) }
                ?.let { text -> names += detectThreeDPluginNamesInText(text) }

            webRoot.findChildIgnoreCase("index.html")
                ?.takeIf(DocumentFile::isFile)
                ?.let { readDocumentTextSafely(it, contentResolver) }
                ?.let { text -> names += detectThreeDPluginNamesInText(text) }

            return names.toList()
        }

        private fun detectThreeDPluginNamesInText(text: String): List<String> {
            val names = linkedSetOf<String>()
            pluginNameRegex.findAll(text).forEach { match ->
                displayThreeDPluginName(match.groupValues[1])?.let(names::add)
            }
            scriptSrcRegex.findAll(text).forEach { match ->
                val fileName = match.groupValues[1]
                    .substringAfterLast("/")
                    .substringBefore("?")
                    .substringBefore("#")
                    .substringBeforeLast(".")
                displayThreeDPluginName(fileName)?.let(names::add)
            }

            val normalizedText = normalizePluginName(text)
            threeDPluginDisplayNames.forEach { (signature, displayName) ->
                if (signature in normalizedText) {
                    if (displayName == "Mode7" && names.any { it.contains("Mode7", ignoreCase = true) }) {
                        return@forEach
                    }
                    names.add(displayName)
                }
            }
            return names.toList()
        }

        private fun displayThreeDPluginName(name: String): String? {
            val normalized = normalizePluginName(name)
            return threeDPluginDisplayNames.entries
                .firstOrNull { (signature, _) -> normalized == signature || normalized.contains(signature) }
                ?.value
        }

        private fun normalizePluginName(value: String): String =
            value.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

        private fun readDocumentTextSafely(
            file: DocumentFile,
            contentResolver: ContentResolver,
        ): String = runCatching {
            contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull().orEmpty()

        private val pluginNameRegex =
            """"name"\s*:\s*"([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
        private val scriptSrcRegex =
            """<script[^>]+src\s*=\s*["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)

        private val threeDPluginDisplayNames = linkedMapOf(
            "MV3D" to "MV3D",
            "MZ3D" to "MZ3D",
            "ULTRAMODE7" to "UltraMode7",
            "TDDPULTRAMODE7" to "TDDP_UltraMode7",
            "MBSFPLE" to "MBS_FPLE",
            "MBSMODE7" to "MBS_Mode7",
            "MODE7" to "Mode7",
        )
    }
}
