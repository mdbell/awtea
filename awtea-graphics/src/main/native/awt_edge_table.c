#include "awt_edge_table.h"
#include "awt_pixel.h"
#include "awt_log.h"
#include "awt_stack.h"
#include "awt_util.h"
#include "awt_memory.h"
#include <stdlib.h>
#include <string.h>
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Helper function to create an edge list
static EdgeList* edge_list_create(int initial_capacity) {
    EdgeList* list = (EdgeList*)malloc(sizeof(EdgeList));
    if (!list) {
        return NULL;
    }
    list->edges = (EdgeNode*)malloc(sizeof(EdgeNode) * initial_capacity);
    if (!list->edges) {
        free(list);
        return NULL;
    }
    list->count = 0;
    list->capacity = initial_capacity;
    return list;
}

// Helper function to destroy an edge list
static void edge_list_destroy(EdgeList* list) {
    if (list) {
        if (list->edges) {
            free(list->edges);
        }
        free(list);
    }
}

// Helper function to add edge to a list (with growth)
static void edge_list_add(EdgeList* list, EdgeNode edge) {
    if (list->count >= list->capacity) {
        // Double the capacity
        int new_capacity = list->capacity * 2;
        EdgeNode* new_edges = (EdgeNode*)tracked_realloc(list->edges, sizeof(EdgeNode) * new_capacity);
        if (!new_edges) {
            log_error("Failed to grow edge list");
            return;
        }
        list->edges = new_edges;
        list->capacity = new_capacity;
    }
    list->edges[list->count++] = edge;
}

// Helper function to clear an edge list
static void edge_list_clear(EdgeList* list) {
    list->count = 0;
}

// Helper function to sort edges by x coordinate (insertion sort)
static void edge_list_sort_by_x(EdgeList* list) {
    for (int i = 1; i < list->count; i++) {
        EdgeNode key = list->edges[i];
        int j = i - 1;
        while (j >= 0 && list->edges[j].x > key.x) {
            list->edges[j + 1] = list->edges[j];
            j--;
        }
        list->edges[j + 1] = key;
    }
}

// Helper function to remove edges that have reached their max y
static void edge_list_remove_inactive(EdgeList* list, int y) {
    int write_idx = 0;
    for (int read_idx = 0; read_idx < list->count; read_idx++) {
        if (list->edges[read_idx].y_max > y) {
            list->edges[write_idx++] = list->edges[read_idx];
        }
    }
    list->count = write_idx;
}

// Create an edge table
EdgeTable* edge_table_create(int min_y, int max_y, int width, int height) {
    STACK_ENTER();
    
    EdgeTable* et = (EdgeTable*)tracked_malloc(sizeof(EdgeTable));
    if (!et) {
        log_error("Failed to allocate edge table");
        STACK_EXIT();
        return NULL;
    }
    
    et->min_y = min_y;
    et->max_y = max_y;
    et->width = width;
    et->height = height;
    
    int num_scanlines = max_y - min_y + 1;
    if (num_scanlines <= 0) {
        log_error("Invalid scanline range: min_y=%d, max_y=%d", min_y, max_y);
        free(et);
        STACK_EXIT();
        return NULL;
    }
    
    et->scanlines = (EdgeList*)tracked_malloc(sizeof(EdgeList) * num_scanlines);
    if (!et->scanlines) {
        log_error("Failed to allocate scanline array");
        free(et);
        STACK_EXIT();
        return NULL;
    }
    
    // Initialize each scanline's edge list
    for (int i = 0; i < num_scanlines; i++) {
        et->scanlines[i].edges = NULL;
        et->scanlines[i].count = 0;
        et->scanlines[i].capacity = 0;
    }
    
    // Initialize active edge list
    et->active.edges = (EdgeNode*)tracked_malloc(sizeof(EdgeNode) * 16);
    if (!et->active.edges) {
        log_error("Failed to allocate active edge list");
        free(et->scanlines);
        free(et);
        STACK_EXIT();
        return NULL;
    }
    et->active.count = 0;
    et->active.capacity = 16;
    
    log_debug("Created edge table: min_y=%d, max_y=%d, width=%d, height=%d", 
             min_y, max_y, width, height);
    
    STACK_EXIT();
    return et;
}

// Destroy an edge table
void edge_table_destroy(EdgeTable* et) {
    if (!et) {
        return;
    }
    
    if (et->scanlines) {
        int num_scanlines = et->max_y - et->min_y + 1;
        for (int i = 0; i < num_scanlines; i++) {
            if (et->scanlines[i].edges) {
                free(et->scanlines[i].edges);
            }
        }
        free(et->scanlines);
    }
    
    if (et->active.edges) {
        free(et->active.edges);
    }
    
    free(et);
}

