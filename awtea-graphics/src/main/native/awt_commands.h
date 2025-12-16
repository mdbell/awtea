#pragma once
#include "awt_raster_internal.h"

// Main rendering function
// Note: bytesUsed parameter is now in bytes (not command count)
// cmdPtr should be 0 to use context's internal buffer
__attribute__((export_name("render_awt")))
int render_awt(int context_id, uint32_t cmdPtr, int bytesUsed);