package me.mdbell.awtea.examples.animationdemo;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Represents a bouncing ball with physics properties.
 */
public class Ball {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private final int radius;
    private final Color color;
    
    // Physics constants
    private static final double GRAVITY = 400.0; // pixels/second^2
    private static final double DAMPING = 0.8;    // velocity multiplier on bounce
    private static final double MIN_BOUNCE_VELOCITY = 50.0; // minimum velocity to bounce
    
    // Rendering constants
    private static final int CIRCLE_SIDES = 20; // number of sides for circle approximation
    
    /**
     * Creates a new ball at the specified position.
     * 
     * @param x Initial x coordinate
     * @param y Initial y coordinate
     * @param radius Ball radius in pixels
     * @param color Ball color
     */
    public Ball(double x, double y, int radius, Color color) {
        this.x = x;
        this.y = y;
        this.vx = 0;
        this.vy = 0;
        this.radius = radius;
        this.color = color;
    }
    
    /**
     * Creates a new ball with initial velocity.
     * 
     * @param x Initial x coordinate
     * @param y Initial y coordinate
     * @param vx Initial x velocity
     * @param vy Initial y velocity
     * @param radius Ball radius in pixels
     * @param color Ball color
     */
    public Ball(double x, double y, double vx, double vy, int radius, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
    }
    
    /**
     * Updates the ball's position based on physics simulation.
     * 
     * @param deltaTime Time elapsed since last update (in seconds)
     * @param width Canvas width for collision detection
     * @param height Canvas height for collision detection
     */
    public void update(double deltaTime, int width, int height) {
        // Apply gravity
        vy += GRAVITY * deltaTime;
        
        // Update position
        x += vx * deltaTime;
        y += vy * deltaTime;
        
        // Check collision with left/right walls
        if (x - radius < 0) {
            x = radius;
            vx = -vx * DAMPING;
        } else if (x + radius > width) {
            x = width - radius;
            vx = -vx * DAMPING;
        }
        
        // Check collision with top/bottom walls
        if (y - radius < 0) {
            y = radius;
            vy = -vy * DAMPING;
        } else if (y + radius > height) {
            y = height - radius;
            vy = -vy * DAMPING;
            
            // Prevent sticking at the bottom
            if (Math.abs(vy) < MIN_BOUNCE_VELOCITY) {
                vy = 0;
            }
        }
    }
    
    /**
     * Draws the ball on the provided Graphics context.
     * 
     * @param g Graphics context to draw on
     */
    public void draw(Graphics g) {
        // Approximate a circle using a polygon
        int[] xPoints = new int[CIRCLE_SIDES];
        int[] yPoints = new int[CIRCLE_SIDES];
        
        for (int i = 0; i < CIRCLE_SIDES; i++) {
            double angle = 2 * Math.PI * i / CIRCLE_SIDES;
            xPoints[i] = (int)(x + radius * Math.cos(angle));
            yPoints[i] = (int)(y + radius * Math.sin(angle));
        }
        
        g.setColor(color);
        g.fillPolygon(xPoints, yPoints, CIRCLE_SIDES);
        
        // Draw outline
        g.setColor(Color.BLACK);
        g.drawPolygon(xPoints, yPoints, CIRCLE_SIDES);
    }
    
    /**
     * Draws velocity vector for visualization.
     * 
     * @param g Graphics context to draw on
     */
    public void drawVelocityVector(Graphics g) {
        g.setColor(Color.RED);
        int endX = (int)(x + vx * 0.1); // Scale down for visibility
        int endY = (int)(y + vy * 0.1);
        g.drawLine((int)x, (int)y, endX, endY);
        
        // Draw arrowhead
        double angle = Math.atan2(vy, vx);
        int arrowSize = 5;
        int x1 = (int)(endX - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int)(endY - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int)(endX - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int)(endY - arrowSize * Math.sin(angle + Math.PI / 6));
        g.drawLine(endX, endY, x1, y1);
        g.drawLine(endX, endY, x2, y2);
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }
}
