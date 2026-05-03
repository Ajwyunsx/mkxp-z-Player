package io.github.mkxpz.rpgplayer.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.android.apksig.ApkSigner
import io.github.mkxpz.rpgplayer.data.GameEntry
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StandaloneApkOptions(
    val appName: String,
    val packageName: String,
    val versionCode: Int,
    val versionName: String,
    val iconUri: String? = null,
)

class StandaloneApkExporter(private val context: Context) {
    suspend fun export(entry: GameEntry, options: StandaloneApkOptions): File = withContext(Dispatchers.IO) {
        validateOptions(options)
        val sourceApk = File(context.applicationInfo.sourceDir)
        if (!sourceApk.isFile) {
            throw GameValidationException("Cannot locate current player APK")
        }
        val gameRoot = File(entry.installedPath)
        if (!gameRoot.isDirectory) {
            throw GameValidationException("Game folder is missing")
        }

        val exportRoot = context.getExternalFilesDir(null) ?: context.filesDir
        val exportDir = File(exportRoot, "exports").apply { mkdirs() }
        val safeTitle = options.appName.safeFileName().ifBlank { "RPGMakerGame" }
        val output = File(exportDir, "$safeTitle-standalone.apk")
        val unsigned = File(context.cacheDir, "$safeTitle-standalone-unsigned.apk")

        unsigned.delete()
        output.delete()
        buildUnsignedApk(sourceApk, gameRoot, entry, options, unsigned)
        signApk(unsigned, output)
        unsigned.delete()
        output
    }

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun buildUnsignedApk(
        sourceApk: File,
        gameRoot: File,
        entry: GameEntry,
        options: StandaloneApkOptions,
        output: File,
    ) {
        output.parentFile?.mkdirs()
        val iconPng = options.iconUri?.let(::loadStandaloneIconPng)
        ZipFile(sourceApk).use { inputApk ->
            ZipOutputStream(FileOutputStream(output).buffered()).use { zip ->
                val names = mutableSetOf<String>()
                inputApk.entries().asSequence().forEach { sourceEntry ->
                    val name = sourceEntry.name
                    if (shouldSkipTemplateEntry(name) || !names.add(name)) {
                        return@forEach
                    }
                    zip.putNextEntry(ZipEntry(name).apply {
                        time = sourceEntry.time.takeIf { it >= 0L } ?: 0L
                    })
                    if (!sourceEntry.isDirectory) {
                        when {
                            name == ANDROID_MANIFEST -> {
                                val patched = inputApk.getInputStream(sourceEntry).use { input ->
                                    patchAndroidManifest(input.readBytes(), options)
                                }
                                zip.write(patched)
                            }
                            iconPng != null && name == STANDALONE_ICON_ENTRY -> {
                                zip.write(iconPng)
                            }
                            else -> {
                                inputApk.getInputStream(sourceEntry).use { input ->
                                    input.copyTo(zip, COPY_BUFFER_SIZE)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }

                writeTextEntry(zip, BUNDLED_MANIFEST, bundledManifest(entry, options).toString())
                addGameDirectory(zip, gameRoot, gameRoot, names)
            }
        }
    }

    private fun signApk(input: File, output: File) {
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "mkxpz-standalone",
            loadPrivateKey(),
            listOf(loadCertificate()),
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(30)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    private fun loadPrivateKey(): PrivateKey {
        val bytes = context.assets.open("standalone_signing/private_key.pk8").use { it.readBytes() }
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    private fun loadCertificate(): X509Certificate {
        return context.assets.open("standalone_signing/certificate.pem").use { input ->
            CertificateFactory.getInstance("X.509").generateCertificate(input) as X509Certificate
        }
    }

    private fun bundledManifest(entry: GameEntry, options: StandaloneApkOptions): JSONObject =
        JSONObject()
            .put("title", options.appName)
            .put("engine", entry.engine.name)
            .put("sourceGameId", entry.id)
            .put("createdAt", System.currentTimeMillis())
            .put("autoLaunch", true)
            .put("standalonePackageName", options.packageName)
            .put("standaloneVersionCode", options.versionCode)
            .put("standaloneVersionName", options.versionName)

    private fun patchAndroidManifest(bytes: ByteArray, options: StandaloneApkOptions): ByteArray {
        val currentVersionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        val replacements = linkedMapOf(
            context.packageName to options.packageName,
            "${context.packageName}.fileprovider" to "${options.packageName}.fileprovider",
            "mkxp-z Player" to options.appName,
        )
        if (currentVersionName.isNotBlank()) {
            replacements[currentVersionName] = options.versionName
        }
        replacements["0.1.0"] = options.versionName

        val withStrings = BinaryXmlStringPool.replaceStrings(bytes, replacements)
        return BinaryXmlStringPool.patchVersionCode(withStrings, options.versionCode)
    }

    private fun loadStandaloneIconPng(iconUri: String): ByteArray? {
        val source = context.contentResolver.openInputStream(android.net.Uri.parse(iconUri))?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null
        val output = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val side = minOf(source.width, source.height).coerceAtLeast(1)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        canvas.drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            RectF(0f, 0f, 432f, 432f),
            paint,
        )
        val bytes = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        source.recycle()
        output.recycle()
        return bytes.toByteArray()
    }

    private fun validateOptions(options: StandaloneApkOptions) {
        if (options.appName.isBlank()) {
            throw GameValidationException("App name cannot be empty")
        }
        if (!PACKAGE_NAME_REGEX.matches(options.packageName)) {
            throw GameValidationException("Invalid package name, for example com.example.game")
        }
        if (options.packageName == context.packageName) {
            throw GameValidationException("Standalone package name cannot be the player package name")
        }
        if (options.versionCode <= 0) {
            throw GameValidationException("versionCode must be greater than 0")
        }
        if (options.versionName.isBlank()) {
            throw GameValidationException("versionName cannot be empty")
        }
    }

/*
    private fun validateOptions(options: StandaloneApkOptions) {
        if (options.appName.isBlank()) {
            throw GameValidationException("应用名不能为空")
        }
        if (!PACKAGE_NAME_REGEX.matches(options.packageName)) {
            throw GameValidationException("包名格式不正确，例如 com.example.game")
        }
        if (options.packageName == context.packageName) {
            throw GameValidationException("直装包名不能和播放器包名相同")
        }
        if (options.versionCode <= 0) {
            throw GameValidationException("versionCode 必须大于 0")
        }
        if (options.versionName.isBlank()) {
            throw GameValidationException("versionName 不能为空")
        }
    }

*/
    private fun addGameDirectory(
        zip: ZipOutputStream,
        root: File,
        current: File,
        names: MutableSet<String>,
    ) {
        current.listFiles()
            .orEmpty()
            .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.US) })
            .forEach { child ->
                val relative = root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/')
                val entryName = "$BUNDLED_FILES_PREFIX/$relative"
                if (!names.add(entryName)) return@forEach

                if (child.isDirectory) {
                    zip.putNextEntry(ZipEntry("$entryName/"))
                    zip.closeEntry()
                    addGameDirectory(zip, root, child, names)
                } else if (child.isFile) {
                    zip.putNextEntry(ZipEntry(entryName).apply { time = child.lastModified().coerceAtLeast(0L) })
                    BufferedInputStream(FileInputStream(child)).use { input ->
                        input.copyTo(zip, COPY_BUFFER_SIZE)
                    }
                    zip.closeEntry()
                }
            }
    }

    private fun writeTextEntry(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name).apply { time = System.currentTimeMillis() })
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun shouldSkipTemplateEntry(name: String): Boolean {
        val normalized = name.replace('\\', '/')
        val lower = normalized.lowercase(Locale.US)
        return lower.startsWith("meta-inf/") ||
            lower.startsWith("assets/bundled_game/") ||
            lower.startsWith("assets/standalone_signing/")
    }

    private companion object {
        private val PACKAGE_NAME_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val COPY_BUFFER_SIZE = 1024 * 1024
        private const val ANDROID_MANIFEST = "AndroidManifest.xml"
        private const val STANDALONE_ICON_ENTRY = "res/drawable/standalone_icon.png"
        private const val BUNDLED_MANIFEST = "assets/bundled_game/manifest.json"
        private const val BUNDLED_FILES_PREFIX = "assets/bundled_game/files"
    }
}
