package me.mdbell.awtea.classlib.java.awt.awtea.peer;

public interface TSurfacePeer {

	/**
	 * Resize the surface.
	 *
	 * @param width  The new width.
	 * @param height The new height.
	 */
	void resize(int width, int height);

	/**
	 * Paint the surface.
	 */
	void paintAll();
}
