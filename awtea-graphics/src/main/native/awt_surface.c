#include "awt_memory.h"
#include "awt_surface.h"
#include "awt_util.h"
#include "awt_log.h"

SurfaceData g_surfaces[NUM_SURFACES];
SurfaceContext g_contexts[NUM_CONTEXTS];

void init_surface_system(void) {
    // Initialize all contexts to mark them as free
    for (int i = 0; i < NUM_CONTEXTS; i++) {
        g_contexts[i].surface_id = -1;
    }
    log_info("Initialized surface system: %d surfaces, %d contexts", 
             NUM_SURFACES, NUM_CONTEXTS);
}

SurfaceData* get_surface_data(int id) {
    if (id < START_SURFACE_ID || id >= END_SURFACE_ID) return NULL;
    return &g_surfaces[id - START_SURFACE_ID];
}

SurfaceContext* get_context_data(int id) {
    if (id < START_CONTEXT_ID || id >= END_CONTEXT_ID) return NULL;
    return &g_contexts[id - START_CONTEXT_ID];
}

int find_free_surface() {
    for (int i = 0; i < NUM_SURFACES; i++) {
        if (g_surfaces[i].ptr == 0) {
            return i + START_SURFACE_ID;
        }
    }
    log_warn("find_free_surface: no free surface available");
    return -1; // no free surface
}

int reset_surface(int surface_id, int layer, int width, int height, PixelFormat format) {
    log_debug("reset_surface: id=%d, layer=%d, size=%dx%d, format=%d", 
              surface_id, layer, width, height, format);

    if (surface_id < START_SURFACE_ID || surface_id >= END_SURFACE_ID)
    {
        log_error("Invalid surface ID: %d (range: %d-%d)", 
                  surface_id, START_SURFACE_ID, END_SURFACE_ID - 1);
        return -3;
    }

    SurfaceData* surface = get_surface_data(surface_id);

    if(!surface) {
        log_error("Failed to get surface data for ID: %d", surface_id);
        return -2;
    }

    if(surface->ptr) {
        free((void*)(uintptr_t)surface->ptr);
    }

    memset(surface, 0, sizeof(SurfaceData));

    if(width == 0 || height == 0 || layer < 0) {
        return 0; // zero-sized surface (freeing the surface)
    }

    surface->layer = (uint32_t)layer;
    surface->width = width;
    surface->height = height;
    surface->stride = width * sizeof(uint32_t);
    surface->format = format;
    surface->ref_count = 0; // will be incremented when first context is created

    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = tracked_malloc(bytes);
    if (!p) {
        log_error("Failed to allocate %zu bytes for surface %d (%dx%d)", 
                  bytes, surface_id, width, height);
        surface->ptr = 0;
        surface->width = 0;
        surface->height = 0;
        return -1;
    }
    surface->ptr = (uint32_t)(uintptr_t)p;

    log_info("Created surface %d: %dx%d, %zu bytes", surface_id, width, height, bytes);

    return 0;
}

uint32_t get_surface_pixels_ptr(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->ptr;
}

int get_surface_width(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->width;
}

int get_surface_height(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->height;
}

int get_surface_stride(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->stride;
}

int clip_x(int x, const RenderSurface* surf) {
    int min_x = 0;
    int max_x = surf->width;
    
    // If clip rectangle is set, intersect with clip bounds
    if (surf->clip.width > 0) {
        min_x = surf->clip.x > 0 ? surf->clip.x : 0;
        max_x = surf->clip.x + surf->clip.width;
        if (max_x > surf->width) {
            max_x = surf->width;
        }
    }
    
    return clamp_int(x, min_x, max_x);
}

int clip_y(int y, const RenderSurface* surf) {
    int min_y = 0;
    int max_y = surf->height;
    
    // If clip rectangle is set, intersect with clip bounds
    if (surf->clip.height > 0) {
        min_y = surf->clip.y > 0 ? surf->clip.y : 0;
        max_y = surf->clip.y + surf->clip.height;
        if (max_y > surf->height) {
            max_y = surf->height;
        }
    }
    
    return clamp_int(y, min_y, max_y);
}

int find_free_context() {
    for (int i = 0; i < NUM_CONTEXTS; i++) {
        if (g_contexts[i].surface_id == -1) {
            return i + START_CONTEXT_ID;
        }
    }
    log_warn("find_free_context: no free context available");
    return -1; // no free context
}

int create_context(int surface_id) {
    log_debug("create_context: surface_id=%d", surface_id);
    
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface || !surface->ptr) {
        log_error("create_context: invalid surface %d", surface_id);
        return -1; // invalid surface
    }

    int context_id = find_free_context();
    if (context_id == -1) {
        log_error("create_context: no free context for surface %d", surface_id);
        return -1; // no free context
    }

    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx) {
        log_error("create_context: failed to get context data for id %d", context_id);
        return -1; // should not happen
    }

    // Initialize context with default state
    ctx->surface_id = surface_id;
    ctx->argb[COLOR_FG] = DEFAULT_FG_COLOR;
    ctx->argb[COLOR_BG] = DEFAULT_BG_COLOR;

    // identity transform
    ctx->transform.m00 = 1.0f;
    ctx->transform.m01 = 0.0f;
    ctx->transform.m02 = 0.0f;
    ctx->transform.m10 = 0.0f;
    ctx->transform.m11 = 1.0f;
    ctx->transform.m12 = 0.0f;

    // clip_rect to full surface
    ctx->clip.x = 0;
    ctx->clip.y = 0;
    ctx->clip.width = surface->width;
    ctx->clip.height = surface->height;

    // Allocate fixed command buffer
    ctx->max_commands = MAX_CONTEXT_COMMANDS;
    size_t bytes = ctx->max_commands * sizeof(SurfaceCommand);
    ctx->command_buffer = (SurfaceCommand*)tracked_malloc(bytes);
    if (!ctx->command_buffer) {
        log_error("create_context: failed to allocate command buffer (%zu bytes)", bytes);
        ctx->surface_id = -1; // mark context as free again
        return -1;
    }
    memset(ctx->command_buffer, 0, bytes);

    // Increment surface reference count
    surface->ref_count++;

    log_info("Created context %d for surface %d (ref_count=%d)", 
             context_id, surface_id, surface->ref_count);

    return context_id;
}

