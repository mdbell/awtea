package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TSurfacePeer;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;

import java.awt.*;


public class TOffscreenBufferPeer implements TSurfacePeer {

    private TBufferedImage buffer;
    private final TSurface root;

    public TOffscreenBufferPeer(TSurface root, int width, int height) {
        this.root = root;
        this.buffer = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void resize(int width, int height) {
        buffer = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void paintAll() {
        TGraphics g = buffer.getGraphics();

        Color bg = root.getBackground();
        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, root.getWidth(), root.getHeight());
        }

        root.superPaint(g);
        g.dispose();
        root.getSurfaceGraphics().drawImage(buffer, 0, 0, null);
    }
}
