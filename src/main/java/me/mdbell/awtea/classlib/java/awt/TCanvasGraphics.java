package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.teavm.jso.dom.html.HTMLCanvasElement;

@RequiredArgsConstructor
@Getter
public abstract class TCanvasGraphics extends TGraphics{

    private final HTMLCanvasElement canvas;

}
