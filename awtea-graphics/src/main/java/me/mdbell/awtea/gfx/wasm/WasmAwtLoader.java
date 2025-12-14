package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

final class WasmAwtLoader {
    private WasmAwtLoader() {
    }

    @JSBody(params = {"url", "logCallback", "timingCallback", "memoryCallback", "assertionCallback"}, script =
            "return (function(url) {" +
                    "  var instance = null;" +
                    "  var imports = {" +
                    "    env: {" +
                    "      abort: function () {" +
                    "        console.error('abort called in wasm');" +
                    "      }," +
                    "      wasm_log_callback: logCallback," +
                    "      wasm_get_time_ms: timingCallback," +
                    "      wasm_report_memory_usage: memoryCallback," +
                    "      wasm_assertion_failed: assertionCallback" +
                    "    }" +
                    "  };" +
                    "  return WebAssembly" +
                    "    .instantiateStreaming(fetch(url), imports)" +
                    "    .then(function (result) {" +
                    "      instance = result.instance;" +
                    "      return instance.exports;" +
                    "    });" +
                    "})(url);"
    )
    public static native JSPromise<WasmAwtRasterizerExports> load(
            String url,
            LogCallback logCallback,
            TimingCallback timingCallback,
            MemoryCallback memoryCallback,
            AssertionCallback assertionCallback
    );

    @JSFunctor
    public interface LogCallback extends JSObject {
        void log(int level, int ptr, int len);
    }

    @JSFunctor
    public interface TimingCallback extends JSObject {
        double getTimeMs();
    }

    @JSFunctor
    public interface MemoryCallback extends JSObject {
        void reportMemory(int allocatedBytes, int peakBytes);
    }

    @JSFunctor
    public interface AssertionCallback extends JSObject {
        void assertionFailed(int exprPtr, int exprLen, int filePtr, int fileLen, int line);
    }
}