// Add a single edge to the edge table
void edge_table_add_edge(EdgeTable* et, int y1, float x1, int y2, float x2) {
    // Skip horizontal edges
    if (y1 == y2) {
        return;
    }
    
    // Ensure y1 < y2 (swap if needed)
    if (y1 > y2) {
        int temp_y = y1;
        y1 = y2;
        y2 = temp_y;
        
        float temp_x = x1;
        x1 = x2;
        x2 = temp_x;
    }
    
    // Clip to surface bounds
    if (y2 < 0 || y1 >= et->height) {
        return;
    }
    
    // Clip y coordinates
    if (y1 < 0) {
        // Interpolate x at y=0
        float t = (0.0f - y1) / (float)(y2 - y1);
        x1 = x1 + t * (x2 - x1);
        y1 = 0;
    }
    if (y2 >= et->height) {
        // Interpolate x at y=height-1
        float t = (float)(et->height - 1 - y1) / (float)(y2 - y1);
        x2 = x1 + t * (x2 - x1);
        y2 = et->height - 1;
    }
    
    // Calculate dx (change in x per scanline)
    float dx = (x2 - x1) / (float)(y2 - y1);
    
    // Determine edge direction for non-zero winding rule
    int direction = (y2 > y1) ? 1 : -1;
    
    // Create edge node
    EdgeNode edge;
    edge.y_max = y2;
    edge.x = x1;
    edge.dx = dx;
    edge.direction = direction;
    
    // Add edge to appropriate scanline
    int scanline_idx = y1 - et->min_y;
    if (scanline_idx >= 0 && scanline_idx < (et->max_y - et->min_y + 1)) {
        EdgeList* list = &et->scanlines[scanline_idx];
        
        // Allocate edge array if needed
        if (list->edges == NULL) {
            list->edges = (EdgeNode*)tracked_malloc(sizeof(EdgeNode) * 4);
            if (!list->edges) {
                log_error("Failed to allocate edge list for scanline %d", y1);
                return;
            }
            list->capacity = 4;
            list->count = 0;
        }
        
        edge_list_add(list, edge);
    }
}

// Add a line segment as edges
void edge_table_add_line(EdgeTable* et, int x1, int y1, int x2, int y2) {
    edge_table_add_edge(et, y1, (float)x1, y2, (float)x2);
}

// Add an arc (tessellated into line segments)
void edge_table_add_arc(EdgeTable* et, int cx, int cy, int rx, int ry, 
                       double start_angle, double end_angle) {
    STACK_ENTER();
    
    // Normalize angles to [0, 2*PI)
    while (start_angle < 0) start_angle += 2.0 * M_PI;
    while (end_angle < 0) end_angle += 2.0 * M_PI;
    while (start_angle >= 2.0 * M_PI) start_angle -= 2.0 * M_PI;
    while (end_angle >= 2.0 * M_PI) end_angle -= 2.0 * M_PI;
    
    // Handle wrap-around
    if (end_angle <= start_angle) {
        end_angle += 2.0 * M_PI;
    }
    
    // Calculate number of segments based on arc length and radius
    double arc_length = fabs(end_angle - start_angle) * (rx + ry) / 2.0;
    int num_segments = (int)(arc_length / 4.0) + 4; // At least 4 segments
    if (num_segments > 360) num_segments = 360; // Cap at 360 segments
    
    double angle_step = (end_angle - start_angle) / num_segments;
    
    // Generate points along the arc
    int prev_x = cx + (int)(rx * cos(start_angle));
    int prev_y = cy + (int)(ry * sin(start_angle));
    
    for (int i = 1; i <= num_segments; i++) {
        double angle = start_angle + i * angle_step;
        int curr_x = cx + (int)(rx * cos(angle));
        int curr_y = cy + (int)(ry * sin(angle));
        
        edge_table_add_line(et, prev_x, prev_y, curr_x, curr_y);
        
        prev_x = curr_x;
        prev_y = curr_y;
    }
    
    STACK_EXIT();
}

// Add a quadratic Bezier curve (tessellated into line segments)
void edge_table_add_bezier(EdgeTable* et, int x1, int y1, int cx, int cy, 
                          int x2, int y2) {
    STACK_ENTER();
    
    // Use 20 segments for quadratic Bezier
    const int num_segments = 20;
    const float step = 1.0f / num_segments;
    
    int prev_x = x1;
    int prev_y = y1;
    
    for (int i = 1; i <= num_segments; i++) {
        float t = i * step;
        float t_inv = 1.0f - t;
        
        // Quadratic Bezier formula: B(t) = (1-t)^2 * P0 + 2*(1-t)*t * P1 + t^2 * P2
        int curr_x = (int)(t_inv * t_inv * x1 + 2.0f * t_inv * t * cx + t * t * x2);
        int curr_y = (int)(t_inv * t_inv * y1 + 2.0f * t_inv * t * cy + t * t * y2);
        
        edge_table_add_line(et, prev_x, prev_y, curr_x, curr_y);
        
        prev_x = curr_x;
        prev_y = curr_y;
    }
    
    STACK_EXIT();
}

