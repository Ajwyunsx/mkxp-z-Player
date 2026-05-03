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

    // Whether use a drawn joystick instead of the classic D-Pad (default: false)
    public Boolean joystickMode = false;

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
    public Integer keycodeRUN = KeyEvent.KEYCODE_SHIFT_LEFT;

    /** Physical Android controller mappings. Values are target keyboard keycodes sent to the game. */
    public Boolean physicalMappingEnabled = true;
    public Boolean physicalBackAsB = true;
    public Integer physicalKeycodeA = KeyEvent.KEYCODE_Z;
    public Integer physicalKeycodeB = KeyEvent.KEYCODE_X;
    public Integer physicalKeycodeX = KeyEvent.KEYCODE_A;
    public Integer physicalKeycodeY = KeyEvent.KEYCODE_S;
    public Integer physicalKeycodeL1 = KeyEvent.KEYCODE_Q;
    public Integer physicalKeycodeR1 = KeyEvent.KEYCODE_W;
    public Integer physicalKeycodeL2 = KeyEvent.KEYCODE_PAGE_UP;
    public Integer physicalKeycodeR2 = KeyEvent.KEYCODE_PAGE_DOWN;
    public Integer physicalKeycodeStart = KeyEvent.KEYCODE_ENTER;
    public Integer physicalKeycodeSelect = KeyEvent.KEYCODE_ESCAPE;
    public Integer physicalKeycodeRun = KeyEvent.KEYCODE_SHIFT_LEFT;
}
