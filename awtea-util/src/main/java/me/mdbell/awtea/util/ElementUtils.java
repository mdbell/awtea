package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Element;

@UtilityClass
public class ElementUtils {

	@JSBody(params = {"element"}, script = "return document.fullscreenElement === element;")
	public native boolean isFullscreen(HTMLElement element);

	@JSBody(params = {"element"}, script = "return element.firstElementChild;")
	public native Element getFirstElementChild(Element element);

	@JSBody(params = {"element"}, script = "return element.nextElementSibling;")
	public native Element getNextElementSibling(Element element);
}
