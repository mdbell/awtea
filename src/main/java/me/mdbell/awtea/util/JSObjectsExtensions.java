package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.ArrayBufferView;
import me.mdbell.awtea.util.jso.JSRecord;

public final class JSObjectsExtensions {

    public static <T extends JSObject> boolean nullish(T obj) {
        return obj == null || JSObjects.isUndefined(obj);
    }

	@JSBody(params = {"arr", "begin", "end"}, script = "arr.subarray(begin, end)")
	public static native <T extends ArrayBufferView> T subarray(T arr, int begin, int end);

    @JSBody(params = {"context", "enabled"}, script = "context.imageSmoothingEnabled = enabled;")
    public static native void setImageSmoothingEnabled(CanvasRenderingContext2D context, boolean enabled);

    public static CanvasRenderingContext2D getContext2d(HTMLCanvasElement element, boolean frequentReads) {
        JSRecord options = JSRecord.create();
        options.put("alpha", false);
//        options.put("willReadFrequently", JSBoolean.valueOf(frequentReads));
        return (CanvasRenderingContext2D) element.getContext("2d", options);
    }
}
