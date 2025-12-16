package me.mdbell.awtea.classlib.java.applet;

import me.mdbell.awtea.Helper;
import me.mdbell.awtea.impl.TeaAppletStub;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSExport;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.awt.Frame;
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
     * <p>When running on the JVM (not TeaVM), creates a Frame for each registered applet
     * to enable side-by-side comparison with browser version.
     * 
     * @param args command-line arguments (currently unused)
     */
    public static void main(String[] args) {
        // Check if we're running on TeaVM or standard JVM
        if (!Helper.isTeaVM()) {
            // Running on standard JVM - launch applets in Frames
            launchOnJVM();
            return;
        }
        
        // Running on TeaVM - use browser DOM discovery
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
     * Sets a system property from JavaScript.
     * This allows configuring awtea settings without recompiling.
     * 
     * @param key the property key
     * @param value the property value
     */
    @JSExport
    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
        log.debug("System property set: {}={}", key, value);
    }
    
    /**
     * Launches all registered applets on the JVM by creating a Frame for each.
     * This enables side-by-side comparison with the browser version.
     */
    private static void launchOnJVM() {
        log.info("AppletLauncher: Running on JVM, creating Frames for registered applets");
        
        var registeredNames = AppletRegistry.getRegisteredNames();
        
        if (registeredNames.isEmpty()) {
            log.warn("No applets registered. Available applets: {}", registeredNames);
            return;
        }
        
        int launchedCount = 0;
        
        for (String appletName : registeredNames) {
            try {
                // Create the applet
                java.applet.Applet applet = AppletRegistry.createApplet(appletName);
                
                // Create a Frame to host the applet
                Frame frame = new Frame();
                frame.setTitle(appletName + " - awtea JVM");
                frame.setSize(800, 600);
                
                // Set up a simple applet stub for JVM mode
                Properties props = new Properties();
                java.applet.AppletStub stub = new java.applet.AppletStub() {
                    @Override
                    public boolean isActive() { return true; }
                    
                    @Override
                    public java.net.URL getDocumentBase() {
                        try {
                            return new java.net.URL("file:///");
                        } catch (java.net.MalformedURLException e) {
                            return null;
                        }
                    }
                    
                    @Override
                    public java.net.URL getCodeBase() {
                        return getDocumentBase();
                    }
                    
                    @Override
                    public String getParameter(String name) {
                        return props.getProperty(name);
                    }
                    
                    @Override
                    public java.applet.AppletContext getAppletContext() {
                        return null;
                    }
                    
                    @Override
                    public void appletResize(int width, int height) {
                        frame.setSize(width, height);
                    }
                };
                
                applet.setStub(stub);
                applet.setSize(800, 600);
                
                // Add applet to frame
                frame.add(applet);
                
                // Initialize and start the applet
                applet.init();
                applet.start();
                
                // Show the frame
                frame.setVisible(true);
                
                launchedCount++;
                log.info("Successfully launched applet '{}' in Frame", appletName);
                
            } catch (Exception e) {
                log.error("Failed to launch applet '{}' on JVM: {}", appletName, e.getMessage(), e);
            }
        }
        
        log.info("AppletLauncher: Launched {} applet(s) on JVM", launchedCount);
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
        java.applet.Applet applet = AppletRegistry.createApplet(appletName);
        
        // Get canvas dimensions
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        
        if (width <= 0) {
            width = 800; // default width
            log.debug("Canvas {} had no width, using default: {}", canvasId, width);
        }
        
        if (height <= 0) {
            height = 600; // default height
            log.debug("Canvas {} had no height, using default: {}", canvasId, height);
        }
        
        // Create and configure heavyweight peer for the applet
        // Note: At runtime, TeaVM aliases java.applet.Applet to TApplet,
        // so this will work correctly in the browser
        TAppletPeer peer = createPeerForApplet(applet, document, width, height);
        
        // Replace the canvas in the DOM with the peer's canvas
        replaceDOMCanvas(canvas, peer.getCanvasElement(), canvasId);
        
        // Set up applet stub with parameters and the peer's canvas
        Properties props = new Properties();
        TeaAppletStub teaStub = new TeaAppletStub(props, peer.getCanvasElement());
        
        // Set the stub on the applet
        applet.setStub(teaStub);
        
        applet.setSize(width, height);
        
        // Initialize and start the applet
        log.debug("Initializing applet '{}'", appletName);
        applet.init();
        
        log.debug("Starting applet '{}'", appletName);
        applet.start();
        
        log.info("Successfully launched applet '{}' on canvas '{}'", appletName, canvasId);
    }
    
    /**
     * Creates a peer for the applet using JavaScript interop.
     * This works around compile-time type issues while maintaining runtime correctness.
     */
    @org.teavm.jso.JSBody(params = {"applet", "document", "width", "height"}, script =
        "var peer = new (Java.type('me.mdbell.awtea.classlib.java.applet.TAppletPeer'))(document, applet, width, height);" +
        "applet.setPeer(peer);" +
        "return peer;"
    )
    private static native TAppletPeer createPeerForApplet(
        java.applet.Applet applet,
        HTMLDocument document,
        int width,
        int height
    );
    
    /**
     * Replaces a canvas element in the DOM with a new canvas element.
     */
    @org.teavm.jso.JSBody(params = {"oldCanvas", "newCanvas", "canvasId"}, script =
        "var parent = oldCanvas.parentNode;" +
        "if (parent) {" +
        "    newCanvas.id = canvasId;" +
        "    var attr = oldCanvas.getAttribute('data-awtea-applet');" +
        "    if (attr) newCanvas.setAttribute('data-awtea-applet', attr);" +
        "    parent.replaceChild(newCanvas, oldCanvas);" +
        "}"
    )
    private static native void replaceDOMCanvas(
        HTMLCanvasElement oldCanvas,
        HTMLCanvasElement newCanvas,
        String canvasId
    );
}
