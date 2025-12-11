package me.mdbell.awtea.gfx.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

public final class WasmAwtLoader {
    private WasmAwtLoader() {
    }

    @JSBody(params = {"url"}, script =
            "return (function(url) {" +
                    "  var wasi = {" +
                    "    proc_exit: function (code) {" +
                    "      console.log('WASI proc_exit called with code', code);" +
                    "    }," +
                    "    fd_write: function (fd, iovs, iovs_len, nwritten) {" +
                    "      return 0;" +
                    "    }," +
                    "    fd_close: function (fd) { return 0; }," +
                    "    fd_fdstat_get: function (fd, buf) { return 0; }," +
                    "    fd_seek: function (fd, offset_low, offset_high, whence, newOffsetPtr) { return 0; }," +
                    "    fd_read: function (fd, iovs, iovs_len, nread) { return 0; }," +
                    "    clock_time_get: function (clockid, precision, timePtr) { return 0; }," +
                    "    random_get: function (buf, buf_len) { return 0; }" +
                    "  };" +
                    "  var imports = {" +
                    "    env: {" +
                    "      abort: function () {" +
                    "        console.error('abort called in wasm');" +
                    "      }" +
                    "    }," +
                    "    wasi_snapshot_preview1: wasi" +
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
