#pragma once
#include <stdlib.h>

#include "awt_raster_internal.h"


int is_identity_transform(const Transform2D* t);
int invert_transform(const Transform2D* t, Transform2D* out);

void transform_point(const Transform2D* t,
                                   float x, float y,
                                   float* outX, float* outY);

void transform_rect( const Transform2D* t, int x, int y, int w, int h, int* outX, int* outY, int* outW, int* outH);