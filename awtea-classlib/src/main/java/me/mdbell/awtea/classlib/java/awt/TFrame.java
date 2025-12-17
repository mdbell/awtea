package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TFrameFloatingPeer;

/**
 * A {@code TFrame} is a top-level window with a title and a border.
 * The size of the frame includes any area designated for the border.
 * <p>
 * The default layout for a frame is {@code BorderLayout}.
 * <p>
 * A frame may have its native decorations (such as border, title, and so on)
 * turned off by using {@code setUndecorated}.
 *
 * @see java.awt.Frame
 * @see TWindow
 */
public class TFrame extends TWindow {

    private final TFrameFloatingPeer peer;

    /**
     * Constructs a new frame that is initially invisible.
     * The frame is created with a peer that manages the actual windowing behavior.
     */
    public TFrame() {
        peer = new TFrameFloatingPeer(this);
        this.surfacePeer = new TOffscreenBufferPeer(this, 1, 1);
    }

    @Override
    public TGraphics getSurfaceGraphics() {
        return peer.getGraphics();
    }

    /**
     * Sets the title for this frame to the specified string.
     *
     * @param title the title to be displayed in the frame's border.
     *              A {@code null} value is treated as an empty string, "".
     */
    public void setTitle(String title) {
        peer.setTitle(title);
    }

    /**
     * Sets whether this frame is resizable by the user.
     * By default, all frames are initially resizable.
     *
     * @param resizable {@code true} if this frame is resizable;
     *                  {@code false} otherwise
     */
    public void setResizable(boolean resizable) {
        peer.setResizeable(resizable);
    }

    /**
     * Shows or hides this frame depending on the value of parameter {@code b}.
     * Also notifies the peer to update the visibility state.
     *
     * @param b if {@code true}, makes the frame visible, otherwise hides the frame
     */
    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        peer.setVisible(b);
    }

    /**
     * If this frame is visible, brings this frame to the front and may make it the focused window.
     */
    @Override
    public void toFront() {
        peer.bringToFront();
    }

    /**
     * Determines the insets of this frame, which indicate the size of the frame's border.
     * This includes the title bar and other decorations.
     *
     * @return the insets of this frame
     */
    @Override
    public TInsets getInsets() {
        // TODO: Calculate actual insets based on frame decorations
        // For now, return zero insets (decorations are handled by peer)
        return new TInsets(0, 0, 0, 0);
    }

    /**
     * Sets the size of this frame to the specified width and height.
     * Also notifies the peer to update the window size and ensures proper
     * layout validation and repainting.
     *
     * @param width  the new width of this frame in pixels
     * @param height the new height of this frame in pixels
     */
    @Override
    public void setSize(int width, int height) {
        if (width == getWidth() && height == getHeight()) {
            return;
        }
        super.setSize(width, height);
        peer.setSize(width, height);
        invalidate();
        validate();
        repaint();
    }
}
