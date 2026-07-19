package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.awtea.TEventManager;
import me.mdbell.awtea.classlib.java.awt.awtea.peer.TFrameFloatingPeer;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.gfx.CompositeSurfaceBackend;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.gfx.SurfaceBackendFactory;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * THeavyCanvas: Heavyweight Hardware-Backed Canvas Component
 *
 * <p>
 * A heavyweight canvas component that manages its own HTML canvas element,
 * surface (hardware/software rendering backend), and event handling.
 * This component provides a unified, consistent implementation for all
 * heavyweight window peers (Frame, Dialog, Applet windows, etc.).
 * </p>
 *
 * <p>
 * <b>Key Features:</b>
 * </p>
 * <ul>
 * <li>Owns and manages an HTMLCanvasElement</li>
 * <li>Manages a dedicated Surface instance for rendering</li>
 * <li>Handles native events via TEventManager</li>
 * <li>Supports resizing with proper surface lifecycle management</li>
 * <li>Provides a TGraphics context for AWT rendering</li>
 * </ul>
 *
 * <p>
 * <b>Usage:</b>
 * </p>
 *
 * <pre>{@code
 * // Create a heavyweight canvas for a container
 * THeavyCanvas canvas = new THeavyCanvas(document, container);
 *
 * // Get the canvas element to attach to DOM
 * HTMLCanvasElement element = canvas.getCanvasElement();
 *
 * // Resize the canvas
 * canvas.resize(800, 600);
 *
 * // Get graphics context for rendering
 * TGraphics graphics = canvas.getGraphics();
 *
 * // Clean up when done
 * canvas.destroy();
 * }</pre>
 *
 * <p>
 * This component is intended for use by heavyweight peers like
 * TFrameFloatingPeer and reduces code duplication across peer implementations.
 * </p>
 *
 * @see TCanvas The lightweight canvas component for embedding in AWT containers
 * @see TFrameFloatingPeer Example heavyweight peer using THeavyCanvas
 */
public class THeavyCanvas {

    private static final Logger log = LoggerFactory.getLogger(THeavyCanvas.class);

    /**
     * -- GETTER --
     * Gets the underlying HTML canvas element.
     * This element should be attached to the DOM by the caller.
     *
     * @return The HTMLCanvasElement managed by this canvas
     */
    @Getter
    private final HTMLCanvasElement canvasElement;
    /**
     * -- GETTER --
     * Gets the event manager for advanced event configuration.
     *
     * @return The TEventManager instance
     */
    @Getter
    private final TEventManager eventManager;
    /**
     * -- GETTER --
     * Gets the underlying surface for advanced rendering operations.
     *
     * @return The Surface instance
     */
    @Getter
    private final Surface surface;
    private final TBufferedImage screenImg;

    /**
     * Creates a new heavyweight canvas with default initial size.
     *
     * @param document  The HTML document to create the canvas element in
     * @param container The AWT container that owns this canvas (for event
     *                  dispatching)
     */
    public THeavyCanvas(HTMLDocument document, TContainer container) {
        this(document, container, 10, 10);
    }

    /**
     * Creates a new heavyweight canvas with specified initial size.
     *
     * @param document  The HTML document to create the canvas element in
     * @param container The AWT container that owns this canvas (for event
     *                  dispatching)
     * @param width     Initial width in pixels
     * @param height    Initial height in pixels
     */
    public THeavyCanvas(HTMLDocument document, TContainer container, int width, int height) {
        // Create canvas element
        canvasElement = (HTMLCanvasElement) document.createElement("canvas");
        canvasElement.setAttribute("tabindex", "0"); // make canvas focusable
        canvasElement.getStyle().setProperty("outline", "none"); // remove focus outline

        // Set initial size
        canvasElement.setWidth(width);
        canvasElement.setHeight(height);

        // Create event manager
        eventManager = new TEventManager(canvasElement, container);

        // Create surface for rendering
        surface = createScreenSurface();

        // Create buffered image wrapper
        screenImg = new TBufferedImage(surface);
    }

    public THeavyCanvas(HTMLCanvasElement canvasElement, TContainer container) {
        this(canvasElement, container, canvasElement.getWidth(), canvasElement.getHeight());
    }

    public THeavyCanvas(HTMLCanvasElement canvasElement, TContainer container, int width, int height) {
        this.canvasElement = canvasElement;

        // Create event manager
        eventManager = new TEventManager(canvasElement, container);

        canvasElement.setWidth(width);
        canvasElement.setHeight(height);

        // Create surface for rendering
        surface = createScreenSurface();

        // Create buffered image wrapper
        screenImg = new TBufferedImage(surface);
    }

