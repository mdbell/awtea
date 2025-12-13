package me.mdbell.awtea.classlib.java.awt.image;

import lombok.AllArgsConstructor;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import me.mdbell.awtea.classlib.java.awt.TImage;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.support.ImageDataProvider;

@AllArgsConstructor
public class TPixelGrabber {

    private final TImage img;
	private final int x;
	private final int y;
	private final int w;
	private final int h;
	private final int[] pix;
	private final int off;
	private final int scansize;

	private void grabFromBufferedImage(TBufferedImage img, int x, int y, int w, int h, int[] pix, int off, int scansize) {
		TColorModel colorModel = img.getColorModel();
		TDataBuffer dataBuffer = img.getRaster().getBuffer();

		if(colorModel == null) {
			throw new IllegalStateException("Image has no color model");
		}

		if(dataBuffer == null) {
			throw new IllegalStateException("Image has no data buffer");
		}

		if(!(dataBuffer instanceof TDataBufferInt)) {
			throw new IllegalStateException("Only INT data buffer is supported");
		}

		TDataBufferInt intBuffer = (TDataBufferInt)dataBuffer;

		int[] imgPixels = intBuffer.getData();

		for (int row = 0; row < h; row++) {
			for (int col = 0; col < w; col++) {
				int imgX = x + col;
				int imgY = y + row;
				int imgIndex = imgY * img.getWidth() + imgX;
				int pixel = imgPixels[imgIndex];

				// Convert pixel using the color model to ARGB
				int argb = colorModel.getRGB(pixel);

				// Store in the pix array considering the scanline offset
				pix[off + row * scansize + col] = argb;
			}
		}
	}

	private void grabFromImageDataProvider(ImageDataProvider img, int x, int y, int w, int h, int[] pix, int off, int scansize) {
		ImageData data = img.getImageData(x, y, w, h);
        Uint8ClampedArray arr = data.getData();

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                int index = (row * w + col) * 4;  // ImageData is in RGBA format
                int r = arr.get(index);
                int g = arr.get(index + 1);
                int b = arr.get(index + 2);
                int a = arr.get(index + 3); // Alpha channel

                // Convert RGBA → ARGB (shifting alpha to the highest byte)
                int argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) |
                        ((g & 0xFF) << 8) | (b & 0xFF);

                // Store in the pix array considering the scanline offset
                pix[off + row * scansize + col] = argb;
            }
        }
	}

	public boolean grabPixels() throws InterruptedException {
		if (img instanceof TBufferedImage) {
			grabFromBufferedImage((TBufferedImage) img, x, y, w, h, pix, off, scansize);
			return true;
		}
		if (img instanceof ImageDataProvider) {
			grabFromImageDataProvider((ImageDataProvider) img, x, y, w, h, pix, off, scansize);
			return true;
		}
		throw Debug.unimplemented();
    }

}
