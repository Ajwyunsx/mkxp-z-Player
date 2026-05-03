package io.github.mkxpz.rpgplayer.domain

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

object BundledInnoExtractor {
    private const val EXECUTABLE_NAME = "libinnoextract_cli_exec.so"
    private const val MAX_OUTPUT_CHARS = 4096

    fun extract(context: Context, installer: File, destination: File): Result<Unit> = runCatching {
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
            ?: throw GameValidationException("无法定位内置 innoextract 目录")
        val executable = File(nativeLibraryDir, EXECUTABLE_NAME)
        if (!executable.isFile) {
            throw GameValidationException("没有找到内置 innoextract：${executable.absolutePath}")
        }

        destination.deleteRecursively()
        destination.mkdirs()

        val output = StringBuilder()
        val process = ProcessBuilder(
            executable.absolutePath,
            "-d",
            destination.absolutePath,
            installer.absolutePath,
        )
            .redirectErrorStream(true)
            .apply {
                val existingPath = environment()["LD_LIBRARY_PATH"].orEmpty()
                environment()["LD_LIBRARY_PATH"] = if (existingPath.isBlank()) {
                    nativeLibraryDir
                } else {
                    "$nativeLibraryDir:$existingPath"
                }
            }
            .start()

        val readerThread = Thread {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (output.length < MAX_OUTPUT_CHARS) {
                        output.appendLine(line)
                    }
                }
            }
        }
        readerThread.isDaemon = true
        readerThread.start()

        if (!process.waitFor(15, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw GameValidationException("内置 innoextract 解包超时")
        }
        readerThread.join(1000)

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val details = output.toString().trim()
            throw GameValidationException(
                if (details.isBlank()) {
                    "内置 innoextract 解包失败：exit=$exitCode"
                } else {
                    "内置 innoextract 解包失败：exit=$exitCode；$details"
                },
            )
        }
    }
}
