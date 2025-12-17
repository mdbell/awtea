#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "awt_raster_internal.h"

// Fill rule constants
#define FILL_RULE_EVENODD 0
#define FILL_RULE_NONZERO 1

// Forward declarations
typedef struct EdgeNode EdgeNode;
typedef struct EdgeList EdgeList;
typedef struct EdgeTable EdgeTable;
typedef struct EdgeTablePool EdgeTablePool;

// Edge node structure
// Represents a single edge in the edge table
struct EdgeNode {
    int y_max;      // Maximum y coordinate (scanline where edge ends)
    float x;        // Current x coordinate (updated during scanline processing)
    float dx;       // Change in x per scanline (1/slope)
    int direction;  // Edge direction for non-zero winding: +1 or -1
};

// Dynamic edge list structure
// Contains an array of edges that can grow as needed
struct EdgeList {
    EdgeNode* edges;     // Dynamic array of edges
    int count;           // Current number of edges
    int capacity;        // Allocated capacity
};

// Edge table structure
// Main data structure for scanline polygon filling
struct EdgeTable {
    EdgeList* scanlines; // Array of edge lists, one per scanline
    EdgeList active;     // Active edge list for current scanline
    int min_y;           // Minimum y coordinate
    int max_y;           // Maximum y coordinate
    int width;           // Surface width (for clipping)
    int height;          // Surface height (for clipping)
};

// Edge table pool structure
// Reusable pool of edge tables to reduce allocations
struct EdgeTablePool {
    EdgeTable** tables;  // Array of edge table pointers
    int* in_use;         // Array of flags indicating if table is in use
    int capacity;        // Number of edge tables in pool
    int count;           // Number of tables currently allocated
};

// Edge table lifecycle functions
EdgeTable* edge_table_create(int min_y, int max_y, int width, int height);
void edge_table_destroy(EdgeTable* et);

// Edge manipulation functions
void edge_table_add_edge(EdgeTable* et, int y1, float x1, int y2, float x2);
void edge_table_add_line(EdgeTable* et, int x1, int y1, int x2, int y2);

// Shape tessellation functions
void edge_table_add_arc(EdgeTable* et, int cx, int cy, int rx, int ry, 
                       double start_angle, double end_angle);
void edge_table_add_bezier(EdgeTable* et, int x1, int y1, int cx, int cy, 
                          int x2, int y2);

// Scanline fill function
void edge_table_fill(EdgeTable* et, SurfaceData* surface, SurfaceContext* context,
                    uint32_t color, int fill_rule);

// Pool management functions
EdgeTablePool* edge_table_pool_create(int initial_capacity);
EdgeTable* edge_table_pool_acquire(EdgeTablePool* pool, int min_y, int max_y, 
                                   int width, int height);
void edge_table_pool_release(EdgeTablePool* pool, EdgeTable* et);
void edge_table_pool_destroy(EdgeTablePool* pool);
