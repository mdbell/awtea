package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.peer.TSurfacePeer;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.instrument.Monitored;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.ImageData;

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
	@Monitored
	public void paintAll() {
		TGraphics g = buffer.getGraphics();
		Color bg = root.getBackground();
		g.setColor(bg != null ? bg : Color.LIGHT_GRAY);
		g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
		root.superPaint(g);
		g.dispose();
		requestBlit();
		//root.getSurfaceGraphics().drawImage(buffer, 0, 0, null);
	}

	private void requestBlit() {
		needsBlit = true;
		if (!rafScheduled) {
			rafScheduled = true;
			scheduleRaf();
		}
	}

	private void scheduleRaf() {
		Window.requestAnimationFrame(time -> {
			rafScheduled = false;

			if (!needsBlit) {
				return; // nothing new to draw
			}
			needsBlit = false;

			// do the actual upload here
			ImageData data = buffer.getImageData();
			TCanvasGraphics gfx = root.getSurfaceGraphics();
			gfx.putImageData(0, 0, data);

			// if more paints happened while we were here, request another rAF
			if (needsBlit) {
				scheduleRaf();
				rafScheduled = true;
			}
		});
	}
}
