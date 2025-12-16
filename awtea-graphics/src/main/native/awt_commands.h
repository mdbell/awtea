#pragma once
#include "awt_raster_internal.h"

// Legacy exports (deprecated, kept for backward compatibility)
__attribute__((export_name("get_command_size")))
int get_command_size(void);

__attribute__((export_name("request_command_buffer")))
int request_command_buffer(int max_commands);

// Main rendering function
// Note: bytesUsed parameter is now in bytes (not command count)
// cmdPtr should be 0 to use context's internal buffer
__attribute__((export_name("render_awt")))
int render_awt(int context_id, uint32_t cmdPtr, int bytesUsed);