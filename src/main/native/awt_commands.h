#pragma once
#include "awt_raster_internal.h"

__attribute__((export_name("get_command_size")))
int get_command_size(void);

__attribute__((export_name("request_command_buffer")))
int request_command_buffer(int max_commands);

__attribute__((export_name("render_awt")))
int render_awt(int surface_id, uint32_t cmdPtr, int cmdCount);