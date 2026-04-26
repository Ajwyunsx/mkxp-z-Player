package io.github.mkxpz.engine

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.concurrent.thread

class GameActivity : Activity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepFullscreen()

        statusView = TextView(this).apply {
            text = "Starting mkxp-z..."
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }

        setContentView(
            FrameLayout(this).apply {
                setBackgroundColor(0xFF05080A.toInt())
                addView(
                    statusView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )

        val configPath = intent.getStringExtra(MkxpLauncher.EXTRA_CONFIG_PATH).orEmpty()
        val gamePath = intent.getStringExtra(MkxpLauncher.EXTRA_GAME_PATH).orEmpty()
        val debug = intent.getBooleanExtra(MkxpLauncher.EXTRA_DEBUG, false)

        thread(name = "mkxp-z-runtime") {
            val result = MkxpNative.start(configPath, gamePath, debug)
            runOnUiThread {
                statusView.text = buildString {
                    append("mkxp-z runtime returned ")
                    append(result.exitCode)
                    append("\n\n")
                    append(result.message)
                    append("\n\nConfig:\n")
                    append(configPath)
                    append("\n\nGame:\n")
                    append(gamePath)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) keepFullscreen()
    }

    private fun keepFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }
}
