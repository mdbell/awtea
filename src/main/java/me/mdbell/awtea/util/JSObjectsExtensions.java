package me.mdbell.awtea.util;

import me.mdbell.awtea.util.jso.JSRecord;
import me.mdbell.awtea.util.jso.MediaQueryList;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.TypedArray;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.util.List;

public final class JSObjectsExtensions {

	public static void cleanup(List<Registration> registrationList) {
		registrationList.removeIf(r -> {
			r.dispose();
			return true;
		});
	}

	public static void track(Registration registration, List<Registration> registrationList) {
		registrationList.add(registration);
	}

	public static <T extends JSObject> boolean nullish(T obj) {
		return obj == null || JSObjects.isUndefined(obj);
	}

	@JSBody(params = {"arr", "begin", "end"}, script = "arr.subarray(begin, end)")
	public static native <T extends TypedArray> T subarray(T arr, int begin, int end);

	@JSBody(params = {"context", "enabled"}, script = "context.imageSmoothingEnabled = enabled;")
	public static native void setImageSmoothingEnabled(CanvasRenderingContext2D context, boolean enabled);

	@JSBody(params = {"window", "query"}, script = "return window.matchMedia(query);")
	public static native MediaQueryList matchMedia(Window window, String query);

	public static CanvasRenderingContext2D getContext2d(HTMLCanvasElement element, boolean frequentReads) {
		JSRecord options = JSRecord.create();
		options.put("alpha", false);
//        options.put("willReadFrequently", JSBoolean.valueOf(frequentReads));
		return (CanvasRenderingContext2D) element.getContext("2d", options);
	}

	@JSBody(params = {"blob"}, script = "return (window.URL || window.webkitURL).createObjectURL(blob);")
	public static native String createObjectUrl(JSObject blob);

	@JSBody(params = {"url"}, script = "return (window.URL || window.webkitURL).revokeObjectURL(url);")
	public static native void revokeObjectUrl(String url);

	@JSByRef
	@JSBody(params = {"arr"}, script = "return arr;")
	public static native byte[] getArrayFromJS(Uint8ClampedArray arr);
}
