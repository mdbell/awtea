package me.mdbell.awtea.sound.worklet;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.typedarrays.Float32Array;

@JSFunctor
public interface ProcessCallback extends JSObject {
    boolean process(JSArray<JSArray<Float32Array>> inputs, JSArray<JSArray<Float32Array>> outputs, JSObject parameters);
}
