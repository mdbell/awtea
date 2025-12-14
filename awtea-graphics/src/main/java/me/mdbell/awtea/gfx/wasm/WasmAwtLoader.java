package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

final class WasmAwtLoader {
    private WasmAwtLoader() {
    }

    @JSBody(params = {"url", "importsEnv"}, script =
            "return (function(url) {" +
                    "  var instance = null;" +
                    "  var imports = {" +
                    "    env: importsEnv" +
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
            WasmAwtRasterizerImportsEnv importsEnv
    );


}
