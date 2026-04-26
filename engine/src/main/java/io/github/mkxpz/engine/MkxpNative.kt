package io.github.mkxpz.engine

import android.util.Log

internal object MkxpNative {
    private const val TAG = "MkxpNative"

    val isBridgeAvailable: Boolean = runCatching {
        System.loadLibrary("mkxpz_bridge")
    }.onFailure {
        Log.e(TAG, "mkxp-z bridge library could not be loaded", it)
    }.isSuccess

    fun start(configPath: String, gamePath: String, debug: Boolean): NativeRunResult {
        if (!isBridgeAvailable) {
            return NativeRunResult(
                exitCode = -100,
                message = "mkxp-z JNI bridge library is missing",
            )
        }

        val exitCode = nativeStart(configPath, gamePath, debug)
        return NativeRunResult(
            exitCode = exitCode,
            message = if (exitCode == 0) "mkxp-z finished" else nativeLastError().ifBlank { "mkxp-z exited with code $exitCode" },
        )
    }

    private external fun nativeStart(configPath: String, gamePath: String, debug: Boolean): Int
    private external fun nativeLastError(): String
}

internal data class NativeRunResult(
    val exitCode: Int,
    val message: String,
)
