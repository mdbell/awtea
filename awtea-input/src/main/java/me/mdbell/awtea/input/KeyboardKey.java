package me.mdbell.awtea.input;

import lombok.Getter;
import me.mdbell.awtea.util.EnumLookup;

import java.awt.event.KeyEvent;

@Getter
public enum KeyboardKey {
    // Arrow keys
    ARROW_LEFT("ArrowLeft", KeyEvent.VK_LEFT),
    ARROW_UP("ArrowUp", KeyEvent.VK_UP),
    ARROW_RIGHT("ArrowRight", KeyEvent.VK_RIGHT),
    ARROW_DOWN("ArrowDown", KeyEvent.VK_DOWN),

    // Control keys
    BACKSPACE("Backspace", KeyEvent.VK_BACK_SPACE),
    TAB("Tab", KeyEvent.VK_TAB, '\t'),
    ENTER("Enter", KeyEvent.VK_ENTER, '\n'),
    SHIFT_LEFT("ShiftLeft", KeyEvent.VK_SHIFT),
    SHIFT_RIGHT("ShiftRight", KeyEvent.VK_SHIFT),
    CONTROL_LEFT("ControlLeft", KeyEvent.VK_CONTROL),
    CONTROL_RIGHT("ControlRight", KeyEvent.VK_CONTROL),
    ALT_LEFT("AltLeft", KeyEvent.VK_ALT),
    ALT_RIGHT("AltRight", KeyEvent.VK_ALT),
    CAPS_LOCK("CapsLock", KeyEvent.VK_CAPS_LOCK),
    ESCAPE("Escape", KeyEvent.VK_ESCAPE),
    SPACE("Space", KeyEvent.VK_SPACE, ' '),
    PAGE_UP("PageUp", KeyEvent.VK_PAGE_UP),
    PAGE_DOWN("PageDown", KeyEvent.VK_PAGE_DOWN),
    END("End", KeyEvent.VK_END),
    HOME("Home", KeyEvent.VK_HOME),
    INSERT("Insert", KeyEvent.VK_INSERT),
    DELETE("Delete", KeyEvent.VK_DELETE),

    // Numbers
    NUM_1("Digit1", KeyEvent.VK_1, '1', '!'),
    NUM_2("Digit2", KeyEvent.VK_2, '2', '@'),
    NUM_3("Digit3", KeyEvent.VK_3, '3', '#'),
    NUM_4("Digit4", KeyEvent.VK_4, '4', '$'),
    NUM_5("Digit5", KeyEvent.VK_5, '5', '%'),
    NUM_6("Digit6", KeyEvent.VK_6, '6', '^'),
    NUM_7("Digit7", KeyEvent.VK_7, '7', '&'),
    NUM_8("Digit8", KeyEvent.VK_8, '8', '*'),
    NUM_9("Digit9", KeyEvent.VK_9, '9', '('),
    NUM_0("Digit0", KeyEvent.VK_0, '0', ')'),

    // Letters
    A("KeyA", KeyEvent.VK_A, 'a'),
    B("KeyB", KeyEvent.VK_B, 'b'),
    C("KeyC", KeyEvent.VK_C, 'c'),
    D("KeyD", KeyEvent.VK_D, 'd'),
    E("KeyE", KeyEvent.VK_E, 'e'),
    F("KeyF", KeyEvent.VK_F, 'f'),
    G("KeyG", KeyEvent.VK_G, 'g'),
    H("KeyH", KeyEvent.VK_H, 'h'),
    I("KeyI", KeyEvent.VK_I, 'i'),
    J("KeyJ", KeyEvent.VK_J, 'j'),
    K("KeyK", KeyEvent.VK_K, 'k'),
    L("KeyL", KeyEvent.VK_L, 'l'),
    M("KeyM", KeyEvent.VK_M, 'm'),
    N("KeyN", KeyEvent.VK_N, 'n'),
    O("KeyO", KeyEvent.VK_O, 'o'),
    P("KeyP", KeyEvent.VK_P, 'p'),
    Q("KeyQ", KeyEvent.VK_Q, 'q'),
    R("KeyR", KeyEvent.VK_R, 'r'),
    S("KeyS", KeyEvent.VK_S, 's'),
    T("KeyT", KeyEvent.VK_T, 't'),
    U("KeyU", KeyEvent.VK_U, 'u'),
    V("KeyV", KeyEvent.VK_V, 'v'),
    W("KeyW", KeyEvent.VK_W, 'w'),
    X("KeyX", KeyEvent.VK_X, 'x'),
    Y("KeyY", KeyEvent.VK_Y, 'y'),
    Z("KeyZ", KeyEvent.VK_Z, 'z'),

