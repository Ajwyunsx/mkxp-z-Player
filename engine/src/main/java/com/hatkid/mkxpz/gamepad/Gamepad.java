package com.hatkid.mkxpz.gamepad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import io.github.mkxpz.engine.R;
import com.hatkid.mkxpz.utils.ViewUtils;

public class Gamepad
{
    private GamepadConfig mGamepadConfig = null;
    private boolean mInvisible = false;

    private OnKeyDownListener mOnKeyDownListener = key -> {};
    private OnKeyUpListener mOnKeyUpListener = key -> {};

    public interface OnKeyDownListener
    {
        void onKeyDown(int key);
    }

    public interface OnKeyUpListener
    {
        void onKeyUp(int key);
    }

    private static class CustomButton
    {
        final GamepadButton view;
        final String label;
        final int keyCode;
        final int sizeDp;

        CustomButton(GamepadButton view, String label, int keyCode, int sizeDp)
        {
            this.view = view;
            this.label = label;
            this.keyCode = keyCode;
            this.sizeDp = sizeDp;
        }
    }

    public void setOnKeyDownListener(OnKeyDownListener onKeyDownListener)
    {
        mOnKeyDownListener = onKeyDownListener;
    }

    public void setOnKeyUpListener(OnKeyUpListener onKeyUpListener)
    {
        mOnKeyUpListener = onKeyUpListener;
    }

    private RelativeLayout mGamepadLayout;
    private GamepadDPad gpadDPad;
    private GamepadJoystick gpadJoystick;
    private View gpadActionLayout;
    private View gpadMiscLeftLayout;
    private View gpadMiscRightLayout;
    private View gpadModLayout;
    private boolean mLayoutEditMode = false;
    private final List<CustomButton> mCustomButtons = new ArrayList<>();

    // Gamepad buttons
    private GamepadButton gpadBtnA;
    private GamepadButton gpadBtnB;
    private GamepadButton gpadBtnC;
    private GamepadButton gpadBtnX;
    private GamepadButton gpadBtnY;
    private GamepadButton gpadBtnZ;
    private GamepadButton gpadBtnL;
    private GamepadButton gpadBtnR;
    private GamepadButton gpadBtnCTRL;
    private GamepadButton gpadBtnALT;
    private GamepadButton gpadBtnSHIFT;

    public void init(GamepadConfig gpadConfig, boolean invisible)
    {
        mGamepadConfig = gpadConfig;
        mInvisible = invisible;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attachTo(Context context, ViewGroup viewGroup)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.gamepad_layout, viewGroup);
        mGamepadLayout = layout.findViewById(R.id.gamepad_layout);

        if (mInvisible) {
            mGamepadLayout.setAlpha(0);
        }

        gpadDPad = layout.findViewById(R.id.dpad);
        gpadJoystick = layout.findViewById(R.id.joystick);
        gpadActionLayout = layout.findViewById(R.id.buttons_action_layout);
        gpadMiscLeftLayout = layout.findViewById(R.id.buttons_misc_left_layout);
        gpadMiscRightLayout = layout.findViewById(R.id.buttons_misc_right_layout);
        gpadModLayout = layout.findViewById(R.id.buttons_mod_layout);
        gpadBtnA = layout.findViewById(R.id.button_A);
        gpadBtnB = layout.findViewById(R.id.button_B);
        gpadBtnC = layout.findViewById(R.id.button_C);
        gpadBtnX = layout.findViewById(R.id.button_X);
        gpadBtnY = layout.findViewById(R.id.button_Y);
        gpadBtnZ = layout.findViewById(R.id.button_Z);
        gpadBtnL = layout.findViewById(R.id.button_L);
        gpadBtnR = layout.findViewById(R.id.button_R);
        gpadBtnCTRL = layout.findViewById(R.id.button_CTRL);
        gpadBtnALT = layout.findViewById(R.id.button_ALT);
        gpadBtnSHIFT = layout.findViewById(R.id.button_SHIFT);

