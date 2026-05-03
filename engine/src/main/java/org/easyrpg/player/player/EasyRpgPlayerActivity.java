package org.easyrpg.player.player;

import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import io.github.mkxpz.engine.MkxpLauncher;
import io.github.mkxpz.engine.SdlGameActivity;

import org.libsdl.app.SDLActivity;

import java.io.File;

/**
 * Compatibility activity for the upstream EasyRPG Android native layer.
 *
 * The native source looks up this exact Java class name for JNI callbacks. Keep
 * the package and method names aligned with upstream while sharing this
 * launcher's SDL overlay and player menu.
 */
public class EasyRpgPlayerActivity extends SdlGameActivity {
    public static final String TAG_PROJECT_PATH = "project_path";
    public static final String TAG_LOG_FILE = "log_file";
    public static final String TAG_SAVE_PATH = "save_path";
    public static final String TAG_COMMAND_LINE = "command_line";
    public static final String TAG_STANDALONE = "standalone_mode";

    private final Handler surfaceRecoveryHandler = new Handler(Looper.getMainLooper());
    private final Runnable surfaceRecoveryRunnable = new Runnable() {
        @Override
        public void run() {
            recoverSurfaceAfterForeground();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        forceLandscape();
        super.onCreate(savedInstanceState);
        applyModifierConfigFromIntent();
        forceLandscape();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyModifierConfigFromIntent();
        scheduleSurfaceRecovery();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            scheduleSurfaceRecovery();
        }
    }

    @Override
    protected void onDestroy() {
        surfaceRecoveryHandler.removeCallbacks(surfaceRecoveryRunnable);
        super.onDestroy();
    }

    @Override
    public void setOrientationBis(int w, int h, boolean resizable, String hint) {
        forceLandscape();
    }

    @Override
    protected boolean shouldResumeInOnStartForMultiWindow() {
        return true;
    }

    @Override
    protected boolean shouldUseOnPauseResumeForMultiWindow() {
        return true;
    }

    @Override
    protected boolean shouldUseFullFocusLifecycle() {
        return true;
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "easyrpg_android"
        };
    }

    @Override
    protected String[] getArguments() {
        String[] args = getIntent().getStringArrayExtra(TAG_COMMAND_LINE);
        return args == null ? new String[0] : args;
    }

    public String getRtpPath() {
        String configured = getIntent().getStringExtra(MkxpLauncher.EXTRA_EASYRPG_RTP_PATH);
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }

        File rtpDir = new File(getFilesDir(), "easyrpg/rtp");
        return rtpDir.isDirectory() ? rtpDir.getAbsolutePath() : "";
    }

    public static SafFile getHandleForPath(String path) {
        return SafFile.fromPath(SDLActivity.getContext(), path);
    }

    public static AssetManager getAssetManager() {
        return SDLActivity.getContext().getAssets();
    }

    public static native void openSettings();

    public static native void endGame();

    public static native void resetGame();

    public static native void toggleFps();

    public static native void applyModifier(String configPath);

    private void scheduleSurfaceRecovery() {
        if (mBrokenLibraries) {
            return;
        }

        surfaceRecoveryHandler.removeCallbacks(surfaceRecoveryRunnable);
        surfaceRecoveryHandler.postDelayed(surfaceRecoveryRunnable, 120L);
        surfaceRecoveryHandler.postDelayed(surfaceRecoveryRunnable, 420L);
    }

    private void recoverSurfaceAfterForeground() {
        if (mBrokenLibraries || mSurface == null || !hasWindowFocus()) {
            return;
        }

        Surface nativeSurface = mSurface.getHolder().getSurface();
        int width = mSurface.getWidth();
        int height = mSurface.getHeight();

        if (nativeSurface == null || !nativeSurface.isValid() || width <= 0 || height <= 0) {
            mSurface.requestLayout();
            mSurface.invalidate();
            return;
        }

        mSurface.surfaceChanged(mSurface.getHolder(), 0, width, height);
        mSurface.requestFocus();
        mSurface.invalidate();
    }

    private void forceLandscape() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void applyModifierConfigFromIntent() {
        String configPath = getIntent().getStringExtra(MkxpLauncher.EXTRA_MODIFIER_CONFIG_PATH);
        if (configPath == null || configPath.isEmpty()) {
            return;
        }
        try {
            applyModifier(configPath);
        } catch (UnsatisfiedLinkError ignored) {
            // Older bundled EasyRPG cores simply do not expose the modifier hook.
        }
    }
}
