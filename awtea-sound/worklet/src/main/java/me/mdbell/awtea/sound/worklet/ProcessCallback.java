package me.mdbell.awtea.sound.worklet;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
public interface ProcessCallback {
    boolean process(@JSByRef float[][] inputs, @JSByRef float[][] outputs, JSObject parameters);
}
