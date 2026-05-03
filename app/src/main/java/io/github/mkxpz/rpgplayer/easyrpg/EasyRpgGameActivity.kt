package io.github.mkxpz.rpgplayer.easyrpg

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EasyRpgGameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        val title = intent.getStringExtra(EXTRA_GAME_TITLE).orEmpty().ifBlank { "RPG Maker 2000/2003" }
        val gamePath = intent.getStringExtra(EXTRA_GAME_PATH).orEmpty()
        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank {
            "当前构建还没有打包 EasyRPG 原生核心，因此不会把 2000/2003 游戏错误交给 mkxp-z 启动。"
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 32, 40, 32)
            setBackgroundColor(Color.BLACK)
        }

        root.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = reason
            setTextColor(0xffd0d0d0.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 12)
        })
        if (gamePath.isNotBlank()) {
            root.addView(TextView(this).apply {
                text = gamePath
                setTextColor(0xff9e9e9e.toInt())
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })
        }
        externalEasyRpgPackage()?.let { packageName ->
            root.addView(Button(this).apply {
                text = "打开 EasyRPG Player"
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
                }
            })
        }
        root.addView(Button(this).apply {
            text = "返回播放器"
            setOnClickListener { finish() }
        })

        setContentView(root)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
        }
    }

    private fun enterImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun externalEasyRpgPackage(): String? {
        return EASYRPG_PACKAGES.firstOrNull { packageName ->
            packageManager.getLaunchIntentForPackage(packageName) != null ||
                runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess
        }
    }

    companion object {
        const val EXTRA_GAME_ID = "io.github.mkxpz.rpgplayer.easyrpg.GAME_ID"
        const val EXTRA_GAME_TITLE = "io.github.mkxpz.rpgplayer.easyrpg.GAME_TITLE"
        const val EXTRA_GAME_PATH = "io.github.mkxpz.rpgplayer.easyrpg.GAME_PATH"
        const val EXTRA_REASON = "io.github.mkxpz.rpgplayer.easyrpg.REASON"

        private val EASYRPG_PACKAGES = listOf(
            "org.easyrpg.player",
            "org.easyrpg.player.debug",
        )

        fun launch(context: Context, gameId: String, title: String, gamePath: String, reason: String) {
            val intent = Intent(context, EasyRpgGameActivity::class.java)
                .putExtra(EXTRA_GAME_ID, gameId)
                .putExtra(EXTRA_GAME_TITLE, title)
                .putExtra(EXTRA_GAME_PATH, gamePath)
                .putExtra(EXTRA_REASON, reason)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
