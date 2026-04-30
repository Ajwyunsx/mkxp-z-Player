package io.github.mkxpz.engine

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.hatkid.mkxpz.gamepad.Gamepad
import com.hatkid.mkxpz.gamepad.GamepadConfig
import org.json.JSONArray
import java.io.File

class WebGameActivity : Activity() {
    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private val gamepad = Gamepad()
    private var gamepadConfig = GamepadConfig()
    private var virtualGamepad = true
    private var gamepadAttached = false
    private var gamepadInvisible = false
    private var playerMenuDialog: AlertDialog? = null
    private var debugLaunch = false
    private var html5ThreeDMode = false
    private var html5ThreeDPlugins = emptyList<String>()
    private var loadedIndexFile: File? = null
    private var playerOptionsButton: TextView? = null
    private var gamepadLayoutEditing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepFullscreen()

        virtualGamepad = intent.getBooleanExtra(MkxpLauncher.EXTRA_VIRTUAL_GAMEPAD, true)
        debugLaunch = intent.getBooleanExtra(MkxpLauncher.EXTRA_DEBUG, false)
        html5ThreeDMode = intent.getBooleanExtra(MkxpLauncher.EXTRA_HTML5_3D_MODE, false)
        html5ThreeDPlugins = intent.getStringArrayListExtra(MkxpLauncher.EXTRA_HTML5_3D_PLUGINS).orEmpty()
        gamepadConfig = readGamepadConfigFromIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { WebView.setDataDirectorySuffix("game") }
        }
        WebView.setWebContentsDebuggingEnabled(debugLaunch)
        CookieManager.getInstance().setAcceptCookie(true)

        webView = WebView(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.loadsImagesAutomatically = true
            settings.blockNetworkImage = false
            runCatching { settings.blockNetworkLoads = false }
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            if (html5ThreeDMode) {
                configureThreeDWebView()
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    if (html5ThreeDMode) installThreeDRuntimeHooks(view)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    if (debugLaunch) {
                        android.util.Log.d("WebGameActivity", consoleMessage.message())
                    }
                    return true
                }
            }
        }

        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        setContentView(rootLayout)

        if (virtualGamepad) installGamepad()
        installPlayerOptionsButton()

        val index = resolveIndexFile(intent.getStringExtra(MkxpLauncher.EXTRA_GAME_PATH).orEmpty())
        if (index == null) {
            showFatalError("没有找到 MV/MZ 的 index.html")
        } else {
            loadIndex(index)
            webView.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && event.repeatCount == 0) {
                showPlayerOptions()
            }
            return true
        }

        if (virtualGamepad && event.keyCode !in systemKeyCodes && !gamepadInvisible) {
            gamepad.hideView()
            gamepadInvisible = true
        }
        if (virtualGamepad && gamepad.processGamepadEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (virtualGamepad && gamepadInvisible) {
            gamepad.showView()
            gamepadInvisible = false
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (virtualGamepad && gamepad.processDPadEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }

    override fun onBackPressed() {
        showPlayerOptions()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        resumeHtml5Runtime()
        if (html5ThreeDMode) {
            webView.post {
                installThreeDRuntimeHooks(webView)
                webView.invalidate()
            }
        }
        playerOptionsButton?.bringToFront()
        keepFullscreen()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        playerMenuDialog?.dismiss()
        rootLayout.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) keepFullscreen()
    }

    private fun WebView.configureThreeDWebView() {
        keepScreenOn = true
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.textZoom = 100
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.offscreenPreRaster = true
    }

    private fun loadIndex(index: File) {
        loadedIndexFile = index
        val indexUrl = index.toURI().toString()
        if (!html5ThreeDMode) {
            webView.loadUrl(indexUrl)
            return
        }

        val html = runCatching { index.readText() }.getOrNull()
        if (html.isNullOrBlank()) {
            webView.loadUrl(indexUrl)
            return
        }

        val baseUrl = index.parentFile?.toURI()?.toString() ?: indexUrl
        webView.loadDataWithBaseURL(
            baseUrl,
            injectThreeDCompatibilityScript(html),
            "text/html",
            "UTF-8",
            indexUrl,
        )
    }

    private fun reloadGame() {
        loadedIndexFile?.let(::loadIndex) ?: webView.reload()
    }

    private fun injectThreeDCompatibilityScript(html: String): String {
        val scriptTag = "<script>${threeDCompatibilityScript()}</script>"
        return insertBefore(html, Regex("</head>", RegexOption.IGNORE_CASE), scriptTag)
            ?: insertBefore(html, Regex("<script\\b", RegexOption.IGNORE_CASE), scriptTag)
            ?: "$scriptTag\n$html"
    }

    private fun insertBefore(html: String, regex: Regex, insertion: String): String? {
        val match = regex.find(html) ?: return null
        return html.substring(0, match.range.first) +
            insertion +
            "\n" +
            html.substring(match.range.first)
    }

    private fun installThreeDRuntimeHooks(view: WebView) {
        view.evaluateJavascript(threeDCompatibilityScript(), null)
    }

    private fun resumeHtml5Runtime() {
        webView.post {
            webView.evaluateJavascript(
                """
                    (function () {
                      try {
                        if (window.WebAudio && WebAudio._context && WebAudio._context.resume) {
                          WebAudio._context.resume();
                        }
                        if (window.Graphics && Graphics._canvas) {
                          Graphics._canvas.style.visibility = 'hidden';
                          void Graphics._canvas.offsetHeight;
                          Graphics._canvas.style.visibility = 'visible';
                        }
                      } catch (error) {}
                    })();
                """.trimIndent(),
                null,
            )
        }
    }

    private fun threeDCompatibilityScript(): String {
        val pluginNames = JSONArray(html5ThreeDPlugins).toString()
        return """
            (function () {
              if (window.__mkxpz3dCompatInstalled) return;
              window.__mkxpz3dCompatInstalled = true;
              window.__MKXPZ_3D_COMPAT__ = true;
              window.__MKXPZ_3D_PLUGINS__ = $pluginNames;

              var originalGetContext = HTMLCanvasElement.prototype.getContext;
              HTMLCanvasElement.prototype.getContext = function (type, attrs) {
                var kind = String(type || '').toLowerCase();
                if (kind === 'webgl' || kind === 'experimental-webgl' || kind === 'webgl2') {
                  attrs = attrs || {};
                  if (attrs.alpha === undefined) attrs.alpha = false;
                  if (attrs.antialias === undefined) attrs.antialias = true;
                  if (attrs.depth === undefined) attrs.depth = true;
                  if (attrs.stencil === undefined) attrs.stencil = true;
                  if (attrs.preserveDrawingBuffer === undefined) attrs.preserveDrawingBuffer = false;
                  if (attrs.powerPreference === undefined) attrs.powerPreference = 'high-performance';
                }
                return originalGetContext.call(this, type, attrs);
              };

              window.addEventListener('webglcontextlost', function (event) {
                event.preventDefault();
              }, false);
              window.addEventListener('webglcontextrestored', function () {
                try {
                  if (window.Graphics && Graphics._renderer && Graphics._renderer.resize) {
                    Graphics._renderer.resize(Graphics._width, Graphics._height);
                  }
                  if (window.SceneManager && SceneManager._scene && SceneManager._scene.start) {
                    SceneManager._scene.start();
                  }
                } catch (error) {}
              }, false);
              window.addEventListener('pageshow', function () {
                try {
                  if (window.WebAudio && WebAudio._context && WebAudio._context.resume) {
                    WebAudio._context.resume();
                  }
                  if (window.Graphics && Graphics._canvas) {
                    Graphics._canvas.style.visibility = 'hidden';
                    void Graphics._canvas.offsetHeight;
                    Graphics._canvas.style.visibility = 'visible';
                  }
                } catch (error) {}
              }, false);
              if (document.documentElement) document.documentElement.style.background = '#000';
              if (document.body) {
                document.body.style.background = '#000';
                document.body.style.overflow = 'hidden';
                document.body.style.margin = '0';
              }
            })();
        """.trimIndent()
    }

    private fun installGamepad() {
        if (gamepadAttached) return

        gamepad.init(gamepadConfig, false)
        gamepad.setOnKeyDownListener { key -> sendKey(KeyEvent.ACTION_DOWN, key) }
        gamepad.setOnKeyUpListener { key -> sendKey(KeyEvent.ACTION_UP, key) }
        gamepad.attachTo(this, rootLayout)
        gamepad.setLayoutEditMode(gamepadLayoutEditing)
        gamepadAttached = true
        gamepadInvisible = false
        playerOptionsButton?.bringToFront()
    }

    private fun installPlayerOptionsButton() {
        if (playerOptionsButton != null) return

        playerOptionsButton = TextView(this).apply {
            text = "播放器选项"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(28, 10, 28, 10)
            isFocusable = false
            isClickable = true
            background = roundedOverlayBackground()
            elevation = 10f
            setOnClickListener { showPlayerOptions() }
        }

        rootLayout.addView(
            playerOptionsButton,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin = 18
            },
        )
        playerOptionsButton?.bringToFront()
    }

    private fun sendKey(action: Int, keyCode: Int) {
        val event = KeyEvent(action, keyCode)
        webView.dispatchKeyEvent(event)
    }

    private fun showPlayerOptions() {
        if (isFinishing || playerMenuDialog?.isShowing == true) return

        val gamepadAction = if (virtualGamepad) "隐藏虚拟按键" else "显示虚拟按键"
        val movementAction = if (gamepad.isJoystickMode) "切换为十字键" else "切换为摇杆"
        val layoutEditAction = if (gamepadLayoutEditing) "结束按键布局编辑" else "编辑按键布局"
        val items = arrayOf("继续游戏", gamepadAction, movementAction, "虚拟按键设置", layoutEditAction, "保存按键布局", "重新加载游戏", "退出到启动器")
        playerMenuDialog = AlertDialog.Builder(this)
            .setTitle("播放器选项")
            .setItems(items) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    1 -> setVirtualGamepadEnabled(!virtualGamepad)
                    2 -> toggleMovementMode()
                    3 -> showGamepadSettings()
                    4 -> setGamepadLayoutEditing(!gamepadLayoutEditing)
                    5 -> saveGamepadLayout()
                    6 -> reloadGame()
                    7 -> finish()
                }
            }
            .setOnDismissListener {
                playerMenuDialog = null
                keepFullscreen()
            }
            .show()
    }

    private fun showGamepadSettings() {
        val working = gamepadConfig.copyConfig()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 18, 32, 0)
        }

        val opacityLabel = TextView(this)
        val opacitySeek = SeekBar(this).apply {
            max = 95
            progress = (working.opacity ?: 30).coerceIn(5, 100) - 5
            setOnSeekBarChangeListener(simpleSeekListener {
                working.opacity = progress + 5
                opacityLabel.text = "透明度：${working.opacity}%"
            })
        }
        opacityLabel.text = "透明度：${working.opacity}%"
        content.addView(opacityLabel)
        content.addView(opacitySeek)

        val scaleLabel = TextView(this)
        val scaleSeek = SeekBar(this).apply {
            max = 100
            progress = (working.scale ?: 100).coerceIn(60, 160) - 60
            setOnSeekBarChangeListener(simpleSeekListener {
                working.scale = progress + 60
                scaleLabel.text = "大小：${working.scale}%"
            })
        }
        scaleLabel.text = "大小：${working.scale}%"
        content.addView(scaleLabel)
        content.addView(scaleSeek)

        val diagonalSwitch = Switch(this).apply {
            text = "方向键允许斜向移动"
            isChecked = working.diagonalMovement ?: false
            setOnCheckedChangeListener { _, checked -> working.diagonalMovement = checked }
        }
        content.addView(diagonalSwitch)

        addKeyPicker(content, "A", working.keycodeA) { working.keycodeA = it }
        addKeyPicker(content, "B", working.keycodeB) { working.keycodeB = it }
        addKeyPicker(content, "C", working.keycodeC) { working.keycodeC = it }
        addKeyPicker(content, "X", working.keycodeX) { working.keycodeX = it }
        addKeyPicker(content, "Y", working.keycodeY) { working.keycodeY = it }
        addKeyPicker(content, "Z", working.keycodeZ) { working.keycodeZ = it }
        addKeyPicker(content, "L", working.keycodeL) { working.keycodeL = it }
        addKeyPicker(content, "R", working.keycodeR) { working.keycodeR = it }
        addKeyPicker(content, "CTRL", working.keycodeCTRL) { working.keycodeCTRL = it }
        addKeyPicker(content, "ALT", working.keycodeALT) { working.keycodeALT = it }
        addKeyPicker(content, "SHIFT", working.keycodeSHIFT) { working.keycodeSHIFT = it }

        content.addView(TextView(this).apply {
            text = "添加自定义按键"
            gravity = Gravity.CENTER
            setPadding(16, 18, 16, 18)
            setOnClickListener { showAddCustomButtonDialog() }
        })

        AlertDialog.Builder(this)
            .setTitle("虚拟按键设置")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("应用") { _, _ ->
                gamepadConfig.applyFrom(working)
                working.layout = gamepad.exportLayout()
                gamepadConfig = working
                MkxpLauncher.saveGamepadLayout(this, working.layout)
                reinstallGamepad()
                keepFullscreen()
            }
            .setNegativeButton("取消") { _, _ -> keepFullscreen() }
            .show()
    }

    private fun addKeyPicker(
        container: LinearLayout,
        title: String,
        initialKeyCode: Int,
        onSelected: (Int) -> Unit,
    ) {
        var selectedKeyCode = initialKeyCode
        val button = TextView(this).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            text = keyLabel(selectedKeyCode)
            setPadding(16, 12, 16, 12)
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 4, 0, 4)
            addView(TextView(this@WebGameActivity).apply {
                text = title
                textSize = 16f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            setOnClickListener {
                showKeyPicker(title, selectedKeyCode) { keyCode ->
                    selectedKeyCode = keyCode
                    button.text = keyLabel(keyCode)
                    onSelected(keyCode)
                }
            }
        }
        container.addView(row)
    }

    private fun showKeyPicker(title: String, selectedKeyCode: Int, onSelected: (Int) -> Unit) {
        val labels = commonKeyCodes.map(::keyLabel).toTypedArray()
        val selectedIndex = commonKeyCodes.indexOf(selectedKeyCode)
        AlertDialog.Builder(this)
            .setTitle("$title 映射")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                onSelected(commonKeyCodes[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleMovementMode() {
        if (!ensureGamepadReady()) return

        val joystick = gamepad.toggleMovementMode()
        gamepadConfig.layout = gamepad.exportLayout()
        Toast.makeText(this, if (joystick) "已切换为摇杆" else "已切换为十字键", Toast.LENGTH_SHORT).show()
        keepFullscreen()
    }

    private fun showAddCustomButtonDialog() {
        if (!ensureGamepadReady()) return

        val labels = commonKeyCodes.map(::keyLabel)
        var sizeDp = 46
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@WebGameActivity,
                android.R.layout.simple_spinner_item,
                labels,
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        val sizeLabel = TextView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 0)
            addView(spinner)
            addView(sizeLabel)
            addView(SeekBar(this@WebGameActivity).apply {
                max = 68
                progress = sizeDp - 28
                setOnSeekBarChangeListener(simpleSeekListener {
                    sizeDp = progress + 28
                    sizeLabel.text = "按键大小：${sizeDp}dp"
                })
            })
        }
        sizeLabel.text = "按键大小：${sizeDp}dp"

        AlertDialog.Builder(this)
            .setTitle("添加自定义按键")
            .setView(content)
            .setPositiveButton("添加") { _, _ ->
                val keyCode = commonKeyCodes[spinner.selectedItemPosition.coerceAtLeast(0)]
                gamepad.addCustomButton(keyLabel(keyCode), keyCode, sizeDp)
                gamepadLayoutEditing = true
                gamepad.setLayoutEditMode(true)
                gamepadConfig.layout = gamepad.exportLayout()
                Toast.makeText(this, "已添加，拖动到位置后请保存布局", Toast.LENGTH_SHORT).show()
                keepFullscreen()
            }
            .setNegativeButton("取消") { _, _ -> keepFullscreen() }
            .show()
    }

    private fun ensureGamepadReady(): Boolean {
        if (!virtualGamepad) {
            Toast.makeText(this, "虚拟按键未启用", Toast.LENGTH_SHORT).show()
            return false
        }
        installGamepad()
        if (!gamepadAttached) return false
        if (gamepadInvisible) {
            gamepad.showView()
            gamepadInvisible = false
        }
        return true
    }

    private fun setVirtualGamepadEnabled(enabled: Boolean) {
        virtualGamepad = enabled
        if (enabled) {
            installGamepad()
            gamepad.showView()
        } else {
            gamepadLayoutEditing = false
            gamepad.setLayoutEditMode(false)
            gamepad.detach()
            gamepadAttached = false
            gamepadInvisible = true
        }
        keepFullscreen()
    }

    private fun reinstallGamepad() {
        val wasEnabled = virtualGamepad
        gamepad.detach()
        gamepadAttached = false
        if (wasEnabled) installGamepad()
        playerOptionsButton?.bringToFront()
    }

    private fun setGamepadLayoutEditing(enabled: Boolean) {
        if (enabled && !virtualGamepad) {
            setVirtualGamepadEnabled(true)
        }
        gamepadLayoutEditing = enabled
        gamepad.setLayoutEditMode(enabled)
        Toast.makeText(
            this,
            if (enabled) "拖动虚拟按键后点击保存按键布局" else "已结束布局编辑",
            Toast.LENGTH_SHORT,
        ).show()
        keepFullscreen()
    }

    private fun saveGamepadLayout() {
        val layout = gamepad.exportLayout()
        if (layout.isBlank()) {
            Toast.makeText(this, "没有可保存的虚拟按键布局", Toast.LENGTH_SHORT).show()
            return
        }
        MkxpLauncher.saveGamepadLayout(this, layout)
        gamepadConfig.layout = layout
        gamepadLayoutEditing = false
        gamepad.setLayoutEditMode(false)
        Toast.makeText(this, "虚拟按键布局已保存", Toast.LENGTH_SHORT).show()
        keepFullscreen()
    }

    private fun roundedOverlayBackground(): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(0x99000000.toInt())
            setStroke(1, 0x33FFFFFF)
        }

    private fun showFatalError(message: String) {
        rootLayout.addView(
            TextView(this).apply {
                text = message
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF05080A.toInt())
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun resolveIndexFile(gamePath: String): File? {
        val root = File(gamePath)
        val direct = root.resolve("index.html")
        if (direct.isFile && root.hasHtml5Core()) return direct
        val wwwRoot = root.resolve("www")
        val www = wwwRoot.resolve("index.html")
        if (www.isFile && wwwRoot.hasHtml5Core()) return www
        return null
    }

    private fun File.hasHtml5Core(): Boolean {
        val jsDir = resolve("js")
        return jsDir.resolve("rpg_core.js").isFile ||
            jsDir.resolve("rpg_managers.js").isFile ||
            jsDir.resolve("rmmz_core.js").isFile ||
            jsDir.resolve("rmmz_managers.js").isFile
    }

    private fun readGamepadConfigFromIntent(): GamepadConfig = GamepadConfig().apply {
        opacity = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_OPACITY, 30).coerceIn(5, 100)
        scale = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_SCALE, 100).coerceIn(60, 160)
        diagonalMovement = intent.getBooleanExtra(MkxpLauncher.EXTRA_GAMEPAD_DIAGONAL_MOVEMENT, false)
        keycodeA = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_A, KeyEvent.KEYCODE_Z)
        keycodeB = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_B, KeyEvent.KEYCODE_X)
        keycodeC = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_C, KeyEvent.KEYCODE_C)
        keycodeX = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_X, KeyEvent.KEYCODE_A)
        keycodeY = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_Y, KeyEvent.KEYCODE_S)
        keycodeZ = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_Z, KeyEvent.KEYCODE_D)
        keycodeL = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_L, KeyEvent.KEYCODE_Q)
        keycodeR = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_R, KeyEvent.KEYCODE_W)
        keycodeCTRL = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_CTRL, KeyEvent.KEYCODE_CTRL_LEFT)
        keycodeALT = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_ALT, KeyEvent.KEYCODE_ALT_LEFT)
        keycodeSHIFT = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_SHIFT, KeyEvent.KEYCODE_SHIFT_LEFT)
        layout = intent.getStringExtra(MkxpLauncher.EXTRA_GAMEPAD_LAYOUT).orEmpty()
    }

    private fun GamepadConfig.copyConfig(): GamepadConfig = GamepadConfig().also {
        it.opacity = opacity
        it.scale = scale
        it.diagonalMovement = diagonalMovement
        it.joystickMode = joystickMode
        it.keycodeA = keycodeA
        it.keycodeB = keycodeB
        it.keycodeC = keycodeC
        it.keycodeX = keycodeX
        it.keycodeY = keycodeY
        it.keycodeZ = keycodeZ
        it.keycodeL = keycodeL
        it.keycodeR = keycodeR
        it.keycodeCTRL = keycodeCTRL
        it.keycodeALT = keycodeALT
        it.keycodeSHIFT = keycodeSHIFT
        it.layout = layout
    }

    private fun GamepadConfig.applyFrom(source: GamepadConfig) {
        opacity = source.opacity
        scale = source.scale
        diagonalMovement = source.diagonalMovement
        joystickMode = source.joystickMode
        keycodeA = source.keycodeA
        keycodeB = source.keycodeB
        keycodeC = source.keycodeC
        keycodeX = source.keycodeX
        keycodeY = source.keycodeY
        keycodeZ = source.keycodeZ
        keycodeL = source.keycodeL
        keycodeR = source.keycodeR
        keycodeCTRL = source.keycodeCTRL
        keycodeALT = source.keycodeALT
        keycodeSHIFT = source.keycodeSHIFT
    }

    private fun simpleSeekListener(onProgressChanged: SeekBar.() -> Unit): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) seekBar.onProgressChanged()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        }

    private fun keyLabel(keyCode: Int): String =
        KeyEvent.keyCodeToString(keyCode)
            .removePrefix("KEYCODE_")
            .removeSuffix("_LEFT")
            .removeSuffix("_RIGHT")

    private fun keepFullscreen() {
        val decorView = window.decorView ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { window.setDecorFitsSystemWindows(false) }
            decorView.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    companion object {
        private val systemKeyCodes = setOf(
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_HEADSETHOOK,
        )

        private val commonKeyCodes = listOf(
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_TAB,
        )
    }
}
