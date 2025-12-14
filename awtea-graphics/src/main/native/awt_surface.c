#include "awt_surface.h"
#include "awt_util.h"

Surface g_surfaces[NUM_SURFACES];

Surface* get_surface_data(int id) {
    if (id < START_SURFACE_ID || id >= END_SURFACE_ID) return NULL;
    return &g_surfaces[id - START_SURFACE_ID];
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

    Surface* surface = get_surface_data(surface_id);

    if(!surface) {
        return -2;
    }

    if(surface->ptr) {
        free((void*)(uintptr_t)surface->ptr);
    }

    memset(surface, 0, sizeof(Surface));

    if(width == 0 || height == 0 || layer < 0) {
        return 0; // zero-sized surface (freeing the surface)
    }

    surface->layer = (uint32_t)layer;
    surface->width = width;
    surface->height = height;
    surface->stride = width * sizeof(uint32_t);
    surface->format = format;

    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = malloc(bytes);
    if (!p) {
        surface->ptr = 0;
        surface->width = 0;
        surface->height = 0;
        return -1;
    }
    surface->ptr = (uint32_t)(uintptr_t)p;

    surface->argb[COLOR_FG] = DEFAULT_FG_COLOR; // default to opaque black
    surface->argb[COLOR_BG] = DEFAULT_BG_COLOR; // default

    // identity transform
    surface->transform.m00 = 1.0f;
    surface->transform.m01 = 0.0f;
    surface->transform.m02 = 0.0f;
    surface->transform.m10 = 0.0f;
    surface->transform.m11 = 1.0f;
    surface->transform.m12 = 0.0f;

    // clip_rect to full surface
    surface->clip.x = 0;
    surface->clip.y = 0;
    surface->clip.width = width;
    surface->clip.height = height;

    return 0;
}

uint32_t get_surface_pixels_ptr(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->ptr;
}

int get_surface_width(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->width;
}

int get_surface_height(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->height;
}

int get_surface_stride(int surface_id) {
    Surface* surface = get_surface_data(surface_id);
    if (!surface) {
        return 0;
    }
    return surface->stride;
}

int clip_x(int x, const Surface* surf) {
    x = clamp_int(x, 0, surf->width);
    if (surf->clip.width > 0) {
        x = clamp_int(x, surf->clip.x, surf->clip.x + surf->clip.width);
    }
    return x;
}

int clip_y(int y, const Surface* surf) {
    y = clamp_int(y, 0, surf->height);
    if (surf->clip.height > 0) {
        y = clamp_int(y, surf->clip.y, surf->clip.y + surf->clip.height);
    }
    return y;
}

// Push current surface state onto the state stack
int push_state(Surface* surface) {
    if (!surface) {
        return -1;
    }
    
    if (surface->stateStack.depth >= MAX_STATE_STACK_DEPTH) {
        return -2; // stack overflow
    }
    
    SurfaceState* state = &surface->stateStack.stack[surface->stateStack.depth];
    
    // Save current state
    state->argb[COLOR_FG] = surface->argb[COLOR_FG];
    state->argb[COLOR_BG] = surface->argb[COLOR_BG];
    state->transform = surface->transform;
    state->clip = surface->clip;
    
    surface->stateStack.depth++;
    return 0;
}

// Pop surface state from the state stack
int pop_state(Surface* surface) {
    if (!surface) {
        return -1;
    }
    
    if (surface->stateStack.depth <= 0) {
        return -2; // stack underflow
    }
    
    surface->stateStack.depth--;
    SurfaceState* state = &surface->stateStack.stack[surface->stateStack.depth];
    
    // Restore state
    surface->argb[COLOR_FG] = state->argb[COLOR_FG];
    surface->argb[COLOR_BG] = state->argb[COLOR_BG];
    surface->transform = state->transform;
    surface->clip = state->clip;
    
    return 0;
}