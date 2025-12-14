#pragma once

#include <stdint.h>
#include <stdarg.h>
#include "awt_imports.h"

// Include auto-generated enum
#include "generated/log_level.h"

// Note: LogLevel enum is now defined in generated/log_level.h
// Edit schemas/log-level.yaml to modify the enum values

// Maximum size of a single log message
#define LOG_MESSAGE_MAX_SIZE 512

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
void log_trace(const char* format, ...);

// Internal log function
void wasm_log(LogLevel level, const char* format, va_list args);

#else

// No-op macros when logging is disabled
#define log_error(format, ...) ((void)0)
#define log_warn(format, ...) ((void)0)
#define log_info(format, ...) ((void)0)
#define log_debug(format, ...) ((void)0)
#define log_trace(format, ...) ((void)0)

#endif
