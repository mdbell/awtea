package me.mdbell.awtea.support;

import org.teavm.jso.canvas.ImageData;

public interface ImageDataProvider {

	public default ImageData getImageData(){
		return getImageData(0, 0, getWidth(), getHeight());
	}

	public boolean isDirty();

	public void markClean();

	public int getWidth();

	public int getHeight();

	public ImageData getImageData(int x, int y, int width, int height);
}
