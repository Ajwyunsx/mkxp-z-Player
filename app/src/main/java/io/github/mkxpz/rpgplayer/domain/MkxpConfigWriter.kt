package io.github.mkxpz.rpgplayer.domain

import io.github.mkxpz.rpgplayer.data.GameEntry
import io.github.mkxpz.rpgplayer.data.LauncherSettings
import org.json.JSONObject
import java.io.File

class MkxpConfigWriter {
    fun writeConfig(entry: GameEntry, settings: LauncherSettings, output: File): File {
        output.parentFile?.mkdirs()

        val json = JSONObject()
            .put("gameFolder", entry.installedPath)
            .put("rgssVersion", entry.engine.rgssVersion)
            .put("windowTitle", entry.title)
            .put("fullscreen", true)
            .put("fixedAspectRatio", settings.keepAspectRatio)
            .put("smoothScaling", settings.smoothScaling)
            .put("pathCache", true)
            .put("fixedFramerate", settings.fixedFramerate)
            .put("displayFPS", settings.debugLaunch)
            .put("printFPS", settings.debugLaunch)
            .put("vsync", false)
            .put("frameSkip", false)
            .put("subImageFix", true)

        if (settings.soundFontPath.isNotBlank()) {
            json.put("midiSoundFont", settings.soundFontPath)
        }

        output.writeText(json.toString(2))
        return output
    }
}
