#include "awt_transform.h"

int is_identity_transform(const Transform2D* t) {
    return  t->m00 == 1.0f && t->m11 == 1.0f &&
            t->m01 == 0.0f && t->m10 == 0.0f &&
            t->m02 == 0.0f && t->m12 == 0.0f;
}

int invert_transform(const Transform2D* t, Transform2D* out) {
    float det = t->m00 * t->m11 - t->m01 * t->m10;
    if (det == 0.0f) {
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
    *outX = t->m00 * x + t->m01 * y + t->m02;
    *outY = t->m10 * x + t->m11 * y + t->m12;
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
}