    private Surface createScreenSurface() {
        try {
            WebGLSurfaceBackend webgl = (WebGLSurfaceBackend) SurfaceBackendFactory.getWebGLBackend(canvasElement);
            Surface surface = webgl.createScreenSurface(getWidth(), getHeight());
            log.trace("Using WebGL surface backend for heavyweight canvas - dimensions: {}x{}", getWidth(),
                    getHeight());

            // Automatically enable GPU-based hit picking for WebGL backend
//			initializeWebGLPickingStrategy(webgl);

            // we use a composite backend as there are still some surfaces that webgl
            // doesn't fully support (like text rendering)
            CompositeSurfaceBackend compositeSurfaceBackend = new CompositeSurfaceBackend(new SurfaceBackend[]{
                    webgl,
                    SurfaceBackendFactory.getDefault()
            });
            SurfaceBackendFactory.setDefault(compositeSurfaceBackend);
            return surface;
        } catch (Exception e) {
            log.warn("Failed to create a webgl instance! Using default");
        }
        return SurfaceBackendFactory.createScreenSurface(getWidth(), getHeight(), canvasElement);
    }

    /**
     * Initializes GPU-based hit picking for WebGL backend.
     * This automatically enables O(1) hit-testing when using WebGL.
     *
     * @param webgl the WebGL backend
     */
    private void initializeWebGLPickingStrategy(WebGLSurfaceBackend webgl) {
        try {
            // Get the container from event manager
            TContainer container = getContainerFromEventManager();
            if (container == null) {
                log.debug("Cannot initialize picking strategy - no container available");
                return;
            }

            // Create picking buffer strategy
            me.mdbell.awtea.classlib.java.awt.awtea.TPickingBufferHitTestStrategy pickingStrategy =
                    new me.mdbell.awtea.classlib.java.awt.awtea.TPickingBufferHitTestStrategy(
                            webgl, container, getWidth(), getHeight()
                    );

            // Set on event manager
            eventManager.setHitTestStrategy(pickingStrategy);

            log.info("GPU-based hit picking enabled for WebGL backend");
        } catch (Exception e) {
            log.warn("Failed to initialize GPU picking strategy: {}", e.getMessage());
            // Fallback to tree-walk strategy (already initialized by default)
        }
    }

    /**
     * Gets the container from the event manager using reflection-free approach.
     */
    private TContainer getContainerFromEventManager() {
        return eventManager.getContainer();
    }

    /**
     * Configures standard event handling for a heavyweight canvas.
     * This sets up:
     * <ul>
     * <li>Context menu suppression</li>
     * <li>Focus events</li>
     * <li>Keyboard events</li>
     * <li>Mouse events</li>
     * <li>Mouse wheel events</li>
     * </ul>
     *
     * @return This canvas instance for method chaining
     */
    public THeavyCanvas configureStandardEvents() {
        eventManager.disableContextMenu()
                .withFocus()
                .withKeyboard()
                .withKeypressSuppression()
                .withMouse()
                .withMouseWheel();

        // Give the canvas DOM focus so keyboard events work immediately
        canvasElement.focus();

        return this;
    }

    /**
     * Gets a graphics context for rendering to this canvas.
     *
     * @return A TGraphics instance for drawing
     */
    public TGraphics getGraphics() {
        return screenImg.getGraphics();
    }

    /**
     * Gets the buffered image associated with this canvas.
     *
     * @return The TBufferedImage wrapping the canvas surface
     */
    public TBufferedImage getBufferedImage() {
        return screenImg;
    }

    /**
     * Resizes the canvas to the specified dimensions.
     * This updates both the canvas element size and the underlying surface.
     * Note: Resizing does not preserve existing pixel data.
     *
     * @param width  New width in pixels
     * @param height New height in pixels
     */
    public void resize(int width, int height) {
        canvasElement.setWidth(width);
        canvasElement.setHeight(height);
        surface.resize(width, height);
    }

    /**
     * Gets the current width of the canvas.
     *
     * @return Canvas width in pixels
     */
    public int getWidth() {
        return canvasElement.getWidth();
    }

    /**
     * Gets the current height of the canvas.
     *
     * @return Canvas height in pixels
     */
    public int getHeight() {
        return canvasElement.getHeight();
    }

    /**
     * Cleans up resources associated with this canvas.
     * This should be called when the canvas is no longer needed.
     * After calling destroy(), this canvas instance should not be used.
     */
    public void destroy() {
        eventManager.detach();
        surface.destroy();
    }
}
