package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Browser-compatible stub for java.awt.Desktop.
 * Desktop operations are not supported in browser environments.
 * All methods throw UnsupportedOperationException with clear error messages.
 * 
 * @see java.awt.Desktop
 */
public class TDesktop {
    
    private static final Logger log = LoggerFactory.getLogger(TDesktop.class);
    
    private TDesktop() {
        // Private constructor - instances should be obtained via getDesktop()
    }
    
    /**
     * Returns the Desktop instance of the current browser/desktop context.
     * 
     * @return the Desktop instance
     * @throws UnsupportedOperationException always, as Desktop is not supported in browser
     */
    public static TDesktop getDesktop() {
        log.warn("Desktop.getDesktop() called - Desktop operations are not supported in browser environment");
        throw new UnsupportedOperationException("java.awt.Desktop is not supported in browser environment. Desktop operations (open, browse, mail, etc.) cannot be performed from browser-based applications.");
    }
    
    /**
     * Tests whether this platform supports the given action.
     * 
     * @param action the action to test
     * @return false always, as Desktop operations are not supported in browser
     */
    public static boolean isDesktopSupported() {
        log.warn("Desktop.isDesktopSupported() called - returning false for browser environment");
        return false;
    }

    public void browse(java.net.URI uri) {
        log.warn("Desktop.browse() called - Desktop operations are not supported in browser environment");
        throw new UnsupportedOperationException("Desktop.browse() is not supported in browser environment.");
    }
}