int clone_context(int context_id) {
    log_debug("clone_context: context_id=%d", context_id);
    
    SurfaceContext* src_ctx = get_context_data(context_id);
    if (!src_ctx || src_ctx->surface_id == -1) {
        log_error("clone_context: invalid context %d", context_id);
        return -1; // invalid context
    }

    int new_context_id = find_free_context();
    if (new_context_id == -1) {
        log_error("clone_context: no free context to clone from %d", context_id);
        return -1; // no free context
    }

    SurfaceContext* new_ctx = get_context_data(new_context_id);
    if (!new_ctx) {
        log_error("clone_context: failed to get new context data for id %d", new_context_id);
        return -1; // should not happen
    }

    // Copy all state from source context
    new_ctx->surface_id = src_ctx->surface_id;
    new_ctx->argb[COLOR_FG] = src_ctx->argb[COLOR_FG];
    new_ctx->argb[COLOR_BG] = src_ctx->argb[COLOR_BG];
    new_ctx->transform = src_ctx->transform;
    new_ctx->clip = src_ctx->clip;

    // Allocate new command buffer for the cloned context
    new_ctx->max_commands = MAX_CONTEXT_COMMANDS;
    size_t bytes = new_ctx->max_commands * sizeof(SurfaceCommand);
    new_ctx->command_buffer = (SurfaceCommand*)tracked_malloc(bytes);
    if (!new_ctx->command_buffer) {
        log_error("clone_context: failed to allocate command buffer (%zu bytes)", bytes);
        new_ctx->surface_id = -1; // mark context as free again
        return -1;
    }
    memset(new_ctx->command_buffer, 0, bytes);

    // Increment surface reference count
    SurfaceData* surface = get_surface_data(src_ctx->surface_id);
    if (surface) {
        surface->ref_count++;
        log_info("Cloned context %d to %d for surface %d (ref_count=%d)", 
                 context_id, new_context_id, src_ctx->surface_id, surface->ref_count);
    }

    return new_context_id;
}

int destroy_context(int context_id) {
    log_debug("destroy_context: context_id=%d", context_id);
    
    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        log_warn("destroy_context: invalid or already destroyed context %d", context_id);
        return -1; // invalid or already destroyed context
    }

    int surface_id = ctx->surface_id;
    SurfaceData* surface = get_surface_data(surface_id);
    
    // Free the command buffer
    if (ctx->command_buffer) {
        tracked_free(ctx->command_buffer);
        ctx->command_buffer = NULL;
        ctx->max_commands = 0;
    }
    
    // Mark context as free
    ctx->surface_id = -1;

    // Decrement surface reference count
    if (surface && surface->ref_count > 0) {
        surface->ref_count--;
        log_info("Destroyed context %d for surface %d (ref_count=%d)", 
                 context_id, surface_id, surface->ref_count);
        
        // If no more references, we could optionally free the surface
        // But for now, we leave it allocated until explicitly reset
    }

    return 0;
}

int create_reference(int surface_id) {
    log_debug("create_reference: surface_id=%d", surface_id);
    
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface || !surface->ptr) {
        log_error("create_reference: invalid surface %d", surface_id);
        return -1; // invalid surface
    }

    surface->ref_count++;
    log_debug("Created reference for surface %d (ref_count=%d)", 
              surface_id, surface->ref_count);
    return surface_id;
}

int release_reference(int surface_id) {
    log_debug("release_reference: surface_id=%d", surface_id);
    
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface) {
        log_error("release_reference: invalid surface %d", surface_id);
        return -1; // invalid surface
    }

    if (surface->ref_count > 0) {
        surface->ref_count--;
        log_debug("Released reference for surface %d (ref_count=%d)", 
                  surface_id, surface->ref_count);
    } else {
        log_warn("release_reference: surface %d already has ref_count=0", surface_id);
    }
    
    return 0;
}

int get_context_surface_id(int context_id) {
    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        log_debug("get_context_surface_id: invalid context %d", context_id);
        return -1;
    }
    return ctx->surface_id;
}

int get_max_context_commands(void) {
    return MAX_CONTEXT_COMMANDS;
}

uint32_t get_context_command_buffer_ptr(int context_id) {
    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || !ctx->command_buffer) {
        log_error("get_context_command_buffer_ptr: invalid context %d or no buffer", context_id);
        return 0;
    }
    return (uint32_t)(uintptr_t)ctx->command_buffer;
}