package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

final class WasmAwtLoader {
    private WasmAwtLoader() {
    }

    @JSBody(params = {"url", "logCallback"}, script =
            "return (function(url) {" +
                    "  var instance = null;" +
                    "  var imports = {" +
                    "    env: {" +
                    "      abort: function () {" +
                    "        console.error('abort called in wasm');" +
                    "      }," +
                    "      wasm_log_callback: logCallback" +
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
    public static native JSPromise<WasmAwtRasterizerExports> load(String url, LogCallback logCallback);

    @JSFunctor
    public interface LogCallback extends JSObject {
        void log(int level, int ptr, int len);
    }
}
