package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

final class WasmAwtLoader {
	private WasmAwtLoader() {
	}

	private static Logger logger;
	
	/**
	 * Get the logger instance for WASM logs.
	 * Called from JavaScript bridge.
	 */
	public static Logger $getLogger() {
		if (logger == null) {
			logger = LoggerFactory.getLogger("wasm.rasterizer");
		}
		return logger;
	}

	/**
	 * JavaScript callback for WASM log messages.
	 * Called directly from WASM when log functions are invoked.
	 */
	@JSBody(params = {"level", "message"}, script = 
		"var logger = $rt_nullCheck(me_mdbell_awtea_gfx_wasm_WasmAwtLoader_$getLogger());" +
		"if (level === 0) {" +  // ERROR
		"  logger.$error$java_lang_String(message);" +
		"} else if (level === 1) {" +  // WARN
		"  logger.$warn$java_lang_String(message);" +
		"} else if (level === 2) {" +  // INFO
		"  logger.$info$java_lang_String(message);" +
		"} else if (level === 3) {" +  // DEBUG
		"  logger.$debug$java_lang_String(message);" +
		"}"
	)
	private static native void logFromWasm(int level, String message);

	@JSBody(params = {"url"}, script =
		"return (function(url) {" +
			"  var instance = null;" +
			"  var logCallback = function(level, messagePtr, messageLen) {" +
			"    try {" +
			"      if (!instance) {" +
			"        console.error('WASM instance not yet initialized');" +
			"        return;" +
			"      }" +
			"      var bytes = new Uint8Array(instance.exports.memory.buffer, messagePtr, messageLen);" +
			"      var message = new TextDecoder('utf-8').decode(bytes);" +
			"      me_mdbell_awtea_gfx_wasm_WasmAwtLoader_logFromWasm(level, $rt_str(message));" +
			"    } catch (e) {" +
			"      console.error('Error in log callback:', e);" +
			"    }" +
			"  };" +
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
	public static native JSPromise<WasmAwtRasterizerExports> load(String url);
}
