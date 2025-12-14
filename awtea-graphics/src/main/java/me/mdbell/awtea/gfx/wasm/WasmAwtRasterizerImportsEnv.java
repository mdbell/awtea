package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface WasmAwtRasterizerImportsEnv extends JSObject {

    @JSProperty("abort")
    void setAbortCallback(AbortCallback callback);

    @JSProperty("wasm_log_callback")
    void setLogCallback(LogCallback callback);

    @JSProperty("wasm_get_time_ms")
    void setTimingCallback(TimingCallback callback);

    @JSProperty("wasm_report_memory_usage")
    void setMemoryCallback(MemoryCallback callback);

    @JSProperty("wasm_assertion_failed")
    void setAssertionCallback(AssertionCallback callback);

    @JSFunctor
    interface AbortCallback extends JSObject {
        void abort();
    }

    @JSFunctor
    interface LogCallback extends JSObject {
        void log(int level, int ptr, int len);
    }

    @JSFunctor
    interface TimingCallback extends JSObject {
        double getTimeMs();
    }

    @JSFunctor
    interface MemoryCallback extends JSObject {
        void reportMemory(int allocatedBytes, int allocatedCount, int peakBytes);
    }

    @JSFunctor
    interface AssertionCallback extends JSObject {
        void assertionFailed(int exprPtr, int exprLen, int filePtr, int fileLen, int line);
    }
}
