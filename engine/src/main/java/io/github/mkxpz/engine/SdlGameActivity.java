package io.github.mkxpz.engine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hatkid.mkxpz.gamepad.Gamepad;
import com.hatkid.mkxpz.gamepad.GamepadConfig;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class SdlGameActivity extends SDLActivity {
    private static final String TAG = "mkxp-z[Activity]";

    public static String GAME_PATH = "";
    public static String CONFIG_PATH = "";
    public static String REAL_GAME_PATH = "";
    public static boolean DEBUG = false;
    public static boolean VIRTUAL_GAMEPAD = true;

    private boolean mStarted = false;
    private boolean mGamepadInvisible = false;
    private boolean mGamepadAttached = false;
    private boolean mGamepadLayoutEditing = false;

    protected static Handler mMainHandler;
    protected static Vibrator mVibrator;
    protected static TextView tvFps;

    private final Gamepad mGamepad = new Gamepad();
    private GamepadConfig mGamepadConfig = new GamepadConfig();
    private AlertDialog mPlayerMenuDialog;
    private TextView mPlayerOptionsButton;

    private void runSDLThread() {
        if (!mStarted) {
            Log.i(TAG, "Game path: " + GAME_PATH);
        }

        mStarted = true;

        if (mHasMultiWindow) {
            resumeNativeThread();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String configPath = stringExtra(MkxpLauncher.EXTRA_CONFIG_PATH);
        String gamePath = stringExtra(MkxpLauncher.EXTRA_GAME_PATH);

        CONFIG_PATH = configPath;
        REAL_GAME_PATH = gamePath;
        GAME_PATH = prepareReferenceGamePath(configPath, gamePath);
        DEBUG = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_DEBUG, false);
        VIRTUAL_GAMEPAD = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_VIRTUAL_GAMEPAD, true);
        mGamepadConfig = readGamepadConfigFromIntent();

        Log.i(TAG, "mkxp launch GAME_PATH=" + GAME_PATH + " REAL_GAME_PATH=" + REAL_GAME_PATH + " CONFIG_PATH=" + CONFIG_PATH);

        super.onCreate(savedInstanceState);

        mMainHandler = new Handler(getMainLooper());
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (!mBrokenLibraries) {
            installGamepad();
            installFpsText();
            installPlayerOptionsButton();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        runSDLThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bringGameOverlaysToFront();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            bringGameOverlaysToFront();
        }
    }

    @Override
    protected String[] getArguments() {
        return DEBUG ? new String[] { "debug" } : new String[] {};
    }

    @Override
    protected void onDestroy() {
        if (mPlayerMenuDialog != null) {
            mPlayerMenuDialog.dismiss();
        }
        super.onDestroy();

        System.exit(0);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (evt.getAction() == KeyEvent.ACTION_UP && evt.getRepeatCount() == 0) {
                showPlayerOptions();
            }
            return true;
        }

        if (VIRTUAL_GAMEPAD && !isSystemKey(evt.getKeyCode())) {
            if (!mGamepadInvisible) {
                mGamepad.hideView();
                mGamepadInvisible = true;
            }
        }

        if (VIRTUAL_GAMEPAD && mGamepad.processGamepadEvent(evt)) {
            return true;
        }

        return super.dispatchKeyEvent(evt);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent evt) {
        if (VIRTUAL_GAMEPAD && mGamepadInvisible) {
            mGamepad.showView();
            mGamepadInvisible = false;
        }

        return super.dispatchTouchEvent(evt);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent evt) {
        if (VIRTUAL_GAMEPAD && mGamepad.processDPadEvent(evt)) {
            return true;
        }

        return super.onGenericMotionEvent(evt);
    }

    @Override
    public void onBackPressed() {
        showPlayerOptions();
    }

    private void installGamepad() {
        if (!VIRTUAL_GAMEPAD || mLayout == null || mGamepadAttached) {
            return;
        }

        mGamepadInvisible = isAndroidTV() || isChromebook();
        mGamepad.init(mGamepadConfig, mGamepadInvisible);
        mGamepad.setOnKeyDownListener(SDLActivity::onNativeKeyDown);
        mGamepad.setOnKeyUpListener(SDLActivity::onNativeKeyUp);
        mGamepad.attachTo(this, mLayout);
        mGamepad.setLayoutEditMode(mGamepadLayoutEditing);
        mGamepad.bringToFront();
        mGamepadAttached = true;
    }

    private void installFpsText() {
        if (mLayout == null) {
            return;
        }

        tvFps = new TextView(this);
        tvFps.setTextSize(8 * ((float) getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
        tvFps.setTextColor(Color.argb(96, 255, 255, 255));
        tvFps.setVisibility(View.GONE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 16, 0, 0);
        tvFps.setLayoutParams(params);

        mLayout.addView(tvFps);
    }

    private void installPlayerOptionsButton() {
        if (mLayout == null || mPlayerOptionsButton != null) {
            return;
        }

        mPlayerOptionsButton = new TextView(this);
        mPlayerOptionsButton.setText("\u64ad\u653e\u5668\u9009\u9879");
        mPlayerOptionsButton.setTextSize(13f);
        mPlayerOptionsButton.setGravity(Gravity.CENTER);
        mPlayerOptionsButton.setTextColor(Color.WHITE);
        mPlayerOptionsButton.setPadding(dp(18), dp(6), dp(18), dp(6));
        mPlayerOptionsButton.setFocusable(false);
        mPlayerOptionsButton.setClickable(true);
        mPlayerOptionsButton.setBackground(roundedOverlayBackground());
        mPlayerOptionsButton.setElevation(dp(4));
        mPlayerOptionsButton.setOnClickListener(view -> showPlayerOptions());

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.topMargin = dp(12);
        mLayout.addView(mPlayerOptionsButton, params);
        mPlayerOptionsButton.bringToFront();
    }

    private void showPlayerOptions() {
        if (isFinishing()) {
            return;
        }

        String gamepadToggle = mGamepadInvisible
                ? "\u663e\u793a\u865a\u62df\u6309\u952e"
                : "\u9690\u85cf\u865a\u62df\u6309\u952e";
        String editToggle = mGamepadLayoutEditing
                ? "\u9000\u51fa\u5e03\u5c40\u7f16\u8f91"
                : "\u7f16\u8f91\u6309\u952e\u5e03\u5c40";

        String[] items = new String[] {
                "\u7ee7\u7eed\u6e38\u620f",
                gamepadToggle,
                editToggle,
                "\u4fdd\u5b58\u6309\u952e\u5e03\u5c40",
                "\u9000\u51fa\u6e38\u620f"
        };

        mPlayerMenuDialog = new AlertDialog.Builder(this)
                .setTitle("\u64ad\u653e\u5668\u9009\u9879")
                .setItems(items, (dialog, which) -> {
                    if (which == 1) {
                        toggleGamepadVisibility();
                    } else if (which == 2) {
                        toggleGamepadLayoutEdit();
                    } else if (which == 3) {
                        saveGamepadLayout();
                    } else if (which == 4) {
                        finish();
                    }
                })
                .show();
    }

    private void toggleGamepadVisibility() {
        if (!VIRTUAL_GAMEPAD) {
            Toast.makeText(this, "\u865a\u62df\u6309\u952e\u672a\u542f\u7528", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mGamepadInvisible) {
            mGamepad.showView();
        } else {
            mGamepad.hideView();
        }
        mGamepadInvisible = !mGamepadInvisible;
    }

    private void toggleGamepadLayoutEdit() {
        if (!VIRTUAL_GAMEPAD) {
            Toast.makeText(this, "\u865a\u62df\u6309\u952e\u672a\u542f\u7528", Toast.LENGTH_SHORT).show();
            return;
        }

        mGamepadLayoutEditing = !mGamepadLayoutEditing;
        mGamepad.setLayoutEditMode(mGamepadLayoutEditing);
        Toast.makeText(
                this,
                mGamepadLayoutEditing ? "\u5df2\u8fdb\u5165\u5e03\u5c40\u7f16\u8f91" : "\u5df2\u9000\u51fa\u5e03\u5c40\u7f16\u8f91",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void saveGamepadLayout() {
        String layout = mGamepad.exportLayout();
        MkxpLauncher.INSTANCE.saveGamepadLayout(this, layout);
        Toast.makeText(this, "\u6309\u952e\u5e03\u5c40\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
    }

    private void bringGameOverlaysToFront() {
        if (VIRTUAL_GAMEPAD) {
            mGamepad.bringToFront();
        }
        if (mPlayerOptionsButton != null) {
            mPlayerOptionsButton.bringToFront();
            mPlayerOptionsButton.invalidate();
        }
    }

    private GamepadConfig readGamepadConfigFromIntent() {
        GamepadConfig config = new GamepadConfig();
        config.opacity = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_OPACITY, 30);
        config.scale = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_SCALE, 100);
        config.diagonalMovement = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_GAMEPAD_DIAGONAL_MOVEMENT, false);
        config.keycodeA = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_A, KeyEvent.KEYCODE_Z);
        config.keycodeB = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_B, KeyEvent.KEYCODE_X);
        config.keycodeC = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_C, KeyEvent.KEYCODE_C);
        config.keycodeX = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_X, KeyEvent.KEYCODE_A);
        config.keycodeY = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_Y, KeyEvent.KEYCODE_S);
        config.keycodeZ = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_Z, KeyEvent.KEYCODE_D);
        config.keycodeL = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_L, KeyEvent.KEYCODE_Q);
        config.keycodeR = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_R, KeyEvent.KEYCODE_W);
        config.keycodeCTRL = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_CTRL, KeyEvent.KEYCODE_CTRL_LEFT);
        config.keycodeALT = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_ALT, KeyEvent.KEYCODE_ALT_LEFT);
        config.keycodeSHIFT = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_SHIFT, KeyEvent.KEYCODE_SHIFT_LEFT);
        config.layout = stringExtra(MkxpLauncher.EXTRA_GAMEPAD_LAYOUT);
        return config;
    }

    private String stringExtra(String name) {
        String value = getIntent().getStringExtra(name);
        return value == null ? "" : value;
    }

    private static String prepareReferenceGamePath(String configPath, String gamePath) {
        File configFile = new File(configPath);
        String configParent = configFile.getParentFile() != null ? configFile.getParentFile().getAbsolutePath() : null;
        File gameDir = new File(gamePath);
        if (!gameDir.isDirectory()) {
            return configParent != null ? configParent : gamePath;
        }
        if (!hasGameIni(gameDir)) {
            return configParent != null ? configParent : gameDir.getAbsolutePath();
        }
        if (!configFile.isFile()) {
            return gameDir.getAbsolutePath();
        }

        try {
            File target = new File(gameDir, "mkxp.json");
            if (!target.isFile() || !sameFileBytes(configFile, target)) {
                copyFile(configFile, target);
            }
            return gameDir.getAbsolutePath();
        } catch (IOException e) {
            Log.w(TAG, "Failed to mirror mkxp.json into game dir; falling back to config dir", e);
            return configParent != null ? configParent : gameDir.getAbsolutePath();
        }
    }

    private static boolean hasGameIni(File gameDir) {
        File[] files = gameDir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (file.isFile() && "Game.ini".equalsIgnoreCase(file.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameFileBytes(File left, File right) throws IOException {
        if (left.length() != right.length()) {
            return false;
        }

        byte[] leftBuffer = new byte[8192];
        byte[] rightBuffer = new byte[8192];
        try (FileInputStream leftIn = new FileInputStream(left);
             FileInputStream rightIn = new FileInputStream(right)) {
            int leftRead;
            while ((leftRead = leftIn.read(leftBuffer)) != -1) {
                int rightRead = rightIn.read(rightBuffer, 0, leftRead);
                if (rightRead != leftRead) {
                    return false;
                }
                for (int i = 0; i < leftRead; i++) {
                    if (leftBuffer[i] != rightBuffer[i]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void copyFile(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private boolean isSystemKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_HEADSETHOOK;
    }

    private GradientDrawable roundedOverlayBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.argb(128, 0, 0, 0));
        drawable.setStroke(dp(1), Color.argb(64, 255, 255, 255));
        drawable.setCornerRadius(dp(14));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("unused")
    private static void updateFPSText(int num) {
        if (mMainHandler != null && tvFps != null) {
            mMainHandler.post(() -> tvFps.setText(num + " FPS"));
        }
    }

    @SuppressWarnings("unused")
    private static void setFPSVisibility(boolean visible) {
        if (mMainHandler != null && tvFps != null) {
            mMainHandler.post(() -> tvFps.setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
        }
    }

    @SuppressWarnings("unused")
    private static String getSystemLanguage() {
        return Locale.getDefault().toString();
    }

    @SuppressWarnings("unused")
    private static boolean hasVibrator() {
        return mVibrator != null && mVibrator.hasVibrator();
    }

    @SuppressWarnings("unused")
    private static void vibrate(int duration) {
        if (mVibrator == null) {
            return;
        }
        int clamped = Math.min(duration, 10000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createOneShot(clamped, VibrationEffect.EFFECT_HEAVY_CLICK));
        } else {
            mVibrator.vibrate(clamped);
        }
    }

    @SuppressWarnings("unused")
    private static void vibrateStop() {
        if (mVibrator != null) {
            mVibrator.cancel();
        }
    }

    @SuppressWarnings("unused")
    private static boolean inMultiWindow(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode();
    }
}
