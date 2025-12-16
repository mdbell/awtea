package me.mdbell.awtea.classlib.java.applet;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.classlib.java.awt.THeavyCanvas;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * Peer for heavyweight applets that manages a THeavyCanvas.
 * 
 * <p>When an applet has a peer, it is considered heavyweight and uses its own
 * canvas element for rendering. This peer provides the canvas and graphics context
 * for the applet's rendering operations.</p>
 * 
 * <p>Applets without a peer are lightweight and delegate rendering to their parent
 * container.</p>
 * 
 * @see TApplet
 * @see THeavyCanvas
 */
public class TAppletPeer {
    
    /**
     * -- GETTER --
     * Gets the heavyweight canvas managed by this peer.
     * 
     * @return The THeavyCanvas instance
     */
    @Getter
    private final THeavyCanvas heavyCanvas;
    
    /**
     * Creates a new applet peer with the specified document and applet.
     * 
     * @param document The HTML document to create the canvas in
     * @param applet The applet that this peer serves
     */
    public TAppletPeer(HTMLDocument document, TApplet applet) {
        this(document, applet, 800, 600);
    }
    
    /**
     * Creates a new applet peer with the specified document, applet, and initial size.
     * 
     * @param document The HTML document to create the canvas in
     * @param applet The applet that this peer serves
     * @param width Initial width in pixels
     * @param height Initial height in pixels
     */
    public TAppletPeer(HTMLDocument document, TApplet applet, int width, int height) {
        // Create heavyweight canvas
        heavyCanvas = new THeavyCanvas(document, applet, width, height);
        
        // Configure standard event handling
        heavyCanvas.configureStandardEvents();
    }
    
    /**
     * Gets the HTML canvas element managed by this peer.
     * This element should be attached to the DOM.
     * 
     * @return The HTMLCanvasElement
     */
    public HTMLCanvasElement getCanvasElement() {
        return heavyCanvas.getCanvasElement();
    }
    
    /**
     * Gets a graphics context for rendering to the applet.
     * 
     * @return A TGraphics instance for drawing
     */
    public TGraphics getGraphics() {
        return heavyCanvas.getGraphics();
    }
    
    /**
     * Resizes the applet's canvas to the specified dimensions.
     * 
     * @param width New width in pixels
     * @param height New height in pixels
     */
    public void resize(int width, int height) {
        heavyCanvas.resize(width, height);
    }
    
    /**
     * Gets the current width of the canvas.
     * 
     * @return Canvas width in pixels
     */
    public int getWidth() {
        return heavyCanvas.getWidth();
    }
    
    /**
     * Gets the current height of the canvas.
     * 
     * @return Canvas height in pixels
     */
    public int getHeight() {
        return heavyCanvas.getHeight();
    }
    
    /**
     * Cleans up resources associated with this peer.
     * This should be called when the applet is no longer needed.
     */
    public void destroy() {
        heavyCanvas.destroy();
    }
}
