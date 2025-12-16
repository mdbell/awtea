package me.mdbell.awtea.classlib.java.applet;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.util.JSObjectsExtensions;

import java.net.URL;

/**
 * Base class for applets that can run in a browser.
 * 
 * <p>Applets can be either heavyweight (with a peer) or lightweight (without a peer):
 * <ul>
 *   <li><b>Heavyweight applets:</b> Have a {@link TAppletPeer} which contains a THeavyCanvas.
 *       They manage their own canvas element and surface for rendering.</li>
 *   <li><b>Lightweight applets:</b> Have no peer and delegate graphics operations to their
 *       parent container, similar to other lightweight components.</li>
 * </ul>
 * 
 * @see java.applet.Applet
 * @see TAppletPeer
 */
@ExtensionMethod({JSObjectsExtensions.class})
public class TApplet extends TContainer {

	private TAppletStub stub;
	
	/**
	 * -- GETTER --
	 * Gets the peer for this applet, if it has one.
	 * 
	 * @return The TAppletPeer, or null if this is a lightweight applet
	 */
	@Getter
	private TAppletPeer peer;

	public URL getCodeBase() {
		return stub.getCodeBase();
	}

	public URL getDocumentBase() {
		return stub.getDocumentBase();
	}

	public TAppletContext getAppletContext() {
		return stub.getAppletContext();
	}

	public final void setStub(TAppletStub stub) {
		this.stub = stub;
		
		// If the stub provides a peer, set it automatically
		// The peer is returned as Object to avoid circular dependencies
		Object peerObj = stub.getPeer();
		if (peerObj instanceof TAppletPeer) {
			this.peer = (TAppletPeer) peerObj;
		}
	}
	
	/**
	 * Sets the peer for this applet, making it heavyweight.
	 * This should be called before init() if the applet needs its own canvas.
	 * 
	 * @param peer The TAppletPeer to associate with this applet
	 */
	public void setPeer(TAppletPeer peer) {
		this.peer = peer;
	}
	
	/**
	 * Checks if this applet is heavyweight (has a peer).
	 * 
	 * @return true if this applet has a peer, false if it's lightweight
	 */
	public boolean isHeavyweight() {
		return peer != null;
	}

	public void init() {
	}

	public void start() {
	}

	public String getParameter(String name) {
		return stub.getParameter(name);
	}
	
	/**
	 * Gets the graphics context for this applet.
	 * 
	 * <p>If the applet has a peer (heavyweight), returns the peer's graphics context.
	 * Otherwise (lightweight), delegates to the parent container.</p>
	 * 
	 * @return A TGraphics instance for drawing
	 */
	@Override
	public TGraphics getGraphics() {
		if (peer != null) {
			// Heavyweight: use peer's graphics
			return peer.getGraphics();
		} else {
			// Lightweight: delegate to parent
			return super.getGraphics();
		}
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		
		// Notify peer if present
		if (peer != null) {
			peer.resize(width, height);
		}
		
		// Notify stub
		if (stub != null) {
			stub.appletResize(width, height);
		}
	}
}
