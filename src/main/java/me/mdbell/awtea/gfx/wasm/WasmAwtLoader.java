package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

final class WasmAwtLoader {
	private WasmAwtLoader() {
	}

	@JSBody(params = {"url"}, script =
		"return (function(url) {" +
			"  var imports = {" +
			"    env: {" +
			"      abort: function () {" +
			"        console.error('abort called in wasm');" +
			"      }" +
			"    }" +
			"  };" +
			"  return WebAssembly" +
			"    .instantiateStreaming(fetch(url), imports)" +
			"    .then(function (result) {" +
			"      return result.instance.exports;" +
			"    });" +
			"})(url);"
	)
	public static native JSPromise<WasmAwtRasterizerExports> load(String url);
}
