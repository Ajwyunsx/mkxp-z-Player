package io.github.mkxpz.rpgplayer.domain

import java.io.File

object NativeInnoExtractor {
    private val loadError: Throwable? = runCatching {
        System.loadLibrary("inno_cli_bridge")
    }.exceptionOrNull()

    fun extract(installer: File, destination: File): Result<Unit> {
        loadError?.let { error ->
            return Result.failure(error)
        }
        val exitCode = extract(installer.absolutePath, destination.absolutePath)
        return if (exitCode == 0) {
            Result.success(Unit)
        } else {
            Result.failure(GameValidationException("innoextract CLI 解包失败：exit=$exitCode"))
        }
    }

    private external fun extract(installerPath: String, destinationPath: String): Int
}
