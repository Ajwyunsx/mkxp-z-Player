package com.hatkid.mkxpz.gamepad;

import android.view.KeyEvent;

public class GamepadConfig
{
    /** In-screen gamepad settings **/

    // Opacity of view elements in percentage (default: 30)
    public Integer opacity = 30;

    // View elements scale in percentage (default: 100)
    public Integer scale = 100;

    // Whether use diagonal (8-way) movement on D-Pad (default: false)
    public Boolean diagonalMovement = false;

    // Saved draggable layout, stored as percentage-based positions.
    public String layout = "";

    /** Key bindings for each RGSS input **/
    public Integer keycodeA = KeyEvent.KEYCODE_Z;
    public Integer keycodeB = KeyEvent.KEYCODE_X;
    public Integer keycodeC = KeyEvent.KEYCODE_C;
    public Integer keycodeX = KeyEvent.KEYCODE_A;
    public Integer keycodeY = KeyEvent.KEYCODE_S;
    public Integer keycodeZ = KeyEvent.KEYCODE_D;
    public Integer keycodeL = KeyEvent.KEYCODE_Q;
    public Integer keycodeR = KeyEvent.KEYCODE_W;
    public Integer keycodeCTRL = KeyEvent.KEYCODE_CTRL_LEFT;
    public Integer keycodeALT = KeyEvent.KEYCODE_ALT_LEFT;
    public Integer keycodeSHIFT = KeyEvent.KEYCODE_SHIFT_LEFT;
}
