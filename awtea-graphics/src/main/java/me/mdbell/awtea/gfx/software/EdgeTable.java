package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Edge table implementation for scanline polygon filling.
 * Used by the software rasterizer for efficient area-filling operations.
 */
public class EdgeTable {
    
    private static final Logger log = LoggerFactory.getLogger(EdgeTable.class);
    
    // Fill rule constants
    public static final int FILL_RULE_EVENODD = 0;
    public static final int FILL_RULE_NONZERO = 1;
    
    /**
     * Represents a single edge in the edge table
     */
    static class EdgeNode {
        int yMax;        // Maximum y coordinate (scanline where edge ends)
        float x;         // Current x coordinate (updated during scanline processing)
        float dx;        // Change in x per scanline (1/slope)
        int direction;   // Edge direction for non-zero winding: +1 or -1
        
        EdgeNode(int yMax, float x, float dx, int direction) {
            this.yMax = yMax;
            this.x = x;
            this.dx = dx;
            this.direction = direction;
        }
    }
    
    /**
     * List of edges for a scanline
     */
    static class EdgeList {
        List<EdgeNode> edges = new ArrayList<>();
        
        void add(EdgeNode edge) {
            edges.add(edge);
        }
        
        void clear() {
            edges.clear();
        }
        
        int size() {
            return edges.size();
        }
        
        EdgeNode get(int index) {
            return edges.get(index);
        }
        
        void sortByX() {
            // Insertion sort - efficient for small lists
            for (int i = 1; i < edges.size(); i++) {
                EdgeNode key = edges.get(i);
                int j = i - 1;
                while (j >= 0 && edges.get(j).x > key.x) {
                    edges.set(j + 1, edges.get(j));
                    j--;
                }
                edges.set(j + 1, key);
            }
        }
        
        void removeInactive(int y) {
            edges.removeIf(edge -> edge.yMax <= y);
        }
    }
    
    private final int minY;
    private final int maxY;
    private final int width;
    private final int height;
    private final EdgeList[] scanlines;
    private final EdgeList active;
    
    /**
     * Create an edge table for a given bounding box
     */
    public EdgeTable(int minY, int maxY, int width, int height) {
        this.minY = minY;
        this.maxY = maxY;
        this.width = width;
        this.height = height;
        
        int numScanlines = maxY - minY + 1;
        this.scanlines = new EdgeList[numScanlines];
        for (int i = 0; i < numScanlines; i++) {
            this.scanlines[i] = new EdgeList();
        }
        
        this.active = new EdgeList();
        
        log.debug("Created edge table: minY={}, maxY={}, width={}, height={}", 
                 minY, maxY, width, height);
    }
    
    /**
     * Add a single edge to the edge table
     */
    public void addEdge(int y1, float x1, int y2, float x2) {
        // Skip horizontal edges
        if (y1 == y2) {
            return;
        }
        
        // Ensure y1 < y2 (swap if needed)
        if (y1 > y2) {
            int tempY = y1;
            y1 = y2;
            y2 = tempY;
            
            float tempX = x1;
            x1 = x2;
            x2 = tempX;
        }
        
        // Clip to surface bounds
        if (y2 < 0 || y1 >= height) {
            return;
        }
        
        // Clip y coordinates
        if (y1 < 0) {
            // Interpolate x at y=0
            float t = (0.0f - y1) / (float)(y2 - y1);
            x1 = x1 + t * (x2 - x1);
            y1 = 0;
        }
        if (y2 >= height) {
            // Interpolate x at y=height-1
            float t = (float)(height - 1 - y1) / (float)(y2 - y1);
            x2 = x1 + t * (x2 - x1);
            y2 = height - 1;
        }
        
        // Calculate dx (change in x per scanline)
        float dx = (x2 - x1) / (float)(y2 - y1);
        
        // Determine edge direction for non-zero winding rule
        int direction = (y2 > y1) ? 1 : -1;
        
        // Create edge node
        EdgeNode edge = new EdgeNode(y2, x1, dx, direction);
        
        // Add edge to appropriate scanline
        int scanlineIdx = y1 - minY;
        if (scanlineIdx >= 0 && scanlineIdx < scanlines.length) {
            scanlines[scanlineIdx].add(edge);
        }
    }
    