// Fill using scanline algorithm
void edge_table_fill(EdgeTable* et, SurfaceData* surface, SurfaceContext* context,
                    uint32_t color, int fill_rule) {
    STACK_ENTER();
    
    if (!et || !surface) {
        log_error("edge_table_fill: null edge table or surface");
        STACK_EXIT();
        return;
    }
    
    log_debug("edge_table_fill: filling with color=0x%08X, fill_rule=%d", color, fill_rule);
    
    const PixelFormatInfo* dstInfo = &g_pixel_format_info[surface->format];
    int dstHasAlpha = (dstInfo->mask_a != 0);
    
    // Process each scanline
    for (int y = et->min_y; y <= et->max_y; y++) {
        // Skip scanlines outside surface bounds
        if (y < 0 || y >= (int)surface->height) {
            continue;
        }
        
        int scanline_idx = y - et->min_y;
        
        // Add new edges from this scanline to active list
        EdgeList* scanline = &et->scanlines[scanline_idx];
        for (int i = 0; i < scanline->count; i++) {
            edge_list_add(&et->active, scanline->edges[i]);
        }
        
        // Remove edges that have reached their max y
        edge_list_remove_inactive(&et->active, y);
        
        // Sort active edges by x coordinate
        edge_list_sort_by_x(&et->active);
        
        // Fill pixels between edge pairs
        if (fill_rule == FILL_RULE_EVENODD) {
            // Even-odd fill rule: toggle on/off at each edge crossing
            for (int i = 0; i + 1 < et->active.count; i += 2) {
                EdgeNode* e1 = &et->active.edges[i];
                EdgeNode* e2 = &et->active.edges[i + 1];
                
                int x_start = (int)(e1->x + 0.5f);
                int x_end = (int)(e2->x + 0.5f);
                
                // Clip to surface bounds and context clip
                x_start = clamp_int(x_start, 0, (int)surface->width - 1);
                x_end = clamp_int(x_end, 0, (int)surface->width - 1);
                
                if (context->clip.width > 0 && context->clip.height > 0) {
                    x_start = clamp_int(x_start, context->clip.x, 
                                       context->clip.x + context->clip.width - 1);
                    x_end = clamp_int(x_end, context->clip.x, 
                                     context->clip.x + context->clip.width - 1);
                }
                
                // Fill pixels in this span
                for (int x = x_start; x <= x_end; x++) {
                    if (!dstHasAlpha) {
                        set_pixel_generic(surface, x, y, PIXEL_FORMAT_ARGB, color);
                    } else {
                        blend_pixel_composite(surface, x, y, PIXEL_FORMAT_ARGB, color,
                                            context->composite_mode, context->composite_alpha);
                    }
                }
            }
        } else {
            // Non-zero winding fill rule: count crossings with direction
            int winding = 0;
            int span_start = -1;
            
            for (int i = 0; i < et->active.count; i++) {
                EdgeNode* e = &et->active.edges[i];
                int x = (int)(e->x + 0.5f);
                
                // Update winding number
                int prev_winding = winding;
                winding += e->direction;
                
                // Check if we're entering or leaving a filled region
                if (prev_winding == 0 && winding != 0) {
                    // Entering filled region
                    span_start = x;
                } else if (prev_winding != 0 && winding == 0) {
                    // Leaving filled region
                    if (span_start >= 0) {
                        int x_start = span_start;
                        int x_end = x;
                        
                        // Clip to surface bounds and context clip
                        x_start = clamp_int(x_start, 0, (int)surface->width - 1);
                        x_end = clamp_int(x_end, 0, (int)surface->width - 1);
                        
                        if (context->clip.width > 0 && context->clip.height > 0) {
                            x_start = clamp_int(x_start, context->clip.x, 
                                               context->clip.x + context->clip.width - 1);
                            x_end = clamp_int(x_end, context->clip.x, 
                                             context->clip.x + context->clip.width - 1);
                        }
                        
                        // Fill pixels in this span
                        for (int px = x_start; px <= x_end; px++) {
                            if (!dstHasAlpha) {
                                set_pixel_generic(surface, px, y, PIXEL_FORMAT_ARGB, color);
                            } else {
                                blend_pixel_composite(surface, px, y, PIXEL_FORMAT_ARGB, color,
                                                    context->composite_mode, context->composite_alpha);
                            }
                        }
                        
                        span_start = -1;
                    }
                }
            }
        }
        
        // Update x coordinates for next scanline
        for (int i = 0; i < et->active.count; i++) {
            et->active.edges[i].x += et->active.edges[i].dx;
        }
    }
    
    log_debug("edge_table_fill: completed");
    STACK_EXIT();
}

