#include "awt_init.h"
#include "awt_stack.h"
#include "awt_alpha_lut.h"
#include "awt_surface.h"
#include "awt_log.h"

// Auto-initialize stack tracking subsystem
// Priority 101: Must run first as other subsystems may use stack tracing
__attribute__((constructor(101)))
static void auto_init_stack_tracking(void) {
    init_stack_tracking();
}

// Auto-initialize alpha blend lookup table
// Priority 102: No dependencies on stack tracking, but should run before surface system
__attribute__((constructor(102)))
static void auto_init_alpha_lut(void) {
    init_alpha_blend_lut();
}

// Auto-initialize surface system
// Priority 103: Depends on stack tracking and alpha LUT being initialized
__attribute__((constructor(103)))
static void auto_init_surface_system(void) {
    STACK_ENTER();
    
    // Initialize all contexts to mark them as free
    for (int i = 0; i < NUM_CONTEXTS; i++) {
        g_contexts[i].surface_id = -1;
    }
    
    log_info("Initialized surface system: %d surfaces, %d contexts", 
             NUM_SURFACES, NUM_CONTEXTS);
    
    STACK_EXIT();
}
