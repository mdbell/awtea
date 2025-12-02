package me.mdbell.awtea.support;

import org.teavm.jso.canvas.ImageData;

public interface ImageDataConsumer {

	default void putImageData(ImageData data){
		putImageData(0, 0, data.getWidth(), data.getHeight(), data);
	}

	default void putImageData(int x, int y, ImageData data){
		putImageData(x, y, data.getWidth(), data.getHeight(), data);
	}

	void putImageData(int x, int y, int w, int h, ImageData data);
}
