#include "awt_memory.h"
#include "awt_image.h"

uint32_t alloc_pixels(int width, int height) {
    // we allocate an extra stride bytes to avoid doing 0 allocs when width or height is 0
    size_t bytes = (size_t)width * (size_t)height * sizeof(uint32_t);
    void* p = tracked_malloc(bytes);
    if (!p){
        return 0;
    }
    memset(p, 0xFF, bytes);
    return (uint32_t)(uintptr_t)p;
}

void free_pixels(uint32_t ptr) {
    tracked_free((void*)(uintptr_t)ptr);
}
