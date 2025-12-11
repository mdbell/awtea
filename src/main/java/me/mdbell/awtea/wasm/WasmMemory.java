package me.mdbell.awtea.wasm;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * WebAssembly.Memory wrapper
 */
public interface WasmMemory extends JSObject {
    @JSProperty("buffer")
    ArrayBuffer getBuffer();
}
