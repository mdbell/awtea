package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

public final class ConsoleBridge {

	@JSFunctor
	public interface LogCallback extends JSObject {
		void onLog(String level, String message);
	}

	private ConsoleBridge() {
	}

	@JSBody(params = "callback", script =
		"  if (window.__awConsoleHookInstalled) return;       \n" +
			"  window.__awConsoleHookInstalled = true;            \n" +
			"  var origLog = console.log.bind(console);           \n" +
			"  var origWarn = console.warn.bind(console);         \n" +
			"  var origErr = console.error.bind(console);         \n" +
			"                                                     \n" +
			"  function joinArgs(args) {                          \n" +
			"    try {                                            \n" +
			"      return Array.prototype.join.call(args, ' ');   \n" +
			"    } catch (e) {                                    \n" +
			"      return '' + args;                              \n" +
			"    }                                                \n" +
			"  }                                                  \n" +
			"                                                     \n" +
			"  console.log = function() {                         \n" +
			"    var msg = joinArgs(arguments);                   \n" +
			"    callback('log', msg);                      \n" +
			"    origLog.apply(console, arguments);               \n" +
			"  };                                                 \n" +
			"  console.info = function() {                         \n" +
			"    var msg = joinArgs(arguments);                   \n" +
			"    callback('info', msg);                      \n" +
			"    origLog.apply(console, arguments);               \n" +
			"  };                                                 \n" +
			"  console.warn = function() {                        \n" +
			"    var msg = joinArgs(arguments);                   \n" +
			"    callback('warn', msg);                     \n" +
			"    origWarn.apply(console, arguments);              \n" +
			"  };                                                 \n" +
			"  console.error = function() {                       \n" +
			"    var msg = joinArgs(arguments);                   \n" +
			"    callback('error', msg);                    \n" +
			"    origErr.apply(console, arguments);               \n" +
			"  };                                                 ")
	public static native void install(LogCallback callback);
}
