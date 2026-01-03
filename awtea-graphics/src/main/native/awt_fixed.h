#pragma once
#include <stdint.h>

// Fixed-Point Math Library for High-Performance 2D Graphics Rasterization
// 
// Format: 16.16 Fixed-Point (Q16.16)
// - Sign bit: 1
// - Integer part: 15 bits
// - Fractional part: 16 bits
// - Range: -32768.0 to +32767.99998
// - Precision: 1/65536 ≈ 0.000015 (sufficient for screen coordinates up to 32K pixels)
//
// Performance Benefits:
// - Addition/Subtraction: 3-5x faster than float (single integer add/sub)
// - Comparison: 5x faster than float (direct integer comparison)
// - SIMD friendly: Can process 4x int32 in parallel vs 4x float
// - Deterministic: No floating-point rounding variations across platforms
//
// Usage in scanline rasterization hot paths:
// - Edge X coordinate updates: x += dx (1 cycle vs 3-5 for float)
// - Edge sorting by X: Integer comparison (1 cycle vs ~5 for float)
// - Slope calculations: One-time conversion cost, amortized over scanlines

// Fixed-point type: 16.16 format (signed 32-bit integer)
typedef int32_t fx16_t;

// Fixed-point constant: 1.0 in 16.16 format
// This is the scaling factor: 2^16 = 65536
#define FX16_ONE 65536
#define FX16_HALF 32768  // 0.5 in fixed-point

// Fixed-point constant: 0.0
#define FX16_ZERO 0

//-----------------------------------------------------------------------------
// Conversion Macros
//-----------------------------------------------------------------------------

// Convert integer to fixed-point
// Example: INT_TO_FIXED(10) = 655360 (represents 10.0)
#define INT_TO_FIXED(i) ((fx16_t)((i) << 16))

// Convert float to fixed-point
// Example: FLOAT_TO_FIXED(10.5f) = 688128 (represents 10.5)
#define FLOAT_TO_FIXED(f) ((fx16_t)((f) * 65536.0f))

// Convert double to fixed-point (for high-precision conversions)
#define DOUBLE_TO_FIXED(d) ((fx16_t)((d) * 65536.0))

// Convert fixed-point to integer (truncates fractional part)
// Example: FIXED_TO_INT(655360) = 10
#define FIXED_TO_INT(fx) ((int32_t)((fx) >> 16))

// Convert fixed-point to integer with rounding (nearest integer)
// Example: FIXED_TO_INT_ROUND(688128) = 11 (10.5 rounds to 11)
#define FIXED_TO_INT_ROUND(fx) ((int32_t)(((fx) + FX16_HALF) >> 16))

// Convert fixed-point to integer with floor (always rounds down)
#define FIXED_TO_INT_FLOOR(fx) FIXED_TO_INT(fx)

// Convert fixed-point to integer with ceiling (always rounds up)
#define FIXED_TO_INT_CEIL(fx) ((int32_t)(((fx) + FX16_ONE - 1) >> 16))

// Convert fixed-point to float
// Example: FIXED_TO_FLOAT(655360) = 10.0f
#define FIXED_TO_FLOAT(fx) ((float)(fx) / 65536.0f)

// Convert fixed-point to double
#define FIXED_TO_DOUBLE(fx) ((double)(fx) / 65536.0)

// Extract fractional part (0.0 to 0.99998)
// Returns fractional bits (0-65535)
#define FIXED_FRAC(fx) ((fx) & 0xFFFF)

// Extract integer part
// Same as FIXED_TO_INT but clearer for extracting whole number
#define FIXED_INT_PART(fx) FIXED_TO_INT(fx)

//-----------------------------------------------------------------------------
// Arithmetic Operations
//-----------------------------------------------------------------------------

// Addition: a + b
// Direct integer addition (1 CPU cycle on most architectures)
#define FIXED_ADD(a, b) ((a) + (b))

// Subtraction: a - b
// Direct integer subtraction (1 CPU cycle)
#define FIXED_SUB(a, b) ((a) - (b))

// Multiplication: a * b
// Uses 64-bit intermediate to prevent overflow
// Cost: ~3-4 cycles (mul + shift)
#define FIXED_MUL(a, b) ((fx16_t)(((int64_t)(a) * (int64_t)(b)) >> 16))

