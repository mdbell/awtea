package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TSurfacePeer;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;

import java.awt.*;


public class TOffscreenBufferPeer implements TSurfacePeer {

	private TBufferedImage buffer;
	private final TSurface root;

	private boolean blitRequested = false;

	private boolean needsBlit = false;
	private boolean rafScheduled = false;

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
		g.setColor(bg != null ? bg : Color.LIGHT_GRAY);
		g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		root.superPaint(g);
		g.dispose();
		root.getSurfaceGraphics().requestBlit(buffer);
		//root.getSurfaceGraphics().drawImage(buffer, 0, 0, null);
	}
}
