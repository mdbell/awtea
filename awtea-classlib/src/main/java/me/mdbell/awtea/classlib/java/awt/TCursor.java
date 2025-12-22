package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Browser-compatible stub for java.awt.Cursor.
 * Provides basic cursor type constants and no-op implementation.
 * Browser cursor changes are typically handled via CSS.
 * 
 * @see java.awt.Cursor
 */
public class TCursor {
    
    private static final Logger log = LoggerFactory.getLogger(TCursor.class);
    
    /**
     * The default cursor type (gets set if no cursor is defined).
     */
    public static final int DEFAULT_CURSOR = 0;
    
    /**
     * The crosshair cursor type.
     */
    public static final int CROSSHAIR_CURSOR = 1;
    
    /**
     * The text cursor type.
     */
    public static final int TEXT_CURSOR = 2;
    
    /**
     * The wait cursor type.
     */
    public static final int WAIT_CURSOR = 3;
    
    /**
     * The southwest-resize cursor type.
     */
    public static final int SW_RESIZE_CURSOR = 4;
    
    /**
     * The southeast-resize cursor type.
     */
    public static final int SE_RESIZE_CURSOR = 5;
    
    /**
     * The northwest-resize cursor type.
     */
    public static final int NW_RESIZE_CURSOR = 6;
    
    /**
     * The northeast-resize cursor type.
     */
    public static final int NE_RESIZE_CURSOR = 7;
    
    /**
     * The north-resize cursor type.
     */
    public static final int N_RESIZE_CURSOR = 8;
    
    /**
     * The south-resize cursor type.
     */
    public static final int S_RESIZE_CURSOR = 9;
    
    /**
     * The west-resize cursor type.
     */
    public static final int W_RESIZE_CURSOR = 10;
    
    /**
     * The east-resize cursor type.
     */
    public static final int E_RESIZE_CURSOR = 11;
    
    /**
     * The hand cursor type.
     */
    public static final int HAND_CURSOR = 12;
    
    /**
     * The move cursor type.
     */
    public static final int MOVE_CURSOR = 13;
    
    private final int type;
    private final String name;
    
    /**
     * Creates a new cursor object with the specified type.
     * 
     * @param type the type of cursor
     */
    public TCursor(int type) {
        this.type = type;
        this.name = getCursorName(type);
        log.debug("Cursor created with type: {} ({})", type, name);
    }
    
    /**
     * Creates a new custom cursor object with the specified name.
     * 
     * @param name the name of the cursor
     */
    protected TCursor(String name) {
        this.type = CUSTOM_CURSOR;
        this.name = name;
        log.debug("Custom cursor created: {}", name);
    }
    
    /**
     * The type associated with all custom cursors.
     */
    public static final int CUSTOM_CURSOR = -1;
    
    /**
     * Returns a cursor object with the specified predefined type.
     * 
     * @param type the type of cursor
     * @return the cursor object with the specified type
     */
    public static TCursor getPredefinedCursor(int type) {
        return new TCursor(type);
    }
    
    /**
     * Returns the default cursor.
     * 
     * @return the default cursor
     */
    public static TCursor getDefaultCursor() {
        return new TCursor(DEFAULT_CURSOR);
    }
    
    /**
     * Returns the type of this cursor.
     * 
     * @return the type of this cursor
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the name of this cursor.
     * 
     * @return the name of this cursor
     */
    public String getName() {
        return name;
    }
    
    private static String getCursorName(int type) {
        switch (type) {
            case DEFAULT_CURSOR: return "Default Cursor";
            case CROSSHAIR_CURSOR: return "Crosshair Cursor";
            case TEXT_CURSOR: return "Text Cursor";
            case WAIT_CURSOR: return "Wait Cursor";
            case SW_RESIZE_CURSOR: return "Southwest Resize Cursor";
            case SE_RESIZE_CURSOR: return "Southeast Resize Cursor";
            case NW_RESIZE_CURSOR: return "Northwest Resize Cursor";
            case NE_RESIZE_CURSOR: return "Northeast Resize Cursor";
            case N_RESIZE_CURSOR: return "North Resize Cursor";
            case S_RESIZE_CURSOR: return "South Resize Cursor";
            case W_RESIZE_CURSOR: return "West Resize Cursor";
            case E_RESIZE_CURSOR: return "East Resize Cursor";
            case HAND_CURSOR: return "Hand Cursor";
            case MOVE_CURSOR: return "Move Cursor";
            case CUSTOM_CURSOR: return "Custom Cursor";
            default: return "Unknown Cursor";
        }
    }
}
