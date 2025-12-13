package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface MediaQueryList extends JSObject {

	@JSProperty("matches")
	boolean getMatches();

	@JSProperty("media")
	String getMedia();
}
