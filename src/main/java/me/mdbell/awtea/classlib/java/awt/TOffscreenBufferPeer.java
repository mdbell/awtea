package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TSurfacePeer;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;


public class TOffscreenBufferPeer implements TSurfacePeer {

	private TBufferedImage buffer;
	private final TSurface root;

	public TOffscreenBufferPeer(TSurface root, int width, int height) {
		this.root = root;
		this.buffer = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_RGB);
	}

	@Override
	public void resize(int width, int height) {
		buffer = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_RGB);
	}

	@Override
	public void paintAll() {
		TGraphics g = buffer.getGraphics();

//		Color bg = root.getBackground();
//		g.setColor(bg != null ? bg : Color.BLACK);
//		g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());

		root.superPaint(g);
		g.dispose();
//		root.getSurfaceGraphics().drawImage(buffer, 0, 0, null);
	}
}
