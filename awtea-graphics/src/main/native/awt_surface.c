#include "awt_surface.h"
#include "awt_util.h"

SurfaceData g_surfaces[NUM_SURFACES];
SurfaceContext g_contexts[NUM_CONTEXTS];

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
    return -1; // no free surface
}

int reset_surface(int surface_id, int layer, int width, int height, PixelFormat format) {

    if (surface_id < START_SURFACE_ID || surface_id >= END_SURFACE_ID)
    {
        return -3;
    }

    SurfaceData* surface = get_surface_data(surface_id);

    if(!surface) {
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
    void* p = malloc(bytes);
    if (!p) {
        surface->ptr = 0;
        surface->width = 0;
        surface->height = 0;
        return -1;
    }
    surface->ptr = (uint32_t)(uintptr_t)p;

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
    x = clamp_int(x, 0, surf->width);
    return x;
}

int clip_y(int y, const RenderSurface* surf) {
    y = clamp_int(y, 0, surf->height);
    return y;
}

int find_free_context() {
    for (int i = 0; i < NUM_CONTEXTS; i++) {
        if (g_contexts[i].surface_id == -1) {
            return i + START_CONTEXT_ID;
        }
    }
    return -1; // no free context
}

int create_context(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface || !surface->ptr) {
        return -1; // invalid surface
    }

    int context_id = find_free_context();
    if (context_id == -1) {
        return -1; // no free context
    }

    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx) {
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

    // Increment surface reference count
    surface->ref_count++;

    return context_id;
}

int clone_context(int context_id) {
    SurfaceContext* src_ctx = get_context_data(context_id);
    if (!src_ctx || src_ctx->surface_id == -1) {
        return -1; // invalid context
    }

    int new_context_id = find_free_context();
    if (new_context_id == -1) {
        return -1; // no free context
    }

    SurfaceContext* new_ctx = get_context_data(new_context_id);
    if (!new_ctx) {
        return -1; // should not happen
    }

    // Copy all state from source context
    new_ctx->surface_id = src_ctx->surface_id;
    new_ctx->argb[COLOR_FG] = src_ctx->argb[COLOR_FG];
    new_ctx->argb[COLOR_BG] = src_ctx->argb[COLOR_BG];
    new_ctx->transform = src_ctx->transform;
    new_ctx->clip = src_ctx->clip;

    // Increment surface reference count
    SurfaceData* surface = get_surface_data(src_ctx->surface_id);
    if (surface) {
        surface->ref_count++;
    }

    return new_context_id;
}

int destroy_context(int context_id) {
    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        return -1; // invalid or already destroyed context
    }

    int surface_id = ctx->surface_id;
    SurfaceData* surface = get_surface_data(surface_id);
    
    // Mark context as free
    ctx->surface_id = -1;

    // Decrement surface reference count
    if (surface && surface->ref_count > 0) {
        surface->ref_count--;
        
        // If no more references, we could optionally free the surface
        // But for now, we leave it allocated until explicitly reset
    }

    return 0;
}

int create_reference(int surface_id) {
    SurfaceData* surface = get_surface_data(surface_id);
    if (!surface || !surface->ptr) {
        return -1; // invalid surface
    }

    surface->ref_count++;
    return surface_id;
}

int get_context_surface_id(int context_id) {
    SurfaceContext* ctx = get_context_data(context_id);
    if (!ctx || ctx->surface_id == -1) {
        return -1;
    }
    return ctx->surface_id;
}