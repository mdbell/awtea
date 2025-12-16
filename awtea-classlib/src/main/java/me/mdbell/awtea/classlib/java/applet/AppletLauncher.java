package me.mdbell.awtea.classlib.java.applet;

import me.mdbell.awtea.impl.TeaAppletStub;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSExport;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.Properties;

/**
 * Launcher for awtea applets that provides ES2015 module exports for JavaScript integration.
 * 
 * <p>This class enables two ways to launch applets:
 * <ul>
 *   <li><strong>Automatic discovery:</strong> Call {@link #main(String[])} (or use the ES module's
 *       default export) to automatically find and launch applets on canvases with the
 *       {@code data-awtea-applet} attribute.</li>
 *   <li><strong>Manual launch:</strong> Call {@link #launchNamed(String, String)} from JavaScript
 *       to launch a specific applet on a specific canvas.</li>
 * </ul>
 * 
 * <p><strong>Automatic Discovery Example:</strong>
 * <pre>
 * HTML:
 *   &lt;canvas id="app-canvas" data-awtea-applet="my-applet"&gt;&lt;/canvas&gt;
 * 
 * JavaScript (ES2015 module):
 *   import { main } from './js/my-app.js';
 *   main([]);
 * </pre>
 * 
 * <p><strong>Manual Launch Example:</strong>
 * <pre>
 * JavaScript (ES2015 module):
 *   import { launchNamed } from './js/my-app.js';
 *   launchNamed('app-canvas', 'my-applet');
 * </pre>
 * 
 * @see AppletRegistry
 * @see AppletFactory
 */
public final class AppletLauncher {
    
    private static final Logger log = LoggerFactory.getLogger(AppletLauncher.class);
    private static final String DATA_APPLET_ATTR = "data-awtea-applet";
    
    /**
     * Private constructor to prevent instantiation.
     */
    private AppletLauncher() {
        throw new UnsupportedOperationException("AppletLauncher cannot be instantiated");
    }
    
    /**
     * Main entry point for automatic applet discovery and launching.
     * Scans the DOM for canvas elements with the {@code data-awtea-applet} attribute
     * and launches the corresponding registered applets.
     * 
     * <p>This method is exported as an ES2015 module function and can be called from JavaScript.
     * 
     * @param args command-line arguments (currently unused)
     */
    @JSExport
    public static void main(String[] args) {
        log.info("AppletLauncher: Starting automatic applet discovery");
        
        HTMLDocument document = Window.current().getDocument();
        
        // Find all canvas elements with data-awtea-applet attribute
        var canvases = document.querySelectorAll("canvas[" + DATA_APPLET_ATTR + "]");
        
        int launchedCount = 0;
        int failedCount = 0;
        
        for (int i = 0; i < canvases.getLength(); i++) {
            HTMLElement element = (HTMLElement) canvases.item(i);
            
            if (!(element instanceof HTMLCanvasElement)) {
                log.warn("Element with {} is not a canvas: {}", DATA_APPLET_ATTR, element.getTagName());
                continue;
            }
            
            HTMLCanvasElement canvas = (HTMLCanvasElement) element;
            String appletName = canvas.getAttribute(DATA_APPLET_ATTR);
            String canvasId = canvas.getId();
            
            if (appletName == null || appletName.trim().isEmpty()) {
                log.warn("Canvas {} has empty {} attribute", canvasId, DATA_APPLET_ATTR);
                continue;
            }
            
            if (canvasId == null || canvasId.trim().isEmpty()) {
                log.warn("Canvas with {}=\"{}\" has no id attribute", DATA_APPLET_ATTR, appletName);
                continue;
            }
            
            log.info("Discovered applet: name='{}' on canvas='{}'", appletName, canvasId);
            
            try {
                launchApplet(canvasId, appletName);
                launchedCount++;
            } catch (Exception e) {
                log.error("Failed to launch applet '{}' on canvas '{}': {}", 
                    appletName, canvasId, e.getMessage(), e);
                failedCount++;
            }
        }
        
        log.info("AppletLauncher: Discovery complete. Launched: {}, Failed: {}", launchedCount, failedCount);
        
        if (launchedCount == 0 && failedCount == 0) {
            log.warn("No canvases with {} attribute found. Available applets: {}", 
                DATA_APPLET_ATTR, AppletRegistry.getRegisteredNames());
        }
    }
    
    /**
     * Launches a named applet on a specific canvas element.
     * This method is exported for manual invocation from JavaScript.
     * 
     * @param canvasId the ID of the canvas element to bind the applet to
     * @param appletName the name of the registered applet to launch
     * @return true if the applet was successfully launched, false otherwise
     */
    @JSExport
    public static boolean launchNamed(String canvasId, String appletName) {
        log.info("launchNamed: canvasId='{}', appletName='{}'", canvasId, appletName);
        
        try {
            launchApplet(canvasId, appletName);
            return true;
        } catch (Exception e) {
            log.error("Failed to launch applet '{}' on canvas '{}': {}", 
                appletName, canvasId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Internal method to launch an applet on a canvas.
     * 
     * @param canvasId the ID of the canvas element
     * @param appletName the name of the registered applet
     * @throws IllegalArgumentException if the canvas or applet doesn't exist
     */
    private static void launchApplet(String canvasId, String appletName) {
        // Validate canvas exists
        HTMLDocument document = Window.current().getDocument();
        HTMLElement element = document.getElementById(canvasId);
        
        if (element == null) {
            throw new IllegalArgumentException("Canvas element not found: " + canvasId);
        }
        
        if (!(element instanceof HTMLCanvasElement)) {
            throw new IllegalArgumentException("Element is not a canvas: " + canvasId);
        }
        
        HTMLCanvasElement canvas = (HTMLCanvasElement) element;
        
        // Create applet instance from registry
        TApplet applet = AppletRegistry.createApplet(appletName);
        
        // Set up applet stub with parameters
        Properties props = new Properties();
        TeaAppletStub stub = new TeaAppletStub(props);
        applet.setStub(stub);
        
        // Get canvas dimensions
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        
        if (width <= 0) {
            width = 800; // default width
            canvas.setWidth(width);
            log.debug("Canvas {} had no width, set to default: {}", canvasId, width);
        }
        
        if (height <= 0) {
            height = 600; // default height
            canvas.setHeight(height);
            log.debug("Canvas {} had no height, set to default: {}", canvasId, height);
        }
        
        applet.setSize(width, height);
        
        // Initialize and start the applet
        log.debug("Initializing applet '{}'", appletName);
        applet.init();
        
        log.debug("Starting applet '{}'", appletName);
        applet.start();
        
        log.info("Successfully launched applet '{}' on canvas '{}'", appletName, canvasId);
    }
}