        mGamepadLayout.setOnTouchListener((view, motionEvent) -> false);
        gpadDPad.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        gpadDPad.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));
        gpadJoystick.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        gpadJoystick.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));

        gpadDPad.isDiagonal = mGamepadConfig.diagonalMovement;
        gpadJoystick.isDiagonal = mGamepadConfig.diagonalMovement;

        initGamepadButtons();
        applySavedSettings(mGamepadConfig.layout);

        ViewUtils.resize(mGamepadLayout, safeInt(mGamepadConfig.scale, 100, 60, 160));
        applyOpacity();
        mGamepadLayout.post(() -> {
            applyLayout(mGamepadConfig.layout);
            applyInputMode();
            setLayoutEditMode(mLayoutEditMode);
        });
    }

    public void detach()
    {
        clearCustomButtons();
        if (mGamepadLayout != null && mGamepadLayout.getParent() instanceof ViewGroup) {
            ((ViewGroup) mGamepadLayout.getParent()).removeView(mGamepadLayout);
        }

        mGamepadLayout = null;
        gpadDPad = null;
        gpadJoystick = null;
        gpadActionLayout = null;
        gpadMiscLeftLayout = null;
        gpadMiscRightLayout = null;
        gpadModLayout = null;
        gpadBtnA = null;
        gpadBtnB = null;
        gpadBtnC = null;
        gpadBtnX = null;
        gpadBtnY = null;
        gpadBtnZ = null;
        gpadBtnL = null;
        gpadBtnR = null;
        gpadBtnCTRL = null;
        gpadBtnALT = null;
        gpadBtnSHIFT = null;
    }

    public void setLayoutEditMode(boolean enabled)
    {
        mLayoutEditMode = enabled;
        if (mGamepadLayout == null)
            return;

        bindDragTarget(gpadDPad, enabled);
        bindDragTarget(gpadJoystick, enabled);
        bindDragTarget(gpadActionLayout, enabled);
        bindDragTarget(gpadMiscLeftLayout, enabled);
        bindDragTarget(gpadMiscRightLayout, enabled);
        bindDragTarget(gpadModLayout, enabled);

        for (CustomButton customButton : mCustomButtons) {
            bindDragTarget(customButton.view, enabled);
        }
    }

    public boolean isJoystickMode()
    {
        return mGamepadConfig != null && Boolean.TRUE.equals(mGamepadConfig.joystickMode);
    }

    public boolean toggleMovementMode()
    {
        if (mGamepadConfig == null)
            return false;

        mGamepadConfig.joystickMode = !Boolean.TRUE.equals(mGamepadConfig.joystickMode);
        if (Boolean.TRUE.equals(mGamepadConfig.joystickMode) && gpadJoystick != null && gpadDPad != null) {
            moveTargetTo(gpadJoystick, Math.round(gpadDPad.getX()), Math.round(gpadDPad.getY()));
        }
        applyInputMode();
        exportLayout();
        return Boolean.TRUE.equals(mGamepadConfig.joystickMode);
    }

    public void setOpacity(int opacity)
    {
        if (mGamepadConfig != null)
            mGamepadConfig.opacity = Math.max(5, Math.min(100, opacity));
        applyOpacity();
    }

    public void setScale(int scale)
    {
        if (mGamepadConfig != null)
            mGamepadConfig.scale = Math.max(60, Math.min(160, scale));
    }

    public void addCustomButton(String label, int keyCode)
    {
        addCustomButton(label, keyCode, 46);
    }

    public void addCustomButton(String label, int keyCode, int sizeDp)
    {
        if (mGamepadLayout == null)
            return;

        GamepadButton button = createCustomButton(label, keyCode, sizeDp);
        placeNewCustomButton(button);
        bindDragTarget(button, mLayoutEditMode);
        applyOpacity();
        exportLayout();
    }

    public String exportLayout()
    {
        if (mGamepadLayout == null)
            return mGamepadConfig != null && mGamepadConfig.layout != null ? mGamepadConfig.layout : "";

        StringBuilder layout = new StringBuilder();
        layout.append("opacity=")
            .append(safeInt(mGamepadConfig.opacity, 30, 5, 100))
            .append(";");
        layout.append("scale=")
            .append(safeInt(mGamepadConfig.scale, 100, 60, 160))
            .append(";");
        layout.append("mode=")
            .append(Boolean.TRUE.equals(mGamepadConfig.joystickMode) ? "joystick" : "dpad")
            .append(";");
        layout.append(encodeLayoutItem("dpad", gpadDPad));
        layout.append(encodeLayoutItem("joystick", gpadJoystick));
        layout.append(encodeLayoutItem("action", gpadActionLayout));
        layout.append(encodeLayoutItem("left", gpadMiscLeftLayout));
        layout.append(encodeLayoutItem("right", gpadMiscRightLayout));
        layout.append(encodeLayoutItem("mod", gpadModLayout));

        for (int i = 0; i < mCustomButtons.size(); i++) {
            layout.append(encodeCustomButton("custom" + i, mCustomButtons.get(i)));
        }

        String result = layout.toString();
        if (mGamepadConfig != null)
            mGamepadConfig.layout = result;
        return result;
    }

    private String encodeLayoutItem(String name, View view)
    {
        if (view == null || mGamepadLayout == null || mGamepadLayout.getWidth() <= 0 || mGamepadLayout.getHeight() <= 0)
            return "";

        int xPercent = Math.round((view.getX() / Math.max(1, mGamepadLayout.getWidth())) * 10000f);
        int yPercent = Math.round((view.getY() / Math.max(1, mGamepadLayout.getHeight())) * 10000f);
        return name + "=" + xPercent + "," + yPercent + ";";
    }

    private String encodeCustomButton(String name, CustomButton button)
    {
        if (button == null || button.view == null || mGamepadLayout == null || mGamepadLayout.getWidth() <= 0 || mGamepadLayout.getHeight() <= 0)
            return "";

        int xPercent = Math.round((button.view.getX() / Math.max(1, mGamepadLayout.getWidth())) * 10000f);
        int yPercent = Math.round((button.view.getY() / Math.max(1, mGamepadLayout.getHeight())) * 10000f);
        return name + "=" + xPercent + "," + yPercent + "," + button.keyCode + "," + button.sizeDp + "," + sanitizeLabel(button.label) + ";";
    }

    private void applyLayout(String layout)
    {
        clearCustomButtons();
        if (layout == null || layout.trim().isEmpty() || mGamepadLayout == null) {
            applyInputMode();
            return;
        }

        boolean joystickPositionSeen = false;
        String[] items = layout.split(";");
        for (String item : items) {
            String[] pair = item.split("=", 2);
            if (pair.length != 2)
                continue;

            String name = pair[0];
            String value = pair[1];

            if ("mode".equals(name)) {
                mGamepadConfig.joystickMode = "joystick".equals(value);
                continue;
            }
            if ("opacity".equals(name)) {
                mGamepadConfig.opacity = safeParsedInt(value, 30, 5, 100);
                continue;
            }
            if ("scale".equals(name)) {
                mGamepadConfig.scale = safeParsedInt(value, 100, 60, 160);
                continue;
            }

            if (name.startsWith("custom")) {
                applyCustomButton(value);
                continue;
            }

            View target = layoutTarget(name);
            if (target == null)
                continue;

            String[] coords = value.split(",");
            if (coords.length != 2)
                continue;

            try {
                int xPercent = Integer.parseInt(coords[0]);
                int yPercent = Integer.parseInt(coords[1]);
                moveTargetToPercent(target, xPercent, yPercent);
                if ("joystick".equals(name))
                    joystickPositionSeen = true;
            } catch (NumberFormatException ignored) {
            }
        }

        if (!joystickPositionSeen && gpadJoystick != null && gpadDPad != null) {
            moveTargetTo(gpadJoystick, Math.round(gpadDPad.getX()), Math.round(gpadDPad.getY()));
        }
        applyInputMode();
        applyOpacity();
    }

    private void applyCustomButton(String value)
    {
        String[] data = value.split(",", 5);
        if (data.length < 5)
            return;

        try {
            int xPercent = Integer.parseInt(data[0]);
            int yPercent = Integer.parseInt(data[1]);
            int keyCode = Integer.parseInt(data[2]);
            int sizeDp = Integer.parseInt(data[3]);
            String label = data[4];
            GamepadButton button = createCustomButton(label, keyCode, sizeDp);
            moveTargetToPercent(button, xPercent, yPercent);
        } catch (NumberFormatException ignored) {
        }
    }

    private View layoutTarget(String name)
    {
        switch (name) {
            case "dpad":
                return gpadDPad;
            case "joystick":
                return gpadJoystick;
            case "action":
                return gpadActionLayout;
            case "left":
                return gpadMiscLeftLayout;
            case "right":
                return gpadMiscRightLayout;
            case "mod":
                return gpadModLayout;
            default:
                return null;
        }
    }

    private void moveTargetToPercent(View target, int xPercent, int yPercent)
    {
        if (target == null || mGamepadLayout == null)
            return;

        int parentWidth = Math.max(1, mGamepadLayout.getWidth());
        int parentHeight = Math.max(1, mGamepadLayout.getHeight());
        int x = Math.round(parentWidth * (xPercent / 10000f));
        int y = Math.round(parentHeight * (yPercent / 10000f));
        moveTargetTo(target, x, y);
    }

    private void moveTargetTo(View target, int x, int y)
    {
        if (target == null || mGamepadLayout == null)
            return;

        int targetWidth = target.getWidth() > 0 ? target.getWidth() : target.getLayoutParams().width;
        int targetHeight = target.getHeight() > 0 ? target.getHeight() : target.getLayoutParams().height;
        int maxX = Math.max(0, mGamepadLayout.getWidth() - targetWidth);
        int maxY = Math.max(0, mGamepadLayout.getHeight() - targetHeight);
        int clampedX = Math.max(0, Math.min(x, maxX));
        int clampedY = Math.max(0, Math.min(y, maxY));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(targetWidth, targetHeight);
        params.leftMargin = clampedX;
        params.topMargin = clampedY;
        target.setLayoutParams(params);
    }

    private void bindDragTarget(View target, boolean enabled)
    {
        if (target == null)
            return;

        setDragHandler(target, target, enabled);
        if (target instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) target;
            for (int i = 0; i < group.getChildCount(); i++)
                bindDragChild(group.getChildAt(i), target, enabled);
        }
    }

    private void bindDragChild(View touchView, View target, boolean enabled)
    {
        setDragHandler(touchView, target, enabled);
        if (touchView instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) touchView;
            for (int i = 0; i < group.getChildCount(); i++)
                bindDragChild(group.getChildAt(i), target, enabled);
        }
    }

    private void setDragHandler(View touchView, View target, boolean enabled)
    {
        if (!enabled) {
            touchView.setOnTouchListener(null);
            return;
        }

        final float[] downRawX = new float[1];
        final float[] downRawY = new float[1];
        final int[] startLeft = new int[1];
        final int[] startTop = new int[1];

        touchView.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX[0] = event.getRawX();
                    downRawY[0] = event.getRawY();
                    startLeft[0] = Math.round(target.getX());
                    startTop[0] = Math.round(target.getY());
                    target.bringToFront();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int nextX = Math.round(startLeft[0] + event.getRawX() - downRawX[0]);
                    int nextY = Math.round(startTop[0] + event.getRawY() - downRawY[0]);
                    moveTargetTo(target, nextX, nextY);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    exportLayout();
                    return true;

                default:
                    return true;
            }
        });
    }

    private void setGamepadButtonKey(GamepadButton gpadBtn, Integer keycode)
    {
        String btnLabel = KeyEvent.keyCodeToString(keycode)
            .replace("KEYCODE_", "")
            .replace("_LEFT", "")
            .replace("_RIGHT", "");

        gpadBtn.setForegroundText(btnLabel);
        gpadBtn.setKey(keycode);
        gpadBtn.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        gpadBtn.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));
    }

    private GamepadButton createCustomButton(String label, int keyCode, int sizeDp)
    {
        GamepadButton button = new GamepadButton(mGamepadLayout.getContext());
        int clampedSizeDp = Math.max(28, Math.min(96, sizeDp));
        int sizePx = dp(clampedSizeDp);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(sizePx, sizePx);
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.gamepad_button_circle);
        button.setForegroundText(sanitizeLabel(label));
        button.setKey(keyCode);
        button.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        button.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));
        mGamepadLayout.addView(button, params);
        mCustomButtons.add(new CustomButton(button, sanitizeLabel(label), keyCode, clampedSizeDp));
        return button;
    }

    private void placeNewCustomButton(GamepadButton button)
    {
        int size = button.getLayoutParams().width;
        int index = Math.max(0, mCustomButtons.size() - 1);
        int left = Math.max(0, mGamepadLayout.getWidth() - size - dp(24 + index * 10));
        int top = Math.max(0, (mGamepadLayout.getHeight() / 2) - (size / 2) + dp(index * 10));
        moveTargetTo(button, left, top);
        button.bringToFront();
    }

    private void clearCustomButtons()
    {
        if (mGamepadLayout != null) {
            for (CustomButton customButton : mCustomButtons) {
                mGamepadLayout.removeView(customButton.view);
            }
        }
        mCustomButtons.clear();
    }

    private void applyInputMode()
    {
        if (gpadDPad == null || gpadJoystick == null || mGamepadConfig == null)
            return;

        boolean joystick = Boolean.TRUE.equals(mGamepadConfig.joystickMode);
        gpadDPad.setVisibility(joystick ? View.GONE : View.VISIBLE);
        gpadJoystick.setVisibility(joystick ? View.VISIBLE : View.GONE);
    }

    private void applyOpacity()
    {
        if (mGamepadLayout == null || mGamepadConfig == null)
            return;

        int opacity = safeInt(mGamepadConfig.opacity, 30, 5, 100);
        ViewUtils.changeOpacity(mGamepadLayout, opacity);
        if (gpadJoystick != null)
            gpadJoystick.setPadOpacity(opacity);
    }

    private void applySavedSettings(String layout)
    {
        if (layout == null || layout.trim().isEmpty() || mGamepadConfig == null)
            return;

        String[] items = layout.split(";");
        for (String item : items) {
            String[] pair = item.split("=", 2);
            if (pair.length != 2)
                continue;

            if ("mode".equals(pair[0])) {
                mGamepadConfig.joystickMode = "joystick".equals(pair[1]);
            } else if ("opacity".equals(pair[0])) {
                mGamepadConfig.opacity = safeParsedInt(pair[1], 30, 5, 100);
            } else if ("scale".equals(pair[0])) {
                mGamepadConfig.scale = safeParsedInt(pair[1], 100, 60, 160);
            }
        }
    }

    private int safeParsedInt(String value, int fallback, int min, int max)
    {
        try {
            return safeInt(Integer.parseInt(value), fallback, min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int safeInt(Integer value, int fallback, int min, int max)
    {
        int safeValue = value == null ? fallback : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private String sanitizeLabel(String label)
    {
        if (label == null || label.trim().isEmpty())
            return "KEY";
        return label.replace(";", "_").replace(",", "_").replace("=", "_").trim();
    }

    private int dp(int value)
    {
        return Math.round(value * mGamepadLayout.getResources().getDisplayMetrics().density);
    }

    public void showView()
    {
        if (mGamepadLayout != null) {
            if (mGamepadLayout.getAlpha() == 0)
                mGamepadLayout.setAlpha(1);

            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(250);
            anim.setFillAfter(true);
            mGamepadLayout.startAnimation(anim);
        }
    }

    public void hideView()
    {
        if (mGamepadLayout != null) {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(500);
            anim.setFillAfter(true);
            mGamepadLayout.startAnimation(anim);
        }
    }

    public void bringToFront()
    {
        if (mGamepadLayout != null) {
            mGamepadLayout.bringToFront();
            for (CustomButton customButton : mCustomButtons) {
                customButton.view.bringToFront();
            }
            mGamepadLayout.invalidate();
        }
    }

    private void initGamepadButtons()
    {
        setGamepadButtonKey(gpadBtnA, mGamepadConfig.keycodeA);
        setGamepadButtonKey(gpadBtnB, mGamepadConfig.keycodeB);
        setGamepadButtonKey(gpadBtnC, mGamepadConfig.keycodeC);
        setGamepadButtonKey(gpadBtnX, mGamepadConfig.keycodeX);
        setGamepadButtonKey(gpadBtnY, mGamepadConfig.keycodeY);
        setGamepadButtonKey(gpadBtnZ, mGamepadConfig.keycodeZ);
        setGamepadButtonKey(gpadBtnL, mGamepadConfig.keycodeL);
        setGamepadButtonKey(gpadBtnR, mGamepadConfig.keycodeR);
        setGamepadButtonKey(gpadBtnCTRL, mGamepadConfig.keycodeCTRL);
        setGamepadButtonKey(gpadBtnALT, mGamepadConfig.keycodeALT);
        setGamepadButtonKey(gpadBtnSHIFT, mGamepadConfig.keycodeSHIFT);
    }

    public boolean processGamepadEvent(KeyEvent evt)
    {
        InputDevice device = evt.getDevice();

        if (device == null)
            return false;

        int sources = device.getSources();

        if (
            ((sources & InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) &&
            ((sources & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD)
        )
            return false;

        int keycode = evt.getKeyCode();

        switch (evt.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mOnKeyDownListener.onKeyDown(keycode);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mOnKeyUpListener.onKeyUp(keycode);
                break;
        }

        return true;
    }

    public boolean processDPadEvent(MotionEvent evt)
    {
        InputDevice device = evt.getDevice();

        if (device == null)
            return false;

        int sources = device.getSources();

        if (((sources & InputDevice.SOURCE_DPAD) != InputDevice.SOURCE_DPAD))
            return false;

        float xAxis = evt.getAxisValue(MotionEvent.AXIS_HAT_X);
        float yAxis = evt.getAxisValue(MotionEvent.AXIS_HAT_Y);

        Integer keycode = null;

        if (Float.compare(yAxis, -1.0f) == 0)
            keycode = KeyEvent.KEYCODE_DPAD_UP;
        else if (Float.compare(yAxis, 1.0f) == 0)
            keycode = KeyEvent.KEYCODE_DPAD_DOWN;
        else if (Float.compare(xAxis, -1.0f) == 0)
            keycode = KeyEvent.KEYCODE_DPAD_LEFT;
        else if (Float.compare(xAxis, 1.0f) == 0)
            keycode = KeyEvent.KEYCODE_DPAD_RIGHT;

        if (keycode == null)
            return false;

        switch (evt.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mOnKeyDownListener.onKeyDown(keycode);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mOnKeyUpListener.onKeyUp(keycode);
                break;
        }

        return true;
    }
}
