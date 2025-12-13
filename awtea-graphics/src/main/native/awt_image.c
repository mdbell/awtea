#include "awt_image.h"

ImageView g_images[MAX_IMAGES];

ImageView* get_image_data(int id) {
    if (id < START_IMAGE_ID || id >= END_IMAGE_ID) return NULL;
    return &g_images[id];
}

int find_free_image_slot() {
    for (int i = 0; i < MAX_IMAGES; i++) {
        if (g_images[START_IMAGE_ID - i].ptr == 0) {
            return i + START_IMAGE_ID;
        }
    }
    return -1; // no free slot
}

int register_image(int format, int width, int height, int stride) {

    int id = find_free_image_slot();
    if (id == -1) {
        return -1; // no free slot
    }

    uint32_t ptr = alloc_pixels(width, height);
    if (ptr == 0) {
        return -2; // allocation failed
    }

    g_images[id].ptr    = ptr;
    g_images[id].format = format;
    g_images[id].width  = width;
    g_images[id].height = height;
    g_images[id].stride = stride;

    return id;
}

uint32_t get_image_pixels_ptr(int id) {
    if (id < START_IMAGE_ID || id >= END_IMAGE_ID) {
        return 0; // invalid id
    }
    ImageView* img = &g_images[id - START_IMAGE_ID];
    return (uint32_t)(uintptr_t)img->ptr;
}

int free_image(int id) {
    if (id < START_IMAGE_ID || id >= END_IMAGE_ID) {
        return -1; // invalid id
    }
    ImageView* img = &g_images[id - START_IMAGE_ID];
    if (img->ptr) {
        free_pixels((uint32_t)(uintptr_t)img->ptr);
        img->ptr = 0;
    }
    return 0;
}

uint32_t alloc_pixels(int width, int height) {
    // we allocate an extra stride bytes to avoid doing 0 allocs when width or height is 0
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
