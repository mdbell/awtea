package me.mdbell.awtea.classlib.javax.swing;

import me.mdbell.awtea.classlib.java.awt.TFrame;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Browser-compatible stub for javax.swing.JFrame.
 * Extends TFrame to provide basic compatibility with Swing applets.
 * 
 * @see javax.swing.JFrame
 * @see TFrame
 */
public class TJFrame extends TFrame {
    
    private static final Logger log = LoggerFactory.getLogger(TJFrame.class);
    
    /**
     * Constructs a new JFrame that is initially invisible.
     */
    public TJFrame() {
        super();
        log.debug("TJFrame created as browser-compatible substitute for javax.swing.JFrame");
    }
    
    /**
     * Constructs a new JFrame with the specified title.
     * 
     * @param title the title for the frame
     */
    public TJFrame(String title) {
        super();
        setTitle(title);
        log.debug("TJFrame created with title: {}", title);
    }
    
    /**
     * Sets the operation that will happen by default when the user initiates
     * a "close" on this frame.
     * 
     * @param operation the operation which should be performed when the user closes the frame
     */
    public void setDefaultCloseOperation(int operation) {
        log.debug("setDefaultCloseOperation({}) - limited support in browser", operation);
        // In browser environment, we can only hide the frame
        // Browser-based frames don't have native close buttons
    }

    public boolean isResizable() {
        log.warn("TJFrame.isResizable() is not supported in browser environment.");
        return false;
    }
}