// Create an edge table pool
EdgeTablePool* edge_table_pool_create(int initial_capacity) {
    STACK_ENTER();
    
    if (initial_capacity <= 0) {
        initial_capacity = 4; // Default capacity
    }
    
    EdgeTablePool* pool = (EdgeTablePool*)malloc(sizeof(EdgeTablePool));
    if (!pool) {
        log_error("Failed to allocate edge table pool");
        STACK_EXIT();
        return NULL;
    }
    
    pool->tables = (EdgeTable**)malloc(sizeof(EdgeTable*) * initial_capacity);
    pool->in_use = (int*)malloc(sizeof(int) * initial_capacity);
    
    if (!pool->tables || !pool->in_use) {
        log_error("Failed to allocate pool arrays");
        if (pool->tables) free(pool->tables);
        if (pool->in_use) free(pool->in_use);
        free(pool);
        STACK_EXIT();
        return NULL;
    }
    
    pool->capacity = initial_capacity;
    pool->count = 0;
    
    // Initialize all tables as not in use
    for (int i = 0; i < initial_capacity; i++) {
        pool->tables[i] = NULL;
        pool->in_use[i] = 0;
    }
    
    log_debug("Created edge table pool with capacity=%d", initial_capacity);
    
    STACK_EXIT();
    return pool;
}

// Acquire an edge table from the pool
EdgeTable* edge_table_pool_acquire(EdgeTablePool* pool, int min_y, int max_y, 
                                   int width, int height) {
    STACK_ENTER();
    
    if (!pool) {
        log_error("edge_table_pool_acquire: null pool");
        STACK_EXIT();
        return edge_table_create(min_y, max_y, width, height);
    }
    
    int required_scanlines = max_y - min_y + 1;
    
    // Try to find an unused edge table with matching dimensions
    for (int i = 0; i < pool->count; i++) {
        if (!pool->in_use[i] && pool->tables[i]) {
            EdgeTable* et = pool->tables[i];
            int existing_scanlines = et->max_y - et->min_y + 1;
            
            // Only reuse if dimensions match to avoid out-of-bounds access
            if (existing_scanlines == required_scanlines && 
                et->width == width && et->height == height) {
                
                // Reset the edge table with new bounds
                et->min_y = min_y;
                et->max_y = max_y;
                
                // Clear all scanlines
                for (int j = 0; j < existing_scanlines; j++) {
                    edge_list_clear(&et->scanlines[j]);
                }
                edge_list_clear(&et->active);
                
                pool->in_use[i] = 1;
                
                log_debug("Reused edge table from pool (index=%d)", i);
                STACK_EXIT();
                return et;
            }
        }
    }
    
    // No matching table found, create a new one
    EdgeTable* et = edge_table_create(min_y, max_y, width, height);
    
    if (et && pool->count < pool->capacity) {
        pool->tables[pool->count] = et;
        pool->in_use[pool->count] = 1;
        pool->count++;
        
        log_debug("Created new edge table in pool (count=%d)", pool->count);
    }
    
    STACK_EXIT();
    return et;
}

// Release an edge table back to the pool
void edge_table_pool_release(EdgeTablePool* pool, EdgeTable* et) {
    if (!pool || !et) {
        return;
    }
    
    // Find the edge table in the pool and mark it as not in use
    for (int i = 0; i < pool->count; i++) {
        if (pool->tables[i] == et) {
            pool->in_use[i] = 0;
            log_debug("Released edge table back to pool (index=%d)", i);
            return;
        }
    }
    
    // Edge table not found in pool, destroy it
    log_debug("Edge table not in pool, destroying it");
    edge_table_destroy(et);
}

// Destroy an edge table pool
void edge_table_pool_destroy(EdgeTablePool* pool) {
    if (!pool) {
        return;
    }
    
    // Destroy all edge tables
    for (int i = 0; i < pool->count; i++) {
        if (pool->tables[i]) {
            edge_table_destroy(pool->tables[i]);
        }
    }
    
    free(pool->tables);
    free(pool->in_use);
    free(pool);
    
    log_debug("Destroyed edge table pool");
}
