package io.github.mkxpz.rpgplayer.domain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import uk.co.armedpineapple.innoextract.service.Configuration
import uk.co.armedpineapple.innoextract.service.ExtractCallback
import uk.co.armedpineapple.innoextract.service.ExtractService

class InnoSetupExtractor(
    context: Context,
    private val label: String = "RTP",
) {
    private val appContext = context.applicationContext

    suspend fun extract(installer: File, destination: File) {
        if (!installer.isFile) {
            throw GameValidationException("没有找到 $label 安装器：${installer.absolutePath}")
        }

        destination.deleteRecursively()
        destination.mkdirs()

        val bundledError = BundledInnoExtractor.extract(appContext, installer, destination).exceptionOrNull()
        if (bundledError == null) {
            return
        }

        destination.deleteRecursively()
        destination.mkdirs()

        val nativeError = NativeInnoExtractor.extract(installer, destination).exceptionOrNull()
        if (nativeError == null) {
            return
        }

        destination.deleteRecursively()
        destination.mkdirs()

        val installerUri = Uri.fromFile(installer)
        suspendCancellableCoroutine { continuation ->
            val completed = AtomicBoolean(false)
            lateinit var connection: ServiceConnection

            fun finish(result: Result<Unit>) {
                if (!completed.compareAndSet(false, true)) return
                runCatching { appContext.unbindService(connection) }
                result
                    .onSuccess { continuation.resume(Unit) }
                    .onFailure { continuation.resumeWithException(it) }
            }

            connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val service = (binder as ExtractService.ServiceBinder).service
                    try {
                        val validation = service.check(installerUri)
                        if (!validation.isValid) {
                            finish(
                                Result.failure(
                                    GameValidationException(
                                        "$label 安装器不是有效的 Inno Setup 文件；内置=${bundledError.message ?: bundledError.javaClass.simpleName}；JNI=${nativeError.message ?: nativeError.javaClass.simpleName}",
                                    ),
                                ),
                            )
                            return
                        }

                        service.extract(
                            toExtract = installerUri,
                            extractDir = destination,
                            callback = object : ExtractCallback {
                                override fun onProgress(value: Long, max: Long, file: String) = Unit

                                override fun onSuccess() {
                                    finish(Result.success(Unit))
                                }

                                override fun onFailure(e: Exception) {
                                    finish(
                                        Result.failure(
                                            GameValidationException(
                                                "$label 安装器解包失败：${e.message ?: e.javaClass.simpleName}；内置=${bundledError.message ?: bundledError.javaClass.simpleName}；JNI=${nativeError.message ?: nativeError.javaClass.simpleName}",
                                            ),
                                        ),
                                    )
                                }
                            },
                            configuration = Configuration(
                                showOngoingNotification = false,
                                showFinalNotification = false,
                            ),
                        )
                    } catch (error: Throwable) {
                        finish(Result.failure(error))
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    finish(Result.failure(GameValidationException("$label 解包服务已断开")))
                }
            }

            val bound = appContext.bindService(
                Intent(appContext, ExtractService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
            if (!bound) {
                finish(Result.failure(GameValidationException("无法启动 $label 解包服务")))
            }

            continuation.invokeOnCancellation {
                if (completed.compareAndSet(false, true)) {
                    runCatching { appContext.unbindService(connection) }
                }
            }
        }
    }
}
