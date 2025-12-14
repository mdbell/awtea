#include "awt_log.h"
#include <stdio.h>
#include <string.h>

#if ENABLE_WASM_LOGGING

// Internal log function with va_list
void wasm_log(LogLevel level, const char* format, va_list args) {
    char buffer[LOG_MESSAGE_MAX_SIZE];
    int len = vsnprintf(buffer, sizeof(buffer), format, args);
    
    if (len < 0) {
        return; // Encoding error
    }
    
    if (len >= (int)sizeof(buffer)) {
        len = sizeof(buffer) - 1;
    }
    
    // Call the imported JavaScript callback
    wasm_log_callback((int)level, buffer, len);
}

// Public logging functions
void log_error(const char* format, ...) {
    va_list args;
    va_start(args, format);
    wasm_log(LOG_LEVEL_ERROR, format, args);
    va_end(args);
}

void log_warn(const char* format, ...) {
    va_list args;
    va_start(args, format);
    wasm_log(LOG_LEVEL_WARN, format, args);
    va_end(args);
}

void log_info(const char* format, ...) {
    va_list args;
    va_start(args, format);
    wasm_log(LOG_LEVEL_INFO, format, args);
    va_end(args);
}

void log_debug(const char* format, ...) {
    va_list args;
    va_start(args, format);
    wasm_log(LOG_LEVEL_DEBUG, format, args);
    va_end(args);
}

#endif
