package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;

@RequiredArgsConstructor
@Getter
public abstract class TCanvasGraphics extends TGraphics {

	protected final HTMLCanvasElement canvas;

	public abstract void putImageData(int x, int y, ImageData imageData);

}
