package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import me.mdbell.awtea.util.logging.LogLevel;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Handles retrieval and routing of logs from the WASM rasterizer module
 * to the Java logging system.
 */
public class WasmLogHandler {
    
    private static final Logger log = LoggerFactory.getLogger("wasm.rasterizer");
    
    // Must match C definitions in awt_log.h
    private static final int LOG_LEVEL_ERROR = 0;
    private static final int LOG_LEVEL_WARN = 1;
    private static final int LOG_LEVEL_INFO = 2;
    private static final int LOG_LEVEL_DEBUG = 3;
    private static final int LOG_MESSAGE_MAX_SIZE = 256;
    
    private final WasmAwtRasterizerExports exports;
    private final WasmMemory memory;
    private int logBufferPtr = -1;
    private int logBufferSize = -1;
    
    public WasmLogHandler(WasmAwtRasterizerExports exports) {
        this.exports = exports;
        this.memory = exports.getMemory();
    }
    
    /**
     * Poll the WASM log buffer and route any pending log messages
     * to the Java logging system.
     */
    public void pollLogs() {
        // Lazy initialization of log buffer info
        if (logBufferPtr < 0) {
            logBufferPtr = exports.getLogBufferPtr();
            logBufferSize = exports.getLogBufferSize();
            
            if (logBufferPtr == 0 || logBufferSize == 0) {
                // Logging is disabled in WASM module
                return;
            }
        }
        
        // Read the log buffer from WASM memory
        ArrayBuffer memoryBuffer = memory.getBuffer();
        Int8Array buffer = Int8Array.create(memoryBuffer);
        
        int pos = 0;
        while (pos + 2 <= logBufferSize) {
            // Read log entry header
            int level = buffer.get(logBufferPtr + pos) & 0xFF;
            int messageLen = buffer.get(logBufferPtr + pos + 1) & 0xFF;
            
            // Check if this is the end of valid entries (zero level and length)
            if (level == 0 && messageLen == 0) {
                break;
            }
            
            pos += 2;
            
            // Validate message length
            if (messageLen > LOG_MESSAGE_MAX_SIZE - 2) {
                break; // Invalid entry
            }
            
            if (pos + messageLen > logBufferSize) {
                break; // Would read past buffer end
            }
            
            // Read message text
            if (messageLen > 0) {
                byte[] messageBytes = new byte[messageLen];
                for (int i = 0; i < messageLen; i++) {
                    messageBytes[i] = buffer.get(logBufferPtr + pos + i);
                }
                
                String message = new String(messageBytes);
                
                // Route to appropriate log level
                switch (level) {
                    case LOG_LEVEL_ERROR:
                        if (log.isErrorEnabled()) {
                            log.error(message);
                        }
                        break;
                    case LOG_LEVEL_WARN:
                        if (log.isWarnEnabled()) {
                            log.warn(message);
                        }
                        break;
                    case LOG_LEVEL_INFO:
                        if (log.isInfoEnabled()) {
                            log.info(message);
                        }
                        break;
                    case LOG_LEVEL_DEBUG:
                        if (log.isDebugEnabled()) {
                            log.debug(message);
                        }
                        break;
                    default:
                        // Unknown log level, treat as info
                        log.info("[UNKNOWN_LEVEL_{}] {}", level, message);
                        break;
                }
                
                pos += messageLen;
            }
        }
        
        // Flush the buffer after reading
        if (pos > 0) {
            exports.flushLogBuffer();
        }
    }
    
    /**
     * Flush the WASM log buffer without reading.
     * Useful for cleanup.
     */
    public void flush() {
        if (logBufferPtr > 0) {
            exports.flushLogBuffer();
        }
    }
}
