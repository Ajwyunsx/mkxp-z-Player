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
    private View gpadActionLayout;
    private View gpadMiscLeftLayout;
    private View gpadMiscRightLayout;
    private View gpadModLayout;
    private boolean mLayoutEditMode = false;

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
        // Setup layout of in-screen gamepad
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.gamepad_layout, viewGroup);
        mGamepadLayout = layout.findViewById(R.id.gamepad_layout);

        if (mInvisible) {
            mGamepadLayout.setAlpha(0);
        }

        // Setup D-Pad and buttons
        gpadDPad = layout.findViewById(R.id.dpad);
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

        // Setup in-screen gamepad listeners
        mGamepadLayout.setOnTouchListener((view, motionEvent) -> false);
        gpadDPad.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        gpadDPad.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));

        // Configure gamepad
        gpadDPad.isDiagonal = mGamepadConfig.diagonalMovement;

        // Setup buttons for gamepad
        initGamepadButtons();

        // Apply scale and opacity from gamepad config
        ViewUtils.resize(mGamepadLayout, mGamepadConfig.scale);
        ViewUtils.changeOpacity(mGamepadLayout, mGamepadConfig.opacity);
        mGamepadLayout.post(() -> {
            applyLayout(mGamepadConfig.layout);
            setLayoutEditMode(mLayoutEditMode);
        });
    }

    public void detach()
    {
        if (mGamepadLayout != null && mGamepadLayout.getParent() instanceof ViewGroup) {
            ((ViewGroup) mGamepadLayout.getParent()).removeView(mGamepadLayout);
        }

        mGamepadLayout = null;
        gpadDPad = null;
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
        bindDragTarget(gpadActionLayout, enabled);
        bindDragTarget(gpadMiscLeftLayout, enabled);
        bindDragTarget(gpadMiscRightLayout, enabled);
        bindDragTarget(gpadModLayout, enabled);
    }

    public String exportLayout()
    {
        if (mGamepadLayout == null)
            return mGamepadConfig != null && mGamepadConfig.layout != null ? mGamepadConfig.layout : "";

        String layout = ""
            + encodeLayoutItem("dpad", gpadDPad)
            + encodeLayoutItem("action", gpadActionLayout)
            + encodeLayoutItem("left", gpadMiscLeftLayout)
            + encodeLayoutItem("right", gpadMiscRightLayout)
            + encodeLayoutItem("mod", gpadModLayout);
        if (mGamepadConfig != null)
            mGamepadConfig.layout = layout;
        return layout;
    }

    private String encodeLayoutItem(String name, View view)
    {
        if (view == null || mGamepadLayout.getWidth() <= 0 || mGamepadLayout.getHeight() <= 0)
            return "";

        int xPercent = Math.round((view.getX() / Math.max(1, mGamepadLayout.getWidth())) * 10000f);
        int yPercent = Math.round((view.getY() / Math.max(1, mGamepadLayout.getHeight())) * 10000f);
        return name + "=" + xPercent + "," + yPercent + ";";
    }

    private void applyLayout(String layout)
    {
        if (layout == null || layout.trim().isEmpty() || mGamepadLayout == null)
            return;

        String[] items = layout.split(";");
        for (String item : items) {
            String[] pair = item.split("=");
            if (pair.length != 2)
                continue;

            View target = layoutTarget(pair[0]);
            if (target == null)
                continue;

            String[] coords = pair[1].split(",");
            if (coords.length != 2)
                continue;

            try {
                int xPercent = Integer.parseInt(coords[0]);
                int yPercent = Integer.parseInt(coords[1]);
                moveTargetToPercent(target, xPercent, yPercent);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private View layoutTarget(String name)
    {
        switch (name) {
            case "dpad":
                return gpadDPad;
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

        int maxX = Math.max(0, mGamepadLayout.getWidth() - target.getWidth());
        int maxY = Math.max(0, mGamepadLayout.getHeight() - target.getHeight());
        int clampedX = Math.max(0, Math.min(x, maxX));
        int clampedY = Math.max(0, Math.min(y, maxY));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
            target.getWidth() > 0 ? target.getWidth() : target.getLayoutParams().width,
            target.getHeight() > 0 ? target.getHeight() : target.getLayoutParams().height
        );
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
        // Prepare label for gamepad button
        String btnLabel = KeyEvent.keyCodeToString(keycode)
            .replace("KEYCODE_", "")
            .replace("_LEFT", "")
            .replace("_RIGHT", "");

        // Set gamepad button
        gpadBtn.setForegroundText(btnLabel);
        gpadBtn.setKey(keycode);
        gpadBtn.setOnKeyDownListener(key -> mOnKeyDownListener.onKeyDown(key));
        gpadBtn.setOnKeyUpListener(key -> mOnKeyUpListener.onKeyUp(key));
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
