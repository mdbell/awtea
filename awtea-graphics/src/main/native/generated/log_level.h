/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/log-level.yaml
 * 
 * Logging levels ordered from most to least severe
 */
#pragma once

#include <stdint.h>

typedef enum {
    // Error messages
    LOG_LEVEL_ERROR = 0,

    // Warning messages
    LOG_LEVEL_WARN = 1,

    // Informational messages
    LOG_LEVEL_INFO = 2,

    // Debug messages
    LOG_LEVEL_DEBUG = 3,

    // Trace messages
    LOG_LEVEL_TRACE = 4,
} LogLevel;
