#pragma once

static inline int clamp_int(int v, int lo, int hi) {
    if (v < lo){
        return lo;
    }
    if (v > hi) {
        return hi;
    }
    return v;
}

static inline float u32_to_float(uint32_t v) {
    union { uint32_t u; float f; } u;
    u.u = v;
    return u.f;
}