    /**
     * Add a line segment as edges (alias for addEdge)
     */
    public void addLine(int x1, int y1, int x2, int y2) {
        addEdge(y1, (float)x1, y2, (float)x2);
    }
    
    /**
     * Add an arc (tessellated into line segments)
     */
    public void addArc(int cx, int cy, int rx, int ry, 
                      double startAngle, double endAngle) {
        // Normalize angles to [0, 2*PI)
        while (startAngle < 0) startAngle += 2.0 * Math.PI;
        while (endAngle < 0) endAngle += 2.0 * Math.PI;
        while (startAngle >= 2.0 * Math.PI) startAngle -= 2.0 * Math.PI;
        while (endAngle >= 2.0 * Math.PI) endAngle -= 2.0 * Math.PI;
        
        // Handle wrap-around
        if (endAngle <= startAngle) {
            endAngle += 2.0 * Math.PI;
        }
        
        // Calculate number of segments based on arc length and radius
        double arcLength = Math.abs(endAngle - startAngle) * (rx + ry) / 2.0;
        int numSegments = (int)(arcLength / 4.0) + 4; // At least 4 segments
        if (numSegments > 360) numSegments = 360; // Cap at 360 segments
        
        double angleStep = (endAngle - startAngle) / numSegments;
        
        // Generate points along the arc
        int prevX = cx + (int)(rx * Math.cos(startAngle));
        int prevY = cy + (int)(ry * Math.sin(startAngle));
        
        for (int i = 1; i <= numSegments; i++) {
            double angle = startAngle + i * angleStep;
            int currX = cx + (int)(rx * Math.cos(angle));
            int currY = cy + (int)(ry * Math.sin(angle));
            
            addLine(prevX, prevY, currX, currY);
            
            prevX = currX;
            prevY = currY;
        }
    }
    
    /**
     * Add a quadratic Bezier curve (tessellated into line segments)
     */
    public void addBezier(int x1, int y1, int cx, int cy, int x2, int y2) {
        // Use 20 segments for quadratic Bezier
        final int numSegments = 20;
        final float step = 1.0f / numSegments;
        
        int prevX = x1;
        int prevY = y1;
        
        for (int i = 1; i <= numSegments; i++) {
            float t = i * step;
            float tInv = 1.0f - t;
            
            // Quadratic Bezier formula: B(t) = (1-t)^2 * P0 + 2*(1-t)*t * P1 + t^2 * P2
            int currX = (int)(tInv * tInv * x1 + 2.0f * tInv * t * cx + t * t * x2);
            int currY = (int)(tInv * tInv * y1 + 2.0f * tInv * t * cy + t * t * y2);
            
            addLine(prevX, prevY, currX, currY);
            
            prevX = currX;
            prevY = currY;
        }
    }
    
    /**
     * Fill using scanline algorithm
     */
    public void fill(int[] pixelData, int surfaceWidth, int surfaceHeight, 
                    int color, int format, int fillRule, 
                    java.awt.Composite composite, java.awt.Rectangle clip) {
        log.debug("edge_table_fill: filling with color=0x{}, fillRule={}", 
                 Integer.toHexString(color), fillRule);
        
        // Process each scanline
        for (int y = minY; y <= maxY; y++) {
            // Skip scanlines outside surface bounds
            if (y < 0 || y >= surfaceHeight) {
                continue;
            }
            
            int scanlineIdx = y - minY;
            
            // Add new edges from this scanline to active list
            EdgeList scanline = scanlines[scanlineIdx];
            for (int i = 0; i < scanline.size(); i++) {
                active.add(scanline.get(i));
            }
            
            // Remove edges that have reached their max y
            active.removeInactive(y);
            
            // Sort active edges by x coordinate
            active.sortByX();
            
            // Fill pixels between edge pairs
            if (fillRule == FILL_RULE_EVENODD) {
                fillScanlineEvenOdd(y, pixelData, surfaceWidth, surfaceHeight, 
                                   color, format, composite, clip);
            } else {
                fillScanlineNonZero(y, pixelData, surfaceWidth, surfaceHeight, 
                                   color, format, composite, clip);
            }
            
            // Update x coordinates for next scanline
            for (int i = 0; i < active.size(); i++) {
                EdgeNode edge = active.get(i);
                edge.x += edge.dx;
            }
        }
        
        log.debug("edge_table_fill: completed");
    }
    
