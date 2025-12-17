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
        TGraphics surface = root.getSurfaceGraphics();

        root.paint(g);
        g.dispose();

        Color bg = root.getBackground();
        if (bg != null) {
            surface.setColor(bg);
            surface.fillRect(0, 0, root.getWidth(), root.getHeight());
        }
        surface.drawImage(buffer, 0, 0, null);
    }
}