    // Symbols
    MINUS("Minus", KeyEvent.VK_MINUS, '-', '_'),
    EQUALS("Equal", KeyEvent.VK_EQUALS, '=', '+'),
    OPEN_BRACKET("BracketLeft", KeyEvent.VK_OPEN_BRACKET, '[', '{'),
    CLOSE_BRACKET("BracketRight", KeyEvent.VK_CLOSE_BRACKET, ']', '}'),
    SEMICOLON("Semicolon", KeyEvent.VK_SEMICOLON, ';', ':'),
    BACK_SLASH("Backslash", KeyEvent.VK_BACK_SLASH, '\\', '|'),

    QUOTE("Quote", KeyEvent.VK_QUOTE, '\'', '"'),
    COMMA("Comma", KeyEvent.VK_COMMA, ',', '<'),
    PERIOD("Period", KeyEvent.VK_PERIOD, '.', '>'),
    SLASH("Slash", KeyEvent.VK_SLASH, '/', '?'),

    // Numpad
    NUMPAD_0("Numpad0", KeyEvent.VK_NUMPAD0),
    NUMPAD_1("Numpad1", KeyEvent.VK_NUMPAD1),
    NUMPAD_2("Numpad2", KeyEvent.VK_NUMPAD2),
    NUMPAD_3("Numpad3", KeyEvent.VK_NUMPAD3),
    NUMPAD_4("Numpad4", KeyEvent.VK_NUMPAD4),
    NUMPAD_5("Numpad5", KeyEvent.VK_NUMPAD5),
    NUMPAD_6("Numpad6", KeyEvent.VK_NUMPAD6),
    NUMPAD_7("Numpad7", KeyEvent.VK_NUMPAD7),
    NUMPAD_8("Numpad8", KeyEvent.VK_NUMPAD8),
    NUMPAD_9("Numpad9", KeyEvent.VK_NUMPAD9),
    NUMPAD_MULTIPLY("NumpadMultiply", KeyEvent.VK_MULTIPLY),
    NUMPAD_ADD("NumpadAdd", KeyEvent.VK_ADD),
    NUMPAD_SUBTRACT("NumpadSubtract", KeyEvent.VK_SUBTRACT),
    NUMPAD_DECIMAL("NumpadDecimal", KeyEvent.VK_DECIMAL),
    NUMPAD_DIVIDE("NumpadDivide", KeyEvent.VK_DIVIDE),

    // Function Keys
    F1("F1", KeyEvent.VK_F1),
    F2("F2", KeyEvent.VK_F2),
    F3("F3", KeyEvent.VK_F3),
    F4("F4", KeyEvent.VK_F4),
    F5("F5", KeyEvent.VK_F5),
    F6("F6", KeyEvent.VK_F6),
    F7("F7", KeyEvent.VK_F7),
    F8("F8", KeyEvent.VK_F8),
    F9("F9", KeyEvent.VK_F9),
    F10("F10", KeyEvent.VK_F10),
    F11("F11", KeyEvent.VK_F11),
    F12("F12", KeyEvent.VK_F12),

    UNDEFINED("Undefined", KeyEvent.VK_UNDEFINED);

    private static final EnumLookup.StringKey<KeyboardKey> LOOKUP = new EnumLookup.StringKey<>(values(),
            KeyboardKey::getHtmlKeyname);

    private final String htmlKeyname;
    private final int keyCode;
    private final char baseChar;
    private final char shiftedSymbol;

    KeyboardKey(String htmlKeyname, int keyCode) {
        this(htmlKeyname, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar) {
        this(htmlKeyname, keyCode, baseChar, KeyEvent.CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar, char shiftedSymbol) {
        this.htmlKeyname = htmlKeyname;
        this.keyCode = keyCode;
        this.baseChar = baseChar;
        this.shiftedSymbol = shiftedSymbol;
    }

    public char getBaseChar() {
        if (this.baseChar != KeyEvent.CHAR_UNDEFINED) {
            return this.baseChar;
        }
        return (htmlKeyname.length() == 1) ? htmlKeyname.charAt(0) : '\0';
    }

    public static KeyboardKey lookup(String htmlKeyname) {
        return LOOKUP.getOrDefault(htmlKeyname, UNDEFINED);
    }
}
