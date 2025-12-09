package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public abstract class TCanvasGraphics extends TGraphics {

	protected final HTMLCanvasElement canvas;

	protected boolean needsBlit = false;
	protected boolean blitScheduled = false;

	protected final Set<TBufferedImage> dirtyImages = new HashSet<>();

	protected boolean supportsBlit = true;

	public abstract void putImageData(int x, int y, ImageData imageData);

	protected void scheduleBlit() {
		// Schedule a blit on the next animation frame
	}


	public final void requestBlit(TBufferedImage buffer) {
		if (!supportsBlit) {
			return;
		}
		dirtyImages.add(buffer);
		needsBlit = true;
		if (!blitScheduled) {
			scheduleBlit();
		}
	}

	protected final void notifyScheduled() {
		blitScheduled = true;
	}

	protected final void clearBlitRequest() {
		needsBlit = false;
		dirtyImages.clear();
	}

}
