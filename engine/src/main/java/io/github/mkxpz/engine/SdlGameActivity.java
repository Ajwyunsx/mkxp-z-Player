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
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hatkid.mkxpz.gamepad.Gamepad;
import com.hatkid.mkxpz.gamepad.GamepadConfig;

import org.libsdl.app.SDLActivity;
import org.easyrpg.player.player.EasyRpgPlayerActivity;

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
    public static String GAME_ID = "";
    public static String MODIFIER_CONFIG_PATH = "";
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
    private ModifierConfig mModifierConfig = new ModifierConfig();
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
        GAME_ID = stringExtra(MkxpLauncher.EXTRA_GAME_ID);
        MODIFIER_CONFIG_PATH = stringExtra(MkxpLauncher.EXTRA_MODIFIER_CONFIG_PATH);
        GAME_PATH = prepareReferenceGamePath(configPath, gamePath);
        DEBUG = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_DEBUG, false);
        VIRTUAL_GAMEPAD = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_VIRTUAL_GAMEPAD, true);
        mGamepadConfig = readGamepadConfigFromIntent();
        mModifierConfig = ModifierConfig.load(this, GAME_ID, MODIFIER_CONFIG_PATH);
        if (!MODIFIER_CONFIG_PATH.isEmpty()) {
            mModifierConfig.writeToFile(new File(MODIFIER_CONFIG_PATH));
        }

        Log.i(TAG, "mkxp launch GAME_PATH=" + GAME_PATH + " REAL_GAME_PATH=" + REAL_GAME_PATH + " CONFIG_PATH=" + CONFIG_PATH);

        super.onCreate(savedInstanceState);

        mMainHandler = new Handler(getMainLooper());
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        configureGamepadInput();

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

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent evt) {
        if (!mBrokenLibraries && mGamepad.processGamepadEvent(evt)) {
            return true;
        }
        if (!mBrokenLibraries && mGamepad.isPhysicalGamepadBackEvent(evt)) {
            return super.dispatchKeyEvent(evt);
        }

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
        if (!mBrokenLibraries && mGamepad.processDPadEvent(evt)) {
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
        configureGamepadInput();
        mGamepad.attachTo(this, mLayout);
        mGamepad.setLayoutEditMode(mGamepadLayoutEditing);
        mGamepad.bringToFront();
        mGamepadAttached = true;
    }

    private void configureGamepadInput() {
        mGamepad.init(mGamepadConfig, mGamepadInvisible);
        mGamepad.setOnKeyDownListener(SDLActivity::onNativeKeyDown);
        mGamepad.setOnKeyUpListener(SDLActivity::onNativeKeyUp);
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
        String movementToggle = mGamepad.isJoystickMode()
                ? "\u5207\u6362\u4e3a\u5341\u5b57\u952e"
                : "\u5207\u6362\u4e3a\u6447\u6746";

        String[] items = new String[] {
                "\u7ee7\u7eed\u6e38\u620f",
                "\u901a\u7528\u4fee\u6539\u5668",
                gamepadToggle,
                movementToggle,
                "\u865a\u62df\u6309\u952e\u8bbe\u7f6e",
                editToggle,
                "\u4fdd\u5b58\u6309\u952e\u5e03\u5c40",
                "\u9000\u51fa\u6e38\u620f"
        };

        mPlayerMenuDialog = new AlertDialog.Builder(this)
                .setTitle("\u64ad\u653e\u5668\u9009\u9879")
                .setItems(items, (dialog, which) -> {
                    if (which == 1) {
                        showModifierSettings();
                    } else if (which == 2) {
                        toggleGamepadVisibility();
                    } else if (which == 3) {
                        toggleMovementMode();
                    } else if (which == 4) {
                        showGamepadSettings();
                    } else if (which == 5) {
                        toggleGamepadLayoutEdit();
                    } else if (which == 6) {
                        saveGamepadLayout();
                    } else if (which == 7) {
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

    private void toggleMovementMode() {
        if (!ensureGamepadReady()) {
            return;
        }

        boolean joystick = mGamepad.toggleMovementMode();
        mGamepadConfig.layout = mGamepad.exportLayout();
        MkxpLauncher.INSTANCE.saveGamepadLayout(this, mGamepadConfig.layout);
        Toast.makeText(
                this,
                joystick ? "\u5df2\u5207\u6362\u4e3a\u6447\u6746" : "\u5df2\u5207\u6362\u4e3a\u5341\u5b57\u952e",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showModifierSettings() {
        ModifierConfig working = copyModifierConfig(mModifierConfig);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(32), dp(18), dp(32), 0);

        Switch infiniteHp = new Switch(this);
        infiniteHp.setText("\u65e0\u9650 HP");
        infiniteHp.setChecked(working.infiniteHp);
        infiniteHp.setOnCheckedChangeListener((buttonView, checked) -> working.infiniteHp = checked);
        content.addView(infiniteHp);

        Switch infiniteMp = new Switch(this);
        infiniteMp.setText("\u65e0\u9650 MP/MB");
        infiniteMp.setChecked(working.infiniteMp);
        infiniteMp.setOnCheckedChangeListener((buttonView, checked) -> working.infiniteMp = checked);
        content.addView(infiniteMp);

        Switch instantKill = new Switch(this);
        instantKill.setText("\u79d2\u6740\u654c\u4eba");
        instantKill.setChecked(working.instantKill);
        instantKill.setOnCheckedChangeListener((buttonView, checked) -> working.instantKill = checked);
        content.addView(instantKill);

        Switch noClip = new Switch(this);
        noClip.setText("\u65e0\u89c6\u6811\u6728\u548c\u5730\u56fe\u78b0\u649e");
        noClip.setChecked(working.noClip);
        noClip.setOnCheckedChangeListener((buttonView, checked) -> working.noClip = checked);
        content.addView(noClip);

        Switch allItems = new Switch(this);
        allItems.setText("\u83b7\u53d6\u5168\u90e8\u7269\u54c1");
        allItems.setChecked(working.allItems);
        allItems.setOnCheckedChangeListener((buttonView, checked) -> working.allItems = checked);
        content.addView(allItems);

        Switch goldEnabled = new Switch(this);
        goldEnabled.setText("\u542f\u7528\u91d1\u94b1\u4fee\u6539");
        goldEnabled.setChecked(working.goldEnabled);
        goldEnabled.setOnCheckedChangeListener((buttonView, checked) -> working.goldEnabled = checked);
        content.addView(goldEnabled);

        EditText goldValue = numberEditText(String.valueOf(Math.max(0, working.gold)));
        goldValue.setHint("\u91d1\u94b1");
        content.addView(goldValue);

        Switch expEnabled = new Switch(this);
        expEnabled.setText("\u542f\u7528\u7ecf\u9a8c\u4fee\u6539");
        expEnabled.setChecked(working.expEnabled);
        expEnabled.setOnCheckedChangeListener((buttonView, checked) -> working.expEnabled = checked);
        content.addView(expEnabled);

        EditText expValue = numberEditText(String.valueOf(Math.max(0, working.exp)));
        expValue.setHint("\u7ecf\u9a8c");
        content.addView(expValue);

        new AlertDialog.Builder(this)
                .setTitle("\u901a\u7528\u4fee\u6539\u5668")
                .setView(wrapInScrollView(content))
                .setPositiveButton("\u5e94\u7528", (dialog, which) -> {
                    working.gold = parsePositiveInt(goldValue.getText().toString(), 0);
                    working.exp = parsePositiveInt(expValue.getText().toString(), 0);
                    working.bumpApplyNonce();
                    mModifierConfig = working;
                    mModifierConfig.save(this, GAME_ID, MODIFIER_CONFIG_PATH);
                    applyRuntimeModifier();
                    Toast.makeText(this, "\u4fee\u6539\u5668\u5df2\u5e94\u7528", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private EditText numberEditText(String value) {
        EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setSingleLine(true);
        editText.setText(value);
        editText.setSelectAllOnFocus(true);
        return editText;
    }

    private ModifierConfig copyModifierConfig(ModifierConfig source) {
        ModifierConfig config = new ModifierConfig();
        config.infiniteHp = source.infiniteHp;
        config.infiniteMp = source.infiniteMp;
        config.instantKill = source.instantKill;
        config.noClip = source.noClip;
        config.allItems = source.allItems;
        config.goldEnabled = source.goldEnabled;
        config.gold = source.gold;
        config.expEnabled = source.expEnabled;
        config.exp = source.exp;
        config.applyNonce = source.applyNonce;
        return config;
    }

    private void applyRuntimeModifier() {
        if (this instanceof EasyRpgPlayerActivity) {
            try {
                EasyRpgPlayerActivity.applyModifier(MODIFIER_CONFIG_PATH);
            } catch (UnsatisfiedLinkError error) {
                Toast.makeText(this, "\u5f53\u524d EasyRPG \u6838\u5fc3\u672a\u5305\u542b\u4fee\u6539\u5668\u7b26\u53f7\uff0c\u8bf7\u91cd\u65b0\u6253\u5305\u6838\u5fc3\u5e93", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showGamepadSettings() {
        if (!ensureGamepadReady()) {
            return;
        }

        GamepadConfig working = copyGamepadConfig(mGamepadConfig);
        working.layout = mGamepad.exportLayout();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(32), dp(18), dp(32), 0);

        TextView opacityLabel = new TextView(this);
        final int[] opacity = new int[] { clamp(working.opacity, 5, 100) };
        opacityLabel.setText("\u900f\u660e\u5ea6\uff1a" + opacity[0] + "%");
        SeekBar opacitySeek = new SeekBar(this);
        opacitySeek.setMax(95);
        opacitySeek.setProgress(opacity[0] - 5);
        opacitySeek.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                opacity[0] = progress + 5;
                working.opacity = opacity[0];
                opacityLabel.setText("\u900f\u660e\u5ea6\uff1a" + opacity[0] + "%");
            }
        });
        content.addView(opacityLabel);
        content.addView(opacitySeek);

        String[] sizeIds = mGamepad.getControlSizeIds();
        int[] sizeValues = new int[sizeIds.length];
        for (int i = 0; i < sizeIds.length; i++) {
            sizeValues[i] = mGamepad.getControlSizeDp(sizeIds[i]);
        }
        addGamepadSizeControls(content, sizeIds, sizeValues);

        Switch diagonalSwitch = new Switch(this);
        diagonalSwitch.setText("\u65b9\u5411\u952e\u5141\u8bb8\u659c\u5411\u79fb\u52a8");
        diagonalSwitch.setChecked(Boolean.TRUE.equals(working.diagonalMovement));
        diagonalSwitch.setOnCheckedChangeListener((buttonView, checked) -> working.diagonalMovement = checked);
        content.addView(diagonalSwitch);

        TextView addCustomButton = new TextView(this);
        addCustomButton.setText("\u6dfb\u52a0\u81ea\u5b9a\u4e49\u6309\u952e");
        addCustomButton.setGravity(Gravity.CENTER);
        addCustomButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        addCustomButton.setOnClickListener(view -> showAddCustomButtonDialog());
        content.addView(addCustomButton);

        new AlertDialog.Builder(this)
                .setTitle("\u865a\u62df\u6309\u952e\u8bbe\u7f6e")
                .setView(wrapInScrollView(content))
                .setPositiveButton("\u5e94\u7528", (dialog, which) -> {
                    working.opacity = opacity[0];
                    applyConfigFields(mGamepadConfig, working);
                    mGamepad.setOpacity(opacity[0]);
                    for (int i = 0; i < sizeIds.length; i++) {
                        mGamepad.setControlSizeDp(sizeIds[i], sizeValues[i]);
                    }
                    working.layout = mGamepad.exportLayout();
                    mGamepadConfig = working;
                    MkxpLauncher.INSTANCE.saveGamepadLayout(this, working.layout);
                    reinstallGamepad();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void addGamepadSizeControls(LinearLayout content, String[] sizeIds, int[] sizeValues) {
        TextView title = new TextView(this);
        title.setText("\u5355\u4e2a\u6309\u952e\u5927\u5c0f");
        title.setTextSize(16f);
        title.setPadding(0, dp(12), 0, dp(4));
        content.addView(title);

        for (int i = 0; i < sizeIds.length; i++) {
            final int index = i;
            String id = sizeIds[index];
            int min = mGamepad.getMinControlSizeDp(id);
            int max = mGamepad.getMaxControlSizeDp(id);
            sizeValues[index] = clamp(sizeValues[index], min, max);

            TextView label = new TextView(this);
            label.setText(mGamepad.getControlLabel(id) + "\uff1a" + sizeValues[index] + "dp");
            SeekBar seekBar = new SeekBar(this);
            seekBar.setMax(max - min);
            seekBar.setProgress(sizeValues[index] - min);
            seekBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) {
                        return;
                    }
                    sizeValues[index] = min + progress;
                    label.setText(mGamepad.getControlLabel(id) + "\uff1a" + sizeValues[index] + "dp");
                }
            });
            content.addView(label);
            content.addView(seekBar);
        }
    }

    private void showAddCustomButtonDialog() {
        if (!ensureGamepadReady()) {
            return;
        }

        int[] keyCodes = commonKeyCodes();
        String[] labels = commonKeyLabels(keyCodes);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(12), dp(24), 0);
        content.addView(spinner);

        TextView sizeLabel = new TextView(this);
        final int[] sizeDp = new int[] { 46 };
        sizeLabel.setText("\u6309\u952e\u5927\u5c0f\uff1a" + sizeDp[0] + "dp");
        SeekBar sizeSeek = new SeekBar(this);
        sizeSeek.setMax(96);
        sizeSeek.setProgress(sizeDp[0] - 24);
        sizeSeek.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                sizeDp[0] = progress + 24;
                sizeLabel.setText("\u6309\u952e\u5927\u5c0f\uff1a" + sizeDp[0] + "dp");
            }
        });
        content.addView(sizeLabel);
        content.addView(sizeSeek);

        new AlertDialog.Builder(this)
                .setTitle("\u6dfb\u52a0\u81ea\u5b9a\u4e49\u6309\u952e")
                .setView(content)
                .setPositiveButton("\u6dfb\u52a0", (dialog, which) -> {
                    int index = spinner.getSelectedItemPosition();
                    int keyCode = keyCodes[Math.max(0, index)];
                    String label = keyLabel(keyCode);
                    mGamepad.addCustomButton(label, keyCode, sizeDp[0]);
                    mGamepadLayoutEditing = true;
                    mGamepad.setLayoutEditMode(true);
                    mGamepadConfig.layout = mGamepad.exportLayout();
                    MkxpLauncher.INSTANCE.saveGamepadLayout(this, mGamepadConfig.layout);
                    Toast.makeText(this, "\u5df2\u6dfb\u52a0\uff0c\u62d6\u52a8\u5230\u4f4d\u7f6e\u540e\u8bf7\u4fdd\u5b58\u5e03\u5c40", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("\u53d6\u6d88", null)
                .show();
    }

    private void saveGamepadLayout() {
        String layout = mGamepad.exportLayout();
        mGamepadConfig.layout = layout;
        MkxpLauncher.INSTANCE.saveGamepadLayout(this, layout);
        mGamepadLayoutEditing = false;
        mGamepad.setLayoutEditMode(false);
        Toast.makeText(this, "\u6309\u952e\u5e03\u5c40\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
    }

    private boolean ensureGamepadReady() {
        if (!VIRTUAL_GAMEPAD) {
            Toast.makeText(this, "\u865a\u62df\u6309\u952e\u672a\u542f\u7528", Toast.LENGTH_SHORT).show();
            return false;
        }
        installGamepad();
        if (!mGamepadAttached) {
            return false;
        }
        if (mGamepadInvisible) {
            mGamepad.showView();
            mGamepadInvisible = false;
        }
        return true;
    }

    private void reinstallGamepad() {
        boolean wasAttached = mGamepadAttached;
        mGamepad.detach();
        mGamepadAttached = false;
        if (wasAttached || VIRTUAL_GAMEPAD) {
            installGamepad();
        }
        bringGameOverlaysToFront();
    }

    private ScrollView wrapInScrollView(View content) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        return scrollView;
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

    private GamepadConfig copyGamepadConfig(GamepadConfig source) {
        GamepadConfig config = new GamepadConfig();
        config.opacity = source.opacity;
        config.scale = source.scale;
        config.diagonalMovement = source.diagonalMovement;
        config.joystickMode = source.joystickMode;
        config.layout = source.layout;
        config.keycodeA = source.keycodeA;
        config.keycodeB = source.keycodeB;
        config.keycodeC = source.keycodeC;
        config.keycodeX = source.keycodeX;
        config.keycodeY = source.keycodeY;
        config.keycodeZ = source.keycodeZ;
        config.keycodeL = source.keycodeL;
        config.keycodeR = source.keycodeR;
        config.keycodeCTRL = source.keycodeCTRL;
        config.keycodeALT = source.keycodeALT;
        config.keycodeSHIFT = source.keycodeSHIFT;
        config.keycodeRUN = source.keycodeRUN;
        config.physicalMappingEnabled = source.physicalMappingEnabled;
        config.physicalBackAsB = source.physicalBackAsB;
        config.physicalKeycodeA = source.physicalKeycodeA;
        config.physicalKeycodeB = source.physicalKeycodeB;
        config.physicalKeycodeX = source.physicalKeycodeX;
        config.physicalKeycodeY = source.physicalKeycodeY;
        config.physicalKeycodeL1 = source.physicalKeycodeL1;
        config.physicalKeycodeR1 = source.physicalKeycodeR1;
        config.physicalKeycodeL2 = source.physicalKeycodeL2;
        config.physicalKeycodeR2 = source.physicalKeycodeR2;
        config.physicalKeycodeStart = source.physicalKeycodeStart;
        config.physicalKeycodeSelect = source.physicalKeycodeSelect;
        config.physicalKeycodeRun = source.physicalKeycodeRun;
        return config;
    }

    private void applyConfigFields(GamepadConfig target, GamepadConfig source) {
        target.opacity = source.opacity;
        target.scale = source.scale;
        target.diagonalMovement = source.diagonalMovement;
        target.joystickMode = source.joystickMode;
        target.keycodeA = source.keycodeA;
        target.keycodeB = source.keycodeB;
        target.keycodeC = source.keycodeC;
        target.keycodeX = source.keycodeX;
        target.keycodeY = source.keycodeY;
        target.keycodeZ = source.keycodeZ;
        target.keycodeL = source.keycodeL;
        target.keycodeR = source.keycodeR;
        target.keycodeCTRL = source.keycodeCTRL;
        target.keycodeALT = source.keycodeALT;
        target.keycodeSHIFT = source.keycodeSHIFT;
        target.keycodeRUN = source.keycodeRUN;
        target.physicalMappingEnabled = source.physicalMappingEnabled;
        target.physicalBackAsB = source.physicalBackAsB;
        target.physicalKeycodeA = source.physicalKeycodeA;
        target.physicalKeycodeB = source.physicalKeycodeB;
        target.physicalKeycodeX = source.physicalKeycodeX;
        target.physicalKeycodeY = source.physicalKeycodeY;
        target.physicalKeycodeL1 = source.physicalKeycodeL1;
        target.physicalKeycodeR1 = source.physicalKeycodeR1;
        target.physicalKeycodeL2 = source.physicalKeycodeL2;
        target.physicalKeycodeR2 = source.physicalKeycodeR2;
        target.physicalKeycodeStart = source.physicalKeycodeStart;
        target.physicalKeycodeSelect = source.physicalKeycodeSelect;
        target.physicalKeycodeRun = source.physicalKeycodeRun;
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
        config.keycodeRUN = getIntent().getIntExtra(MkxpLauncher.EXTRA_GAMEPAD_KEY_RUN, KeyEvent.KEYCODE_SHIFT_LEFT);
        config.physicalMappingEnabled = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_MAPPING, true);
        config.physicalBackAsB = getIntent().getBooleanExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_BACK_AS_B, true);
        config.physicalKeycodeA = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_A, KeyEvent.KEYCODE_Z);
        config.physicalKeycodeB = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_B, KeyEvent.KEYCODE_X);
        config.physicalKeycodeX = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_X, KeyEvent.KEYCODE_A);
        config.physicalKeycodeY = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_Y, KeyEvent.KEYCODE_S);
        config.physicalKeycodeL1 = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_L1, KeyEvent.KEYCODE_Q);
        config.physicalKeycodeR1 = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_R1, KeyEvent.KEYCODE_W);
        config.physicalKeycodeL2 = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_L2, KeyEvent.KEYCODE_PAGE_UP);
        config.physicalKeycodeR2 = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_R2, KeyEvent.KEYCODE_PAGE_DOWN);
        config.physicalKeycodeStart = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_START, KeyEvent.KEYCODE_ENTER);
        config.physicalKeycodeSelect = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_SELECT, KeyEvent.KEYCODE_ESCAPE);
        config.physicalKeycodeRun = getIntent().getIntExtra(MkxpLauncher.EXTRA_PHYSICAL_GAMEPAD_KEY_RUN, KeyEvent.KEYCODE_SHIFT_LEFT);
        config.layout = stringExtra(MkxpLauncher.EXTRA_GAMEPAD_LAYOUT);
        return config;
    }

    private String stringExtra(String name) {
        String value = getIntent().getStringExtra(name);
        return value == null ? "" : value;
    }

    private int clamp(Integer value, int min, int max) {
        int safeValue = value == null ? min : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(0, Math.min(99999999, Integer.parseInt(value.trim())));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String keyLabel(int keyCode) {
        return KeyEvent.keyCodeToString(keyCode)
                .replace("KEYCODE_", "")
                .replace("_LEFT", "")
                .replace("_RIGHT", "");
    }

    private String[] commonKeyLabels(int[] keyCodes) {
        String[] labels = new String[keyCodes.length];
        for (int i = 0; i < keyCodes.length; i++) {
            labels[i] = keyLabel(keyCodes[i]);
        }
        return labels;
    }

    private int[] commonKeyCodes() {
        return new int[] {
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
                KeyEvent.KEYCODE_TAB
        };
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
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
