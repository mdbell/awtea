#include <math.h>
#include "awt_transform.h"
#include "awt_fixed.h"

#define EPS 1e-9

// Compile-time flag to enable fixed-point transforms for performance
// Set to 1 to use 16.16 fixed-point arithmetic (default)
// Set to 0 to use floating-point for compatibility testing
#ifndef USE_FIXED_POINT_TRANSFORMS
#define USE_FIXED_POINT_TRANSFORMS 1
#endif

int is_identity_transform(const Transform2D* t) {
    return  t->m00 == 1.0f && t->m11 == 1.0f &&
            t->m01 == 0.0f && t->m10 == 0.0f &&
            t->m02 == 0.0f && t->m12 == 0.0f;
}

int invert_transform(const Transform2D* t, Transform2D* out) {
    float det = t->m00 * t->m11 - t->m01 * t->m10;
    if (fabs(det) < EPS) {
        return 0; // non-invertible
    }
    float invDet = 1.0f / det;

    out->m00 =  t->m11 * invDet;
    out->m01 = -t->m01 * invDet;
    out->m10 = -t->m10 * invDet;
    out->m11 =  t->m00 * invDet;

    // translation part: -R⁻¹ * t
    out->m02 = -(out->m00 * t->m02 + out->m01 * t->m12);
    out->m12 = -(out->m10 * t->m02 + out->m11 * t->m12);

    return 1;
}

void transform_point(const Transform2D* t,
                                   float x, float y,
                                   float* outX, float* outY) {
#if USE_FIXED_POINT_TRANSFORMS
    // Use 16.16 fixed-point arithmetic for ~1.5-2x speedup
    // Convert transform matrix to fixed-point
    fx16_t m00_fx = FLOAT_TO_FIXED(t->m00);
    fx16_t m01_fx = FLOAT_TO_FIXED(t->m01);
    fx16_t m02_fx = FLOAT_TO_FIXED(t->m02);
    fx16_t m10_fx = FLOAT_TO_FIXED(t->m10);
    fx16_t m11_fx = FLOAT_TO_FIXED(t->m11);
    fx16_t m12_fx = FLOAT_TO_FIXED(t->m12);
    
    // Convert input coordinates to fixed-point
    fx16_t x_fx = FLOAT_TO_FIXED(x);
    fx16_t y_fx = FLOAT_TO_FIXED(y);
    
    // Perform matrix multiplication in fixed-point
    // outX = m00 * x + m01 * y + m02
    // outY = m10 * x + m11 * y + m12
    fx16_t outX_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m00_fx, x_fx),
                                          FIXED_MUL(m01_fx, y_fx)), m02_fx);
    fx16_t outY_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m10_fx, x_fx),
                                          FIXED_MUL(m11_fx, y_fx)), m12_fx);
    
    // Convert back to floating-point for output
    *outX = FIXED_TO_FLOAT(outX_fx);
    *outY = FIXED_TO_FLOAT(outY_fx);
#else
    // Original floating-point implementation
    *outX = t->m00 * x + t->m01 * y + t->m02;
    *outY = t->m10 * x + t->m11 * y + t->m12;
#endif
}


