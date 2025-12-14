#pragma once

#include <stdint.h>
#include <stdarg.h>

// Log levels (must match Java LogLevel enum priority)
typedef enum {
    LOG_LEVEL_ERROR = 0,
    LOG_LEVEL_WARN = 1,
    LOG_LEVEL_INFO = 2,
    LOG_LEVEL_DEBUG = 3
} LogLevel;

// Maximum size of a single log message
#define LOG_MESSAGE_MAX_SIZE 256

// Total log buffer size (circular buffer)
#define LOG_BUFFER_SIZE 4096

// Control logging at compile time
#ifndef ENABLE_WASM_LOGGING
#define ENABLE_WASM_LOGGING 1
#endif

#if ENABLE_WASM_LOGGING

// Log functions with printf-style formatting
void log_error(const char* format, ...);
void log_warn(const char* format, ...);
void log_info(const char* format, ...);
void log_debug(const char* format, ...);

// Internal log function
void wasm_log(LogLevel level, const char* format, va_list args);

// Functions exported to host for log retrieval
uint32_t get_log_buffer_ptr(void);
uint32_t get_log_buffer_size(void);
void flush_log_buffer(void);

#else

// No-op macros when logging is disabled
#define log_error(format, ...) ((void)0)
#define log_warn(format, ...) ((void)0)
#define log_info(format, ...) ((void)0)
#define log_debug(format, ...) ((void)0)

// Export stubs
static inline uint32_t get_log_buffer_ptr(void) { return 0; }
static inline uint32_t get_log_buffer_size(void) { return 0; }
static inline void flush_log_buffer(void) {}

#endif

// Log entry structure in the buffer
typedef struct {
    uint8_t level;      // LogLevel
    uint8_t length;     // Length of message (0-255)
    char message[LOG_MESSAGE_MAX_SIZE - 2]; // Message text
} LogEntry;
