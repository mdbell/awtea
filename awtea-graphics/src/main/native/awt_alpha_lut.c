#include "awt_alpha_lut.h"
#include "awt_log.h"

// Global 256×256 lookup table (64KB)
// Uninitialized to avoid bloating binary size - populated at runtime
uint8_t g_alpha_blend_lut[256][256];

// Initialize alpha blend lookup table
// This function computes all 65,536 possible results of (alpha * component) / 255
// Must be called once during module initialization
void init_alpha_blend_lut(void) {
    log_info("Initializing alpha blend lookup table (64KB)");
    
    for (int alpha = 0; alpha < 256; alpha++) {
        for (int component = 0; component < 256; component++) {
            // Compute: (alpha * component + 127) / 255
            // The +127 provides round-to-nearest behavior:
            // - If fractional part < 0.5: rounds down
            // - If fractional part >= 0.5: rounds up
            //
            // Example: 128 * 200 = 25600
            //          25600 + 127 = 25727
            //          25727 / 255 = 100.89... → 100
            //
            // This matches the behavior of: (uint8_t)((float)alpha * component / 255.0f + 0.5f)
            int result = (alpha * component + 127) / 255;
            
            // Store in lookup table
            g_alpha_blend_lut[alpha][component] = (uint8_t)result;
        }
    }
    
    log_info("Alpha blend LUT initialized successfully");
}

// Verification function for testing
// Returns 0 if LUT matches expected floating-point calculations
// Returns number of mismatches otherwise
#ifdef AWTEA_DEBUG_BUILD
int verify_alpha_blend_lut(void) {
    int mismatches = 0;
    
    for (int alpha = 0; alpha < 256; alpha++) {
        for (int component = 0; component < 256; component++) {
            // Expected result using floating-point
            float expected_f = ((float)alpha * (float)component) / 255.0f;
            uint8_t expected = (uint8_t)(expected_f + 0.5f);
            
            // Actual result from LUT
            uint8_t actual = g_alpha_blend_lut[alpha][component];
            
            // Check for mismatch
            if (expected != actual) {
                if (mismatches < 10) {
                    log_error("LUT mismatch at [%d][%d]: expected=%d, actual=%d",
                             alpha, component, expected, actual);
                }
                mismatches++;
            }
        }
    }
    
    if (mismatches == 0) {
        log_info("Alpha blend LUT verification: PASS (all 65536 entries match)");
    } else {
        log_error("Alpha blend LUT verification: FAIL (%d mismatches)", mismatches);
    }
    
    return mismatches;
}
#endif
