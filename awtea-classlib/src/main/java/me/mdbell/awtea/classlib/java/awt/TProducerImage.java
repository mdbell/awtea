package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class TProducerImage extends TImage {

	private final List<TImageObserver> observers = new ArrayList<>();

	private final TImageProducer producer;
	private final ProducerConsumer consumer;

	protected TBufferedImage bufferedImage = null;

	private int width = -1;
	private int height = -1;

	private boolean productionStarted = false;

	public TProducerImage(TImageProducer producer) {
		this.producer = producer;
		this.consumer = new ProducerConsumer();
	}

	@Override
	public int getWidth(TImageObserver obs) {
		if (width >= 0) {
			return width;
		}
		if (obs != null) {
			registerObserver(obs, TImageObserver.WIDTH);
		}
		startProductionIfNeeded();
		return -1;
	}

	@Override
	public int getHeight(TImageObserver obs) {
		if (height >= 0) {
			return height;
		}
		if (obs != null) {
			registerObserver(obs, TImageObserver.HEIGHT);
		}
		startProductionIfNeeded();
		return -1;
	}

	@Override
	public TImageProducer getSource() {
		return producer;
	}

	@Override
	public TGraphics getGraphics() {
		if (bufferedImage != null) {
			return bufferedImage.getGraphics();
		}
		return null;
	}

	@Override
	public Object getProperty(String name, TImageObserver observer) {
		return null;
	}

	private synchronized void startProductionIfNeeded() {
		if (!productionStarted) {
			productionStarted = true;
			producer.startProduction(consumer);
		}
	}

	private void registerObserver(TImageObserver obs, int flags) {
		if (!observers.contains(obs)) {
			observers.add(obs);
		}
		obs.imageUpdate(this, flags, 0, 0, width, height);
	}

	private void notifyObservers(int flags) {
		for (TImageObserver obs : observers) {
			obs.imageUpdate(this, flags, 0, 0, width, height);
		}
	}

	private final class ProducerConsumer implements TImageConsumer {

		private TColorModel cm;
		private int hints;

		@Override
		public void setProperties(Hashtable<?, ?> props) {

		}

		@Override
		public void setDimensions(int w, int h) {
			width = w;
			height = h;

			// allocate backing store now that we know size:
			if (bufferedImage == null) {
				// Use whatever BufferedImage impl you already have
				bufferedImage = new TBufferedImage(w, h, TBufferedImage.TYPE_INT_ARGB);
			}

			notifyObservers(TImageObserver.WIDTH | TImageObserver.HEIGHT);
		}

		@Override
		public void setColorModel(TColorModel model) {
			this.cm = model;
			// You can store and convert to ARGB in setPixels
		}

		@Override
		public void setHints(int hints) {
			this.hints = hints;
			// Optional – can be ignored
		}

		@Override
		public void setPixels(int x, int y, int w, int h,
							  TColorModel model,
							  int[] pixels, int off, int scansize) {

			if (bufferedImage == null && width > 0 && height > 0) {
				bufferedImage = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
			}

			TWritableRaster raster = bufferedImage.getRaster();

			// Convert to ARGB and stuff into the raster’s DataBuffer
			for (int row = 0; row < h; row++) {
				int srcIndex = off + row * scansize;
				for (int col = 0; col < w; col++) {
					int argb = model.getRGB(pixels[srcIndex++]);
					raster.setDataElements(x + col, y + row, new int[]{argb});
				}
			}

			notifyObservers(TImageObserver.SOMEBITS);
		}

		@Override
		public void setPixels(int x, int y, int w, int h,
							  TColorModel model,
							  byte[] pixels, int off, int scansize) {
			if (bufferedImage == null && width > 0 && height > 0) {
				bufferedImage = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
			}
			TWritableRaster raster = bufferedImage.getRaster();
			// Convert to ARGB and stuff into the raster’s DataBuffer
			for (int row = 0; row < h; row++) {
				int srcIndex = off + row * scansize;
				for (int col = 0; col < w; col++) {
					int argb = model.getRGB(pixels[srcIndex++] & 0xFF);
					raster.setDataElements(x + col, y + row, new int[]{argb});
				}
			}
		}

		@Override
		public void imageComplete(int status) {
			// status: IMAGEABORTED, IMAGEERROR, SINGLEFRAMEDONE, STATICIMAGEDONE
			notifyObservers(TImageObserver.ALLBITS | TImageObserver.FRAMEBITS);
			// You might also mark a "complete" flag here
		}
	}
}