void transform_rect(
        const Transform2D* t,
        int x, int y, int w, int h,
        int* outX, int* outY,
        int* outW, int* outH) {

    if (is_identity_transform(t)) {
        *outX = x;
        *outY = y;
        *outW = w;
        *outH = h;
        return;
    }

#if USE_FIXED_POINT_TRANSFORMS
    // Use fixed-point for corner transformations
    fx16_t m00_fx = FLOAT_TO_FIXED(t->m00);
    fx16_t m01_fx = FLOAT_TO_FIXED(t->m01);
    fx16_t m02_fx = FLOAT_TO_FIXED(t->m02);
    fx16_t m10_fx = FLOAT_TO_FIXED(t->m10);
    fx16_t m11_fx = FLOAT_TO_FIXED(t->m11);
    fx16_t m12_fx = FLOAT_TO_FIXED(t->m12);
    
    fx16_t x0_fx = INT_TO_FIXED(x);
    fx16_t y0_fx = INT_TO_FIXED(y);
    fx16_t x1_fx = INT_TO_FIXED(x + w);
    fx16_t y1_fx = INT_TO_FIXED(y + h);

    // Transform four corners using fixed-point
    fx16_t tx0_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m00_fx, x0_fx),
                                         FIXED_MUL(m01_fx, y0_fx)), m02_fx);
    fx16_t ty0_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m10_fx, x0_fx),
                                         FIXED_MUL(m11_fx, y0_fx)), m12_fx);

    fx16_t tx1_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m00_fx, x1_fx),
                                         FIXED_MUL(m01_fx, y0_fx)), m02_fx);
    fx16_t ty1_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m10_fx, x1_fx),
                                         FIXED_MUL(m11_fx, y0_fx)), m12_fx);

    fx16_t tx2_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m00_fx, x1_fx),
                                         FIXED_MUL(m01_fx, y1_fx)), m02_fx);
    fx16_t ty2_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m10_fx, x1_fx),
                                         FIXED_MUL(m11_fx, y1_fx)), m12_fx);

    fx16_t tx3_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m00_fx, x0_fx),
                                         FIXED_MUL(m01_fx, y1_fx)), m02_fx);
    fx16_t ty3_fx = FIXED_ADD(FIXED_ADD(FIXED_MUL(m10_fx, x0_fx),
                                         FIXED_MUL(m11_fx, y1_fx)), m12_fx);

    // Find min/max using fixed-point comparisons
    fx16_t minX_fx = FIXED_MIN(FIXED_MIN(tx0_fx, tx1_fx), FIXED_MIN(tx2_fx, tx3_fx));
    fx16_t minY_fx = FIXED_MIN(FIXED_MIN(ty0_fx, ty1_fx), FIXED_MIN(ty2_fx, ty3_fx));
    fx16_t maxX_fx = FIXED_MAX(FIXED_MAX(tx0_fx, tx1_fx), FIXED_MAX(tx2_fx, tx3_fx));
    fx16_t maxY_fx = FIXED_MAX(FIXED_MAX(ty0_fx, ty1_fx), FIXED_MAX(ty2_fx, ty3_fx));

    // Convert to integers with proper rounding
    int ix = FIXED_TO_INT_FLOOR(minX_fx);
    int iy = FIXED_TO_INT_FLOOR(minY_fx);
    int iw = FIXED_TO_INT_CEIL(maxX_fx) - ix;
    int ih = FIXED_TO_INT_CEIL(maxY_fx) - iy;

    *outX = ix;
    *outY = iy;
    *outW = iw;
    *outH = ih;
#else
    // Original floating-point implementation
    float x0 = (float)x;
    float y0 = (float)y;
    float x1 = (float)(x + w);
    float y1 = (float)(y + h);

    // transform four corners
    float tx0 = t->m00 * x0 + t->m01 * y0 + t->m02;
    float ty0 = t->m10 * x0 + t->m11 * y0 + t->m12;

    float tx1 = t->m00 * x1 + t->m01 * y0 + t->m02;
    float ty1 = t->m10 * x1 + t->m11 * y0 + t->m12;

    float tx2 = t->m00 * x1 + t->m01 * y1 + t->m02;
    float ty2 = t->m10 * x1 + t->m11 * y1 + t->m12;

    float tx3 = t->m00 * x0 + t->m01 * y1 + t->m02;
    float ty3 = t->m10 * x0 + t->m11 * y1 + t->m12;

    float minX = fminf(fminf(tx0, tx1), fminf(tx2, tx3));
    float minY = fminf(fminf(ty0, ty1), fminf(ty2, ty3));
    float maxX = fmaxf(fmaxf(tx0, tx1), fmaxf(tx2, tx3));
    float maxY = fmaxf(fmaxf(ty0, ty1), fmaxf(ty2, ty3));

    int ix = (int)floorf(minX);
    int iy = (int)floorf(minY);
    int iw = (int)ceilf(maxX) - ix;
    int ih = (int)ceilf(maxY) - iy;

    *outX = ix;
    *outY = iy;
    *outW = iw;
    *outH = ih;
#endif
}