// Division: a / b
// Uses 64-bit intermediate for precision
// Cost: ~10-40 cycles (div is expensive, but still faster than float div)
// WARNING: Does not check for division by zero
#define FIXED_DIV(a, b) ((fx16_t)((((int64_t)(a)) << 16) / (b)))

// Multiply by integer (faster than FIXED_MUL for integer scalars)
// Example: FIXED_MUL_INT(FLOAT_TO_FIXED(1.5), 3) = FLOAT_TO_FIXED(4.5)
#define FIXED_MUL_INT(fx, i) ((fx) * (i))

// Divide by integer (faster than FIXED_DIV for integer divisors)
#define FIXED_DIV_INT(fx, i) ((fx) / (i))

// Negate: -a
#define FIXED_NEG(a) (-(a))

// Absolute value: |a|
#define FIXED_ABS(a) ((a) < 0 ? -(a) : (a))

//-----------------------------------------------------------------------------
// Comparison Operations
//-----------------------------------------------------------------------------

// All comparisons are direct integer comparisons (1 CPU cycle)
// Much faster than floating-point comparisons (~5 cycles)

#define FIXED_EQ(a, b) ((a) == (b))
#define FIXED_NE(a, b) ((a) != (b))
#define FIXED_LT(a, b) ((a) < (b))
#define FIXED_LE(a, b) ((a) <= (b))
#define FIXED_GT(a, b) ((a) > (b))
#define FIXED_GE(a, b) ((a) >= (b))

// Minimum and maximum
#define FIXED_MIN(a, b) ((a) < (b) ? (a) : (b))
#define FIXED_MAX(a, b) ((a) > (b) ? (a) : (b))

//-----------------------------------------------------------------------------
// Utility Functions
//-----------------------------------------------------------------------------

// Clamp fixed-point value to range [min, max]
#define FIXED_CLAMP(x, min, max) FIXED_MAX(min, FIXED_MIN(x, max))

// Linear interpolation: a + t * (b - a)
// where t is a fixed-point value in [0, 1]
#define FIXED_LERP(a, b, t) FIXED_ADD(a, FIXED_MUL(t, FIXED_SUB(b, a)))

// Square a fixed-point value: a * a
#define FIXED_SQUARE(a) FIXED_MUL(a, a)

//-----------------------------------------------------------------------------
// Special Values and Limits
//-----------------------------------------------------------------------------

// Maximum and minimum representable values
#define FX16_MAX 0x7FFFFFFF  // +32767.99998
#define FX16_MIN 0x80000000  // -32768.0

//-----------------------------------------------------------------------------
// Overflow Detection (optional, for debugging)
//-----------------------------------------------------------------------------

// Check if multiplication would overflow (returns 0 if safe, non-zero if overflow)
// This is expensive and should only be used in debug builds
#ifdef AWTEA_DEBUG_BUILD
#define FIXED_MUL_WOULD_OVERFLOW(a, b) \
    ((((int64_t)(a) * (int64_t)(b)) >> 16) > FX16_MAX || \
     (((int64_t)(a) * (int64_t)(b)) >> 16) < FX16_MIN)
#else
#define FIXED_MUL_WOULD_OVERFLOW(a, b) 0
#endif

//-----------------------------------------------------------------------------
// Performance Notes
//-----------------------------------------------------------------------------
//
// Typical speedups in scanline rasterization hot paths:
//
// 1. Edge X coordinate update (scanline loop):
//    Float:  x += dx  → 3-5 CPU cycles (FP add)
//    Fixed:  x += dx  → 1 CPU cycle (integer add)
//    Speedup: 3-5x
//
// 2. Edge sorting by X (per scanline):
//    Float:  if (e1.x > e2.x) → ~5 cycles (FP compare + branch)
//    Fixed:  if (e1.x > e2.x) → 1 cycle (int compare + branch)
//    Speedup: ~5x
//
// 3. Edge creation (per edge, amortized):
//    Slope calculation: dx = (x2-x1)/(y2-y1) → one-time float→fixed conversion
//    Cost is negligible compared to scanline loop iterations
//
// Overall expected speedup for filled polygon rendering: 2-3x
// Overall expected speedup for font glyph rasterization: 1.8-2x
//
//-----------------------------------------------------------------------------