    private void fillScanlineEvenOdd(int y, int[] pixelData, int surfaceWidth, 
                                     int surfaceHeight, int color, int format,
                                     java.awt.Composite composite, java.awt.Rectangle clip) {
        // Even-odd fill rule: toggle on/off at each edge crossing
        for (int i = 0; i + 1 < active.size(); i += 2) {
            EdgeNode e1 = active.get(i);
            EdgeNode e2 = active.get(i + 1);
            
            int xStart = Math.round(e1.x);
            int xEnd = Math.round(e2.x);
            
            // Clip to surface bounds
            xStart = clamp(xStart, 0, surfaceWidth - 1);
            xEnd = clamp(xEnd, 0, surfaceWidth - 1);
            
            // Apply context clip if present
            if (clip != null) {
                xStart = clamp(xStart, clip.x, clip.x + clip.width - 1);
                xEnd = clamp(xEnd, clip.x, clip.x + clip.width - 1);
                
                if (y < clip.y || y >= clip.y + clip.height) {
                    continue;
                }
            }
            
            // Fill pixels in this span
            for (int x = xStart; x <= xEnd; x++) {
                int idx = y * surfaceWidth + x;
                pixelData[idx] = color;
            }
        }
    }
    
    private void fillScanlineNonZero(int y, int[] pixelData, int surfaceWidth, 
                                     int surfaceHeight, int color, int format,
                                     java.awt.Composite composite, java.awt.Rectangle clip) {
        // Non-zero winding fill rule: count crossings with direction
        int winding = 0;
        int spanStart = -1;
        
        for (int i = 0; i < active.size(); i++) {
            EdgeNode e = active.get(i);
            int x = Math.round(e.x);
            
            // Update winding number
            int prevWinding = winding;
            winding += e.direction;
            
            // Check if we're entering or leaving a filled region
            if (prevWinding == 0 && winding != 0) {
                // Entering filled region
                spanStart = x;
            } else if (prevWinding != 0 && winding == 0) {
                // Leaving filled region
                if (spanStart >= 0) {
                    int xStart = spanStart;
                    int xEnd = x;
                    
                    // Clip to surface bounds
                    xStart = clamp(xStart, 0, surfaceWidth - 1);
                    xEnd = clamp(xEnd, 0, surfaceWidth - 1);
                    
                    // Apply context clip if present
                    if (clip != null) {
                        xStart = clamp(xStart, clip.x, clip.x + clip.width - 1);
                        xEnd = clamp(xEnd, clip.x, clip.x + clip.width - 1);
                        
                        if (y < clip.y || y >= clip.y + clip.height) {
                            spanStart = -1;
                            continue;
                        }
                    }
                    
                    // Fill pixels in this span
                    for (int px = xStart; px <= xEnd; px++) {
                        int idx = y * surfaceWidth + px;
                        pixelData[idx] = color;
                    }
                    
                    spanStart = -1;
                }
            }
        }
    }
    
    private static int clamp(int val, int min, int max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }
    
    /**
     * Cleanup method to prepare edge table for reuse
     */
    public void destroy() {
        active.clear();
        for (EdgeList scanline : scanlines) {
            scanline.clear();
        }
    }
}
