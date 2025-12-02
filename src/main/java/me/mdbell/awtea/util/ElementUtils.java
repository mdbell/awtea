package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

@UtilityClass
public class ElementUtils {

    @JSBody(params = {"element"}, script = "return document.fullscreenElement === element;")
    public native boolean isFullscreen(HTMLElement element);
}
