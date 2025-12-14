#include "awt_log.h"
#include <stdio.h>
#include <string.h>

#if ENABLE_WASM_LOGGING

// Circular log buffer
static uint8_t g_log_buffer[LOG_BUFFER_SIZE];
static uint32_t g_log_write_pos = 0;
static uint32_t g_log_read_pos = 0;

// Write a log entry to the circular buffer
static void write_log_entry(LogLevel level, const char* message, uint32_t message_len) {
    if (message_len > LOG_MESSAGE_MAX_SIZE - 2) {
        message_len = LOG_MESSAGE_MAX_SIZE - 2;
    }
    
    // Calculate total entry size (level byte + length byte + message)
    uint32_t entry_size = 2 + message_len;
    
    // Check if we need to wrap around or if there's enough space
    uint32_t available = LOG_BUFFER_SIZE - g_log_write_pos;
    
    if (entry_size > available) {
        // Not enough space at the end, wrap to beginning
        g_log_write_pos = 0;
    }
    
    // Check if we would overwrite unread data
    if (g_log_write_pos < g_log_read_pos && g_log_write_pos + entry_size > g_log_read_pos) {
        // Would overwrite unread data, advance read position
        g_log_read_pos = g_log_write_pos + entry_size;
        if (g_log_read_pos >= LOG_BUFFER_SIZE) {
            g_log_read_pos = 0;
        }
    }
    
    // Write the log entry
    g_log_buffer[g_log_write_pos] = (uint8_t)level;
    g_log_buffer[g_log_write_pos + 1] = (uint8_t)message_len;
    
    if (message_len > 0) {
        memcpy(&g_log_buffer[g_log_write_pos + 2], message, message_len);
    }
    
    g_log_write_pos += entry_size;
    if (g_log_write_pos >= LOG_BUFFER_SIZE) {
        g_log_write_pos = 0;
    }
}

// Internal log function with va_list
void wasm_log(LogLevel level, const char* format, va_list args) {
    char buffer[LOG_MESSAGE_MAX_SIZE - 2];
    int len = vsnprintf(buffer, sizeof(buffer), format, args);
    
    if (len < 0) {
        return; // Encoding error
    }
    
    if (len >= (int)sizeof(buffer)) {
        len = sizeof(buffer) - 1;
    }
    
    write_log_entry(level, buffer, (uint32_t)len);
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

// Exported functions for host to access log buffer
uint32_t get_log_buffer_ptr(void) {
    return (uint32_t)(uintptr_t)g_log_buffer;
}

uint32_t get_log_buffer_size(void) {
    return LOG_BUFFER_SIZE;
}

void flush_log_buffer(void) {
    // Reset buffer pointers
    g_log_write_pos = 0;
    g_log_read_pos = 0;
}

#endif
