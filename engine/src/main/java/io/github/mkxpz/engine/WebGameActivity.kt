package io.github.mkxpz.engine

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.EditText
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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

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
    private var html5RootDir: File? = null
    private var preferOggAudio = false
    private var playerOptionsButton: TextView? = null
    private var gamepadLayoutEditing = false
    private var modifierConfigPath = ""
    private var modifierConfig = ModifierConfig()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepFullscreen()

        virtualGamepad = intent.getBooleanExtra(MkxpLauncher.EXTRA_VIRTUAL_GAMEPAD, true)
        debugLaunch = intent.getBooleanExtra(MkxpLauncher.EXTRA_DEBUG, false)
        html5ThreeDMode = intent.getBooleanExtra(MkxpLauncher.EXTRA_HTML5_3D_MODE, false)
        html5ThreeDPlugins = intent.getStringArrayListExtra(MkxpLauncher.EXTRA_HTML5_3D_PLUGINS).orEmpty()
        modifierConfigPath = intent.getStringExtra(MkxpLauncher.EXTRA_MODIFIER_CONFIG_PATH).orEmpty()
        modifierConfig = ModifierConfig.load(this, intent.getStringExtra(MkxpLauncher.EXTRA_GAME_ID).orEmpty(), modifierConfigPath)
        if (modifierConfigPath.isNotBlank()) {
            modifierConfig.writeToFile(File(modifierConfigPath))
        }
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
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    return interceptLocalGameResource(request)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (html5ThreeDMode) installThreeDRuntimeHooks(view)
                    installModifierRuntimeHooks(view)
                    applyHtml5Modifier()
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

        configureGamepadInput()
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
        if (gamepad.processGamepadEvent(event)) return true
        if (gamepad.isPhysicalGamepadBackEvent(event)) return true

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
        if (gamepad.processDPadEvent(event)) return true
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
                installModifierRuntimeHooks(webView)
                applyHtml5Modifier()
                webView.invalidate()
            }
        } else {
            webView.post {
                installModifierRuntimeHooks(webView)
                applyHtml5Modifier()
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
        html5RootDir = index.parentFile
        preferOggAudio = shouldPreferOggAudio(index.parentFile)
        val indexUrl = index.toURI().toString()
        webView.loadUrl(indexUrl)
    }

    private fun reloadGame() {
        loadedIndexFile?.let(::loadIndex) ?: webView.reload()
    }

    private fun injectHtml5CompatibilityScripts(html: String): String {
        var patched = html
        patched = injectModifierCompatibilityScript(patched)
        if (preferOggAudio) {
            patched = injectAudioCompatibilityScript(patched)
        }
        if (html5ThreeDMode) {
            patched = injectThreeDCompatibilityScript(patched)
        }
        return patched
    }

    private fun injectModifierCompatibilityScript(html: String): String {
        val scriptTag = "<script>${modifierCompatibilityScript()}</script>"
        return insertBefore(html, Regex("</head>", RegexOption.IGNORE_CASE), scriptTag)
            ?: insertBefore(html, Regex("<script\\b", RegexOption.IGNORE_CASE), scriptTag)
            ?: "$scriptTag\n$html"
    }

    private fun injectAudioCompatibilityScript(html: String): String {
        val scriptTag = "<script>${audioCompatibilityScript()}</script>"
        return insertAfter(
            html,
            Regex(
                "<script\\b[^>]*src=[\"'][^\"']*(?:rpg_managers|rmmz_managers)\\.js[^\"']*[\"'][^>]*>\\s*</script>",
                RegexOption.IGNORE_CASE,
            ),
            scriptTag,
        )
            ?: insertBefore(
                html,
                Regex("<script\\b[^>]*src=[\"'][^\"']*main\\.js[^\"']*[\"']", RegexOption.IGNORE_CASE),
                scriptTag,
            )
            ?: insertBefore(html, Regex("</head>", RegexOption.IGNORE_CASE), scriptTag)
            ?: "$scriptTag\n$html"
    }

    private fun injectThreeDCompatibilityScript(html: String): String {
        val scriptTag = "<script>${threeDCompatibilityScript()}</script>"
        return insertBefore(html, Regex("</head>", RegexOption.IGNORE_CASE), scriptTag)
            ?: insertBefore(html, Regex("<script\\b", RegexOption.IGNORE_CASE), scriptTag)
            ?: "$scriptTag\n$html"
    }

    private fun insertAfter(html: String, regex: Regex, insertion: String): String? {
        val match = regex.find(html) ?: return null
        val insertAt = match.range.last + 1
        return html.substring(0, insertAt) +
            "\n" +
            insertion +
            "\n" +
            html.substring(insertAt)
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

    private fun installModifierRuntimeHooks(view: WebView) {
        view.evaluateJavascript(modifierCompatibilityScript(), null)
    }

    private fun applyHtml5Modifier() {
        val json = modifierConfig.toJsonString()
        webView.evaluateJavascript(
            """
                (function () {
                  window.__MKXPZ_MODIFIER_INITIAL__ = $json;
                  if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.setConfig) {
                    window.MKXPZPlayerModifier.setConfig($json);
                  }
                })();
            """.trimIndent(),
            null,
        )
    }

    private fun audioCompatibilityScript(): String {
        return """
            (function () {
              if (window.__mkxpzAudioCompatInstalled) return;
              window.__mkxpzAudioCompatInstalled = true;
              function patchAudioManager() {
                if (!window.AudioManager || window.AudioManager.__mkxpzAudioCompat) return false;
                window.AudioManager.__mkxpzAudioCompat = true;
                window.AudioManager.audioFileExt = function () {
                  return '.ogg';
                };
                return true;
              }
              if (!patchAudioManager()) {
                var tries = 0;
                var timer = setInterval(function () {
                  tries += 1;
                  if (patchAudioManager() || tries > 180) clearInterval(timer);
                }, 8);
              }
            })();
        """.trimIndent()
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

    private fun modifierCompatibilityScript(): String {
        val initial = modifierConfig.toJsonString()
        return """
            (function () {
              var initial = $initial;
              if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.setConfig) {
                window.MKXPZPlayerModifier.setConfig(window.__MKXPZ_MODIFIER_INITIAL__ || initial);
                return;
              }
              var modifier = {
                config: window.__MKXPZ_MODIFIER_INITIAL__ || initial || {},
                lastApplyNonce: null,
                patched: false,
                setConfig: function (next) {
                  this.config = next || {};
                  this.patchRuntime();
                  this.apply(true);
                },
                enabled: function (name) {
                  return !!this.config[name];
                },
                active: function () {
                  return !!(this.enabled('infiniteHp') || this.enabled('infiniteMp') || this.enabled('instantKill') ||
                    this.enabled('noClip') || this.enabled('allItems') || this.enabled('goldEnabled') || this.enabled('expEnabled'));
                },
                number: function (name, fallback) {
                  var value = Number(this.config[name]);
                  return isFinite(value) && value >= 0 ? Math.floor(value) : fallback;
                },
                patchRuntime: function () {
                  if (window.Game_Enemy && Game_Enemy.prototype && Game_Enemy.prototype.setHp && !Game_Enemy.prototype.__mkxpzInstantKillPatched) {
                    Game_Enemy.prototype.__mkxpzInstantKillPatched = true;
                    var originalEnemySetHp = Game_Enemy.prototype.setHp;
                    Game_Enemy.prototype.setHp = function (hp) {
                      try {
                        if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.enabled('instantKill') && hp < this.hp) {
                          hp = 0;
                        }
                      } catch (e) {}
                      return originalEnemySetHp.call(this, hp);
                    };
                  }
                  if (this.patched || !window.Game_Player || !Game_Player.prototype) return;
                  this.patched = true;
                  var originalCanPass = Game_Player.prototype.canPass;
                  if (originalCanPass) {
                    Game_Player.prototype.canPass = function () {
                      if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.enabled('noClip')) return true;
                      return originalCanPass.apply(this, arguments);
                    };
                  }
                  var originalIsMapPassable = Game_Player.prototype.isMapPassable;
                  if (originalIsMapPassable) {
                    Game_Player.prototype.isMapPassable = function () {
                      if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.enabled('noClip')) return true;
                      return originalIsMapPassable.apply(this, arguments);
                    };
                  }
                  var originalCollision = Game_Player.prototype.isCollidedWithCharacters;
                  if (originalCollision) {
                    Game_Player.prototype.isCollidedWithCharacters = function () {
                      if (window.MKXPZPlayerModifier && window.MKXPZPlayerModifier.enabled('noClip')) return false;
                      return originalCollision.apply(this, arguments);
                    };
                  }
                },
                actors: function () {
                  try {
                    if (window.${'$'}gameParty && ${'$'}gameParty.members) return ${'$'}gameParty.members().filter(Boolean);
                  } catch (e) {}
                  return [];
                },
                applyActor: function (actor) {
                  try {
                    if (this.enabled('infiniteHp') && actor.mhp && actor.setHp) actor.setHp(actor.mhp);
                    if (this.enabled('infiniteMp') && actor.mmp && actor.setMp) actor.setMp(actor.mmp);
                  } catch (e) {}
                },
                applyGold: function () {
                  try {
                    if (!this.enabled('goldEnabled') || !window.${'$'}gameParty || !${'$'}gameParty.gold || !${'$'}gameParty.gainGold) return;
                    var target = this.number('gold', 999999);
                    ${'$'}gameParty.gainGold(target - ${'$'}gameParty.gold());
                  } catch (e) {}
                },
                applyAllItems: function () {
                  try {
                    if (!this.enabled('allItems') || !window.${'$'}gameParty || !${'$'}gameParty.gainItem) return;
                    [window.${'$'}dataItems, window.${'$'}dataWeapons, window.${'$'}dataArmors].forEach(function (group) {
                      if (!group) return;
                      group.forEach(function (item) {
                        if (item) ${'$'}gameParty.gainItem(item, 99, true);
                      });
                    });
                  } catch (e) {}
                },
                applyExp: function () {
                  try {
                    if (!this.enabled('expEnabled')) return;
                    var target = this.number('exp', 999999);
                    this.actors().forEach(function (actor) {
                      if (actor.changeExp) actor.changeExp(target, false);
                    });
                  } catch (e) {}
                },
                apply: function (forceOnce) {
                  this.patchRuntime();
                  if (!this.active()) return;
                  var self = this;
                  if (this.enabled('infiniteHp') || this.enabled('infiniteMp')) {
                    this.actors().forEach(function (actor) { self.applyActor(actor); });
                  }
                  if (this.enabled('goldEnabled')) this.applyGold();
                  var nonce = this.number('applyNonce', 0);
                  if (forceOnce || nonce !== this.lastApplyNonce) {
                    this.lastApplyNonce = nonce;
                    if (this.enabled('allItems')) this.applyAllItems();
                    if (this.enabled('expEnabled')) this.applyExp();
                  }
                }
              };
              window.MKXPZPlayerModifier = modifier;
              modifier.patchRuntime();
              if (!window.__mkxpzModifierTimer) {
                window.__mkxpzModifierTimer = setInterval(function () {
                  try { window.MKXPZPlayerModifier.apply(false); } catch (e) {}
                }, 250);
              }
            })();
        """.trimIndent()
    }

    private fun installGamepad() {
        if (gamepadAttached) return

        configureGamepadInput()
        gamepad.attachTo(this, rootLayout)
        gamepad.setLayoutEditMode(gamepadLayoutEditing)
        gamepadAttached = true
        gamepadInvisible = false
        playerOptionsButton?.bringToFront()
    }

    private fun configureGamepadInput() {
        gamepad.init(gamepadConfig, false)
        gamepad.setOnKeyDownListener { key -> sendKey(KeyEvent.ACTION_DOWN, key) }
        gamepad.setOnKeyUpListener { key -> sendKey(KeyEvent.ACTION_UP, key) }
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
        showUpdatedPlayerOptions()
        return
    }

    private fun showUpdatedPlayerOptions() {
        if (isFinishing || playerMenuDialog?.isShowing == true) return
        val updatedGamepadAction = if (virtualGamepad) "隐藏虚拟按键" else "显示虚拟按键"
        val updatedMovementAction = if (gamepad.isJoystickMode) "切换为十字键" else "切换为摇杆"
        val updatedLayoutEditAction = if (gamepadLayoutEditing) "结束按键布局编辑" else "编辑按键布局"
        val updatedItems = arrayOf(
            "继续游戏",
            "通用修改器",
            updatedGamepadAction,
            updatedMovementAction,
            "虚拟按键设置",
            updatedLayoutEditAction,
            "保存按键布局",
            "重新加载游戏",
            "退出到启动器",
        )
        playerMenuDialog = AlertDialog.Builder(this)
            .setTitle("播放器选项")
            .setItems(updatedItems) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    1 -> showModifierSettings()
                    2 -> setVirtualGamepadEnabled(!virtualGamepad)
                    3 -> toggleMovementMode()
                    4 -> showGamepadSettings()
                    5 -> setGamepadLayoutEditing(!gamepadLayoutEditing)
                    6 -> saveGamepadLayout()
                    7 -> reloadGame()
                    8 -> finish()
                }
            }
            .setOnDismissListener {
                playerMenuDialog = null
                keepFullscreen()
            }
            .show()
        return

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

    private fun showModifierSettings() {
        val working = ModifierConfig().also {
            it.infiniteHp = modifierConfig.infiniteHp
            it.infiniteMp = modifierConfig.infiniteMp
            it.instantKill = modifierConfig.instantKill
            it.noClip = modifierConfig.noClip
            it.allItems = modifierConfig.allItems
            it.goldEnabled = modifierConfig.goldEnabled
            it.gold = modifierConfig.gold
            it.expEnabled = modifierConfig.expEnabled
            it.exp = modifierConfig.exp
            it.applyNonce = modifierConfig.applyNonce
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 18, 32, 0)
        }
        content.addView(Switch(this).apply {
            text = "无限 HP"
            isChecked = working.infiniteHp
            setOnCheckedChangeListener { _, checked -> working.infiniteHp = checked }
        })
        content.addView(Switch(this).apply {
            text = "无限 MP/MB"
            isChecked = working.infiniteMp
            setOnCheckedChangeListener { _, checked -> working.infiniteMp = checked }
        })
        content.addView(Switch(this).apply {
            text = "\u79d2\u6740\u654c\u4eba"
            isChecked = working.instantKill
            setOnCheckedChangeListener { _, checked -> working.instantKill = checked }
        })
        content.addView(Switch(this).apply {
            text = "无视树木和地图碰撞"
            isChecked = working.noClip
            setOnCheckedChangeListener { _, checked -> working.noClip = checked }
        })
        content.addView(Switch(this).apply {
            text = "获取全部物品"
            isChecked = working.allItems
            setOnCheckedChangeListener { _, checked -> working.allItems = checked }
        })
        content.addView(Switch(this).apply {
            text = "启用金钱修改"
            isChecked = working.goldEnabled
            setOnCheckedChangeListener { _, checked -> working.goldEnabled = checked }
        })
        val goldValue = numberEditText(working.gold.coerceAtLeast(0).toString(), "金钱")
        content.addView(goldValue)
        content.addView(Switch(this).apply {
            text = "启用经验修改"
            isChecked = working.expEnabled
            setOnCheckedChangeListener { _, checked -> working.expEnabled = checked }
        })
        val expValue = numberEditText(working.exp.coerceAtLeast(0).toString(), "经验")
        content.addView(expValue)

        AlertDialog.Builder(this)
            .setTitle("通用修改器")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton("应用") { _, _ ->
                working.gold = goldValue.text.toString().toIntOrNull()?.coerceIn(0, 99999999) ?: 0
                working.exp = expValue.text.toString().toIntOrNull()?.coerceIn(0, 99999999) ?: 0
                working.bumpApplyNonce()
                modifierConfig = working
                modifierConfig.save(this, intent.getStringExtra(MkxpLauncher.EXTRA_GAME_ID).orEmpty(), modifierConfigPath)
                applyHtml5Modifier()
                Toast.makeText(this, "修改器已应用", Toast.LENGTH_SHORT).show()
                keepFullscreen()
            }
            .setNegativeButton("取消") { _, _ -> keepFullscreen() }
            .show()
    }

    private fun numberEditText(value: String, hintText: String): EditText =
        EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(value)
            hint = hintText
            selectAll()
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

        val sizeIds = gamepad.getControlSizeIds()
        val sizeValues = sizeIds.associateWith { gamepad.getControlSizeDp(it) }.toMutableMap()
        addGamepadSizeControls(content, sizeIds, sizeValues)

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
        addKeyPicker(content, "RUN", working.keycodeRUN) { working.keycodeRUN = it }

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
                val appliedOpacity = (working.opacity ?: 30).coerceIn(5, 100)
                working.opacity = appliedOpacity
                gamepadConfig.applyFrom(working)
                gamepad.setOpacity(appliedOpacity)
                sizeValues.forEach { (id, size) -> gamepad.setControlSizeDp(id, size) }
                working.layout = gamepad.exportLayout()
                gamepadConfig = working
                MkxpLauncher.saveGamepadLayout(this, working.layout)
                reinstallGamepad()
                keepFullscreen()
            }
            .setNegativeButton("取消") { _, _ -> keepFullscreen() }
            .show()
    }

    private fun addGamepadSizeControls(
        container: LinearLayout,
        sizeIds: Array<String>,
        sizeValues: MutableMap<String, Int>,
    ) {
        container.addView(TextView(this).apply {
            text = "单个按键大小"
            textSize = 16f
            setPadding(0, 18, 0, 6)
        })

        sizeIds.forEach { id ->
            val min = gamepad.getMinControlSizeDp(id)
            val max = gamepad.getMaxControlSizeDp(id)
            sizeValues[id] = (sizeValues[id] ?: gamepad.getControlSizeDp(id)).coerceIn(min, max)
            val label = TextView(this)
            label.text = "${gamepad.getControlLabel(id)}：${sizeValues[id]}dp"
            val seek = SeekBar(this).apply {
                this.max = max - min
                progress = (sizeValues[id] ?: min) - min
                setOnSeekBarChangeListener(simpleSeekListener {
                    val nextSize = min + progress
                    sizeValues[id] = nextSize
                    label.text = "${gamepad.getControlLabel(id)}：${nextSize}dp"
                })
            }
            container.addView(label)
            container.addView(seek)
        }
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
        MkxpLauncher.saveGamepadLayout(this, gamepadConfig.layout)
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
                max = 96
                progress = sizeDp - 24
                setOnSeekBarChangeListener(simpleSeekListener {
                    sizeDp = progress + 24
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
                MkxpLauncher.saveGamepadLayout(this, gamepadConfig.layout)
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

    private fun shouldPreferOggAudio(root: File?): Boolean {
        val audio = root?.resolve("audio") ?: return false
        if (!audio.isDirectory) return false
        var hasOgg = false
        var hasM4a = false
        audio.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            when (file.extension.lowercase(Locale.US)) {
                "ogg", "oga" -> hasOgg = true
                "m4a", "mp4", "aac" -> hasM4a = true
            }
            if (hasOgg && hasM4a) return false
        }
        return hasOgg && !hasM4a
    }

    private fun interceptLocalGameResource(request: WebResourceRequest): WebResourceResponse? {
        val root = html5RootDir ?: return null
        val uri = request.url ?: return null
        if (!uri.scheme.equals("file", ignoreCase = true)) return null

        val requestPath = Uri.decode(uri.path ?: return null)
        val requestedFile = File(requestPath)
        val relativePath = relativePathInsideRoot(root, requestedFile) ?: return null
        createPatchedScriptResponse(root, relativePath)?.let { return it }
        if (!isAudioResourcePath(relativePath)) {
            return null
        }
        val target = resolveGameResource(root, relativePath) ?: return null
        return runCatching { createWebResourceResponse(target, request) }.getOrNull()
    }

    private fun createPatchedScriptResponse(root: File, relativePath: String): WebResourceResponse? {
        val normalized = relativePath.replace('\\', '/').lowercase(Locale.US)
        val shouldPatchAudio = preferOggAudio && isAudioManagerScript(normalized)
        val shouldPatchThreeD = html5ThreeDMode && isCoreScript(normalized)
        val shouldPatchModifier = isCoreScript(normalized) || isAudioManagerScript(normalized)
        if (!shouldPatchAudio && !shouldPatchThreeD && !shouldPatchModifier) return null

        val script = resolveCaseInsensitive(root, relativePath)?.takeIf { it.isFile } ?: return null
        val original = runCatching { script.readText() }.getOrNull() ?: return null
        val patched = buildString {
            append(original)
            appendLine()
            if (shouldPatchThreeD) {
                appendLine(";")
                appendLine(threeDCompatibilityScript())
            }
            if (shouldPatchModifier) {
                appendLine(";")
                appendLine(modifierCompatibilityScript())
            }
            if (shouldPatchAudio) {
                appendLine(";")
                appendLine(audioCompatibilityScript())
            }
        }
        return WebResourceResponse(
            "application/javascript",
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Access-Control-Allow-Origin" to "*",
                "Content-Length" to patched.toByteArray(Charsets.UTF_8).size.toString(),
            ),
            patched.byteInputStream(Charsets.UTF_8),
        )
    }

    private fun isAudioResourcePath(relativePath: String): Boolean {
        val normalized = relativePath.replace('\\', '/').lowercase(Locale.US)
        if (!normalized.startsWith("audio/")) return false
        return normalized.substringAfterLast('.', "").lowercase(Locale.US) in audioResourceExtensions
    }

    private fun isAudioManagerScript(normalizedPath: String): Boolean {
        return normalizedPath == "js/rpg_managers.js" || normalizedPath == "js/rmmz_managers.js"
    }

    private fun isCoreScript(normalizedPath: String): Boolean {
        return normalizedPath == "js/rpg_core.js" || normalizedPath == "js/rmmz_core.js"
    }

    private fun relativePathInsideRoot(root: File, file: File): String? {
        val rootPath = root.absoluteFile.path.trimEnd(File.separatorChar)
        val filePath = file.absoluteFile.path
        if (filePath == rootPath) return ""
        if (!filePath.startsWith(rootPath + File.separator)) return null
        return filePath.substring(rootPath.length + 1)
    }

    private fun resolveGameResource(root: File, relativePath: String): File? {
        resolveCaseInsensitive(root, relativePath)?.takeIf { it.isFile }?.let { return it }
        return resolveAudioFallback(root, relativePath)
    }

    private fun resolveAudioFallback(root: File, relativePath: String): File? {
        val normalized = relativePath.replace('\\', '/')
        val lower = normalized.lowercase(Locale.US)
        if (!lower.startsWith("audio/") || '.' !in normalized.substringAfterLast('/')) return null

        val base = normalized.substringBeforeLast('.')
        val extension = normalized.substringAfterLast('.').lowercase(Locale.US)
        val candidates = when (extension) {
            "m4a", "mp4", "aac" -> listOf("ogg", "oga", "mp3", "wav")
            "ogg", "oga" -> listOf("m4a", "mp4", "aac", "mp3", "wav")
            else -> emptyList()
        }
        return candidates
            .asSequence()
            .mapNotNull { candidate -> resolveCaseInsensitive(root, "$base.$candidate") }
            .firstOrNull { it.isFile }
    }

    private fun resolveCaseInsensitive(root: File, relativePath: String): File? {
        val parts = relativePath.replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." }
        var current = root
        for (part in parts) {
            if (part == "..") return null
            val exact = File(current, part)
            current = if (exact.exists()) {
                exact
            } else {
                current.listFiles()
                    ?.firstOrNull { it.name.equals(part, ignoreCase = true) }
                    ?: exact
            }
        }
        return current
    }

    private fun createWebResourceResponse(file: File, request: WebResourceRequest): WebResourceResponse {
        val length = file.length().coerceAtLeast(0L)
        val mime = mimeTypeFor(file)
        val range = parseRangeHeader(request.requestHeaders, length)
        val headers = linkedMapOf(
            "Accept-Ranges" to "bytes",
            "Access-Control-Allow-Origin" to "*",
        )

        return if (range != null) {
            val input = BufferedInputStream(FileInputStream(file))
            skipFully(input, range.start)
            val contentLength = range.endInclusive - range.start + 1L
            headers["Content-Length"] = contentLength.toString()
            headers["Content-Range"] = "bytes ${range.start}-${range.endInclusive}/$length"
            WebResourceResponse(
                mime,
                null,
                206,
                "Partial Content",
                headers,
                LimitedInputStream(input, contentLength),
            )
        } else {
            headers["Content-Length"] = length.toString()
            WebResourceResponse(
                mime,
                textEncodingFor(file),
                200,
                "OK",
                headers,
                BufferedInputStream(FileInputStream(file)),
            )
        }
    }

    private fun parseRangeHeader(headers: Map<String, String>, length: Long): ByteRange? {
        if (length <= 0L) return null
        val range = headers.entries
            .firstOrNull { it.key.equals("Range", ignoreCase = true) }
            ?.value
            ?.trim()
            ?: return null
        if (!range.startsWith("bytes=", ignoreCase = true)) return null

        val spec = range.substringAfter("=").substringBefore(",").trim()
        val dash = spec.indexOf('-')
        if (dash < 0) return null

        val startText = spec.substring(0, dash).trim()
        val endText = spec.substring(dash + 1).trim()
        val start: Long
        val end: Long
        if (startText.isBlank()) {
            val suffixLength = endText.toLongOrNull()?.coerceAtLeast(0L) ?: return null
            if (suffixLength == 0L) return null
            start = (length - suffixLength).coerceAtLeast(0L)
            end = length - 1L
        } else {
            start = startText.toLongOrNull() ?: return null
            end = endText.toLongOrNull() ?: length - 1L
        }
        if (start !in 0 until length) return null
        return ByteRange(start, end.coerceIn(start, length - 1L))
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else if (input.read() >= 0) {
                remaining -= 1L
            } else {
                break
            }
        }
    }

    private fun mimeTypeFor(file: File): String {
        return when (file.extension.lowercase(Locale.US)) {
            "html", "htm" -> "text/html"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "css" -> "text/css"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ogg", "oga" -> "audio/ogg"
            "m4a", "mp4", "aac" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "webm" -> "video/webm"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "wasm" -> "application/wasm"
            else -> "application/octet-stream"
        }
    }

    private fun textEncodingFor(file: File): String? {
        return when (file.extension.lowercase(Locale.US)) {
            "html", "htm", "js", "json", "css", "svg" -> "UTF-8"
            else -> null
        }
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
        keycodeRUN = intent.getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_RUN, KeyEvent.KEYCODE_SHIFT_LEFT)
        physicalMappingEnabled = intent.getBooleanExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_MAPPING, true)
        physicalBackAsB = intent.getBooleanExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B, true)
        physicalKeycodeA = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_A, KeyEvent.KEYCODE_Z)
        physicalKeycodeB = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_B, KeyEvent.KEYCODE_X)
        physicalKeycodeX = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_X, KeyEvent.KEYCODE_A)
        physicalKeycodeY = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_Y, KeyEvent.KEYCODE_S)
        physicalKeycodeL1 = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_L1, KeyEvent.KEYCODE_Q)
        physicalKeycodeR1 = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_R1, KeyEvent.KEYCODE_W)
        physicalKeycodeL2 = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_L2, KeyEvent.KEYCODE_PAGE_UP)
        physicalKeycodeR2 = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_R2, KeyEvent.KEYCODE_PAGE_DOWN)
        physicalKeycodeStart = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_START, KeyEvent.KEYCODE_ENTER)
        physicalKeycodeSelect = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT, KeyEvent.KEYCODE_ESCAPE)
        physicalKeycodeRun = intent.getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_RUN, KeyEvent.KEYCODE_SHIFT_LEFT)
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
        it.keycodeRUN = keycodeRUN
        it.physicalMappingEnabled = physicalMappingEnabled
        it.physicalBackAsB = physicalBackAsB
        it.physicalKeycodeA = physicalKeycodeA
        it.physicalKeycodeB = physicalKeycodeB
        it.physicalKeycodeX = physicalKeycodeX
        it.physicalKeycodeY = physicalKeycodeY
        it.physicalKeycodeL1 = physicalKeycodeL1
        it.physicalKeycodeR1 = physicalKeycodeR1
        it.physicalKeycodeL2 = physicalKeycodeL2
        it.physicalKeycodeR2 = physicalKeycodeR2
        it.physicalKeycodeStart = physicalKeycodeStart
        it.physicalKeycodeSelect = physicalKeycodeSelect
        it.physicalKeycodeRun = physicalKeycodeRun
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
        keycodeRUN = source.keycodeRUN
        physicalMappingEnabled = source.physicalMappingEnabled
        physicalBackAsB = source.physicalBackAsB
        physicalKeycodeA = source.physicalKeycodeA
        physicalKeycodeB = source.physicalKeycodeB
        physicalKeycodeX = source.physicalKeycodeX
        physicalKeycodeY = source.physicalKeycodeY
        physicalKeycodeL1 = source.physicalKeycodeL1
        physicalKeycodeR1 = source.physicalKeycodeR1
        physicalKeycodeL2 = source.physicalKeycodeL2
        physicalKeycodeR2 = source.physicalKeycodeR2
        physicalKeycodeStart = source.physicalKeycodeStart
        physicalKeycodeSelect = source.physicalKeycodeSelect
        physicalKeycodeRun = source.physicalKeycodeRun
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

    private data class ByteRange(
        val start: Long,
        val endInclusive: Long,
    )

    private class LimitedInputStream(
        private val input: InputStream,
        private var remaining: Long,
    ) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = input.read()
            if (value >= 0) remaining -= 1L
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0L) return -1
            val maxRead = minOf(length.toLong(), remaining).toInt()
            val read = input.read(buffer, offset, maxRead)
            if (read > 0) remaining -= read.toLong()
            return read
        }

        override fun close() {
            input.close()
        }
    }

    companion object {
        private val audioResourceExtensions = setOf("m4a", "mp4", "aac", "ogg", "oga", "mp3", "wav")

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
