package com.hatkid.mkxpz.gamepad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.util.AttributeSet;

import com.hatkid.mkxpz.utils.DirectionUtils;
import com.hatkid.mkxpz.utils.DirectionUtils.Direction;

public class GamepadJoystick extends View
{
    public Boolean isDiagonal = false;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float mThumbX = 0.5f;
    private float mThumbY = 0.5f;
    private int mPadOpacity = 30;
    private Direction mDirection = Direction.UNKNOWN;

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

    public GamepadJoystick(Context context)
    {
        super(context);
        init();
    }

    public GamepadJoystick(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public GamepadJoystick(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init();
    }

    public GamepadJoystick(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init()
    {
        setClickable(true);
        setFocusable(false);
    }

    public void setOnKeyDownListener(OnKeyDownListener onKeyDownListener)
    {
        mOnKeyDownListener = onKeyDownListener;
    }

    public void setOnKeyUpListener(OnKeyUpListener onKeyUpListener)
    {
        mOnKeyUpListener = onKeyUpListener;
    }

    public void setPadOpacity(int opacity)
    {
        mPadOpacity = Math.max(5, Math.min(100, opacity));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float baseRadius = Math.min(width, height) * 0.46f;
        float ringRadius = baseRadius * 0.72f;
        float thumbRadius = baseRadius * 0.32f;
        int fillAlpha = Math.round(mPadOpacity * 1.3f);
        int strokeAlpha = Math.round(mPadOpacity * 2.2f);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.argb(Math.min(190, fillAlpha), 10, 12, 16));
        canvas.drawCircle(centerX, centerY, baseRadius, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Math.max(2f, baseRadius * 0.04f));
        mPaint.setColor(Color.argb(Math.min(230, strokeAlpha), 255, 255, 255));
        canvas.drawCircle(centerX, centerY, ringRadius, mPaint);
        canvas.drawCircle(centerX, centerY, baseRadius, mPaint);

        float knobX = mThumbX * width;
        float knobY = mThumbY * height;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.argb(Math.min(240, strokeAlpha), 255, 255, 255));
        canvas.drawCircle(knobX, knobY, thumbRadius, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Math.max(1f, baseRadius * 0.025f));
        mPaint.setColor(Color.argb(Math.min(180, strokeAlpha), 0, 0, 0));
        canvas.drawCircle(knobX, knobY, thumbRadius, mPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent evt)
    {
        switch (evt.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateThumb(evt.getX(), evt.getY());
                updateDirection(evt.getX(), evt.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                resetThumb();
                releaseDirection();
                return true;

            default:
                return true;
        }
    }

    private void updateThumb(float x, float y)
    {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float maxDistance = Math.min(getWidth(), getHeight()) * 0.34f;
        float dx = x - centerX;
        float dy = y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > maxDistance && distance > 0) {
            dx = dx / distance * maxDistance;
            dy = dy / distance * maxDistance;
        }

        mThumbX = (centerX + dx) / Math.max(1, getWidth());
        mThumbY = (centerY + dy) / Math.max(1, getHeight());
        invalidate();
    }

    private void resetThumb()
    {
        mThumbX = 0.5f;
        mThumbY = 0.5f;
        invalidate();
    }

    private void updateDirection(float x, float y)
    {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float dx = x - centerX;
        float dy = y - centerY;
        float deadZone = Math.min(getWidth(), getHeight()) * 0.14f;

        Direction next = Direction.UNKNOWN;
        if ((dx * dx + dy * dy) >= deadZone * deadZone) {
            next = DirectionUtils.getAngle(centerX, x, centerY, y, isDiagonal);
        }

        if (mDirection == next)
            return;

        releaseDirection();
        mDirection = next;
        pressDirection(next);
    }

    private void releaseDirection()
    {
        pressOrReleaseDirection(mDirection, false);
        mDirection = Direction.UNKNOWN;
    }

    private void pressDirection(Direction direction)
    {
        pressOrReleaseDirection(direction, true);
    }

    private void pressOrReleaseDirection(Direction direction, boolean press)
    {
        if (direction == null || direction == Direction.UNKNOWN)
            return;

        switch (direction)
        {
            case UP:
                send(KeyEvent.KEYCODE_DPAD_UP, press);
                break;
            case UP_RIGHT:
                send(KeyEvent.KEYCODE_DPAD_UP, press);
                send(KeyEvent.KEYCODE_DPAD_RIGHT, press);
                break;
            case RIGHT:
                send(KeyEvent.KEYCODE_DPAD_RIGHT, press);
                break;
            case DOWN_RIGHT:
                send(KeyEvent.KEYCODE_DPAD_DOWN, press);
                send(KeyEvent.KEYCODE_DPAD_RIGHT, press);
                break;
            case DOWN:
                send(KeyEvent.KEYCODE_DPAD_DOWN, press);
                break;
            case DOWN_LEFT:
                send(KeyEvent.KEYCODE_DPAD_DOWN, press);
                send(KeyEvent.KEYCODE_DPAD_LEFT, press);
                break;
            case LEFT:
                send(KeyEvent.KEYCODE_DPAD_LEFT, press);
                break;
            case UP_LEFT:
                send(KeyEvent.KEYCODE_DPAD_UP, press);
                send(KeyEvent.KEYCODE_DPAD_LEFT, press);
                break;
        }
    }

    private void send(int key, boolean press)
    {
        if (press)
            mOnKeyDownListener.onKeyDown(key);
        else
            mOnKeyUpListener.onKeyUp(key);
    }
}
