#include "awt_image.h"

ImageView g_images[MAX_IMAGES];

ImageView* get_image_data(int id) {
    if (id < START_IMAGE_ID || id >= END_IMAGE_ID) return NULL;
    return &g_images[id];
}

void register_image(int id, uint32_t ptr, int format, int width, int height, int stride) {
    if (id < 0 || id >= MAX_IMAGES) return;
    g_images[id].ptr    = ptr;
    g_images[id].format = format;
    g_images[id].width  = width;
    g_images[id].height = height;
    g_images[id].stride = stride;
}

uint32_t alloc_pixels(int width, int height) {
    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = malloc(bytes);
    if (!p){
        return 0;
    }
    memset(p, 0, bytes);
    return (uint32_t)(uintptr_t)p;
}

void free_pixels(uint32_t ptr) {
    free((void*)(uintptr_t)ptr);
}