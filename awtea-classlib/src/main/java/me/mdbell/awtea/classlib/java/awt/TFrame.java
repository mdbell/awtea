package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TFrameFloatingPeer;
import me.mdbell.awtea.classlib.java.awt.event.TWindowListener;

import java.awt.*;
import java.awt.event.ComponentListener;

public class TFrame extends TSurface {

    private final TFrameFloatingPeer peer;

    private static final Color FRAME_BACKGROUND_COLOR = new Color(223, 223, 223);

    // Constructor
    public TFrame() {
        peer = new TFrameFloatingPeer(this);
        this.surfacePeer = new TOffscreenBufferPeer(this, 1, 1);
        this.setBackground(FRAME_BACKGROUND_COLOR);
        // Frames use BorderLayout by default in AWT
        setLayout(new TBorderLayout());
    }

    @Override
    public TGraphics getSurfaceGraphics() {
        return peer.getGraphics();
    }

    // Set the title of the frame
    public void setTitle(String title) {
        peer.setTitle(title);
    }

    // Set whether the frame is resizable
    public void setResizable(boolean resizable) {
        peer.setResizeable(resizable);
    }

    // Add a window listener
    public void addWindowListener(TWindowListener l) {
        // Implement event listener logic
    }

    // Set the frame visible
    public void setVisible(boolean b) {
        super.setVisible(b);
        peer.setVisible(b);
    }

    public void toFront() {
        peer.bringToFront();
    }

    // Get Insets, using the TInsets class
    public TInsets getInsets() {
        return new TInsets(0, 0, 0, 0);
    }

    public void setSize(int width, int height) {
        if (width == getWidth() && height == getHeight()) {
            return;
        }
        super.setSize(width, height);
        peer.setSize(width, height);
        repaint();
    }

    // this is _technically_ part of Window, but whatever
    public void pack() {
        int minWidth = 0;
        int minHeight = 0;
        TComponent[] components = this.getComponents();
        for (TComponent child : components) {
            minWidth = Math.max(minWidth, child.getX() + child.getWidth());
            minHeight = Math.max(minHeight, child.getY() + child.getHeight());
        }

        setSize(minWidth, minHeight);
        repaint();
    }

    public void setMinimumSize(Dimension dim) {
        // No-op mock
    }

    public void addComponentListener(ComponentListener l) {

    }
}
