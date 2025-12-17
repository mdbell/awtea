package me.mdbell.awtea.examples.animationdemo;

import me.mdbell.awtea.Helper;
import me.mdbell.awtea.gfx.DefaultSurfaceBackend;
import me.mdbell.awtea.gfx.wasm.WasmDiagnostics;
import me.mdbell.awtea.gfx.wasm.WasmSurfaceBackend;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animation demo showcasing:
 * - Frame-based animation loop (~60 FPS)
 * - Physics simulation with gravity
 * - Collision detection
 * - Mouse and keyboard interaction
 * - Double buffering
 * - FPS monitoring
 * - WASM diagnostics (when running under TeaVM with WASM backend)
 */
public class AnimationDemo {

    public static void main(String[] args) {
        // LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        // System.setProperty("me.mdbell.awtea.gfx.backend", "java");
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");

        // Create the main window
        Frame frame = new Frame();
        frame.setTitle("Animation Demo - awtea Example");
        frame.setSize(800, 600);

        // Create and add the animation canvas
        AnimationCanvas canvas = new AnimationCanvas();

        canvas.setSize(800, 600);

        frame.add(canvas);

        // Show the window
        frame.setVisible(true);

        // Start animation loop
        canvas.startAnimation();
    }
    
    /**
     * Check if we're running under TeaVM with WASM backend loaded.
     * @return true if WASM diagnostics are available
     */
    private static boolean isWasmBackendAvailable() {
        // First check if running under TeaVM
        if (!Helper.isTeaVM()) {
            return false;
        }
        
        try {
            DefaultSurfaceBackend defaultBackend = DefaultSurfaceBackend.getDefault();
            return defaultBackend.getWasmBackend() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get WASM diagnostics if available.
     * @return WasmDiagnostics instance, or null if not available
     */
    private static WasmDiagnostics getWasmDiagnostics() {
        // First check if running under TeaVM
        if (!Helper.isTeaVM()) {
            return null;
        }
        
        try {
            DefaultSurfaceBackend defaultBackend = DefaultSurfaceBackend.getDefault();
            WasmSurfaceBackend wasmBackend = defaultBackend.getWasmBackend();
            if (wasmBackend != null) {
                return wasmBackend.getDiagnostics();
            }
        } catch (Exception e) {
            // Ignore - not running with WASM
        }
        return null;
    }

    /**
     * Canvas that handles animation, physics, and user interaction.
     */
    static class AnimationCanvas extends Canvas implements Runnable {
        private static final int TARGET_FPS = 60;
        private static final long FRAME_TIME_MS = 1000 / TARGET_FPS;
        private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);

        private final List<Ball> balls;
        private final FPSCounter fpsCounter;
        private final Random random;

        private Thread animationThread;
        private volatile boolean running;
        private volatile boolean paused;

        private Image offscreenImage;
        private Graphics offscreenGraphics;

        // Visual options
        private boolean showTrails;
        private boolean showVelocityVectors;

        // Mouse state
        private int mouseX = -1;
        private int mouseY = -1;

        public AnimationCanvas() {
            balls = new ArrayList<>();
            fpsCounter = new FPSCounter();
            random = new Random();
            running = false;
            paused = false;
            showTrails = false;
            showVelocityVectors = false;

            // Initialize with 15 random balls
            initializeBalls();

            // Set up mouse listeners
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Left click: Add ball at mouse position
                        addRandomBall(e.getX(), e.getY(), false);
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // Right click: Add ball with random velocity
                        addRandomBall(e.getX(), e.getY(), true);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    mouseX = e.getX();
                    mouseY = e.getY();
                }
            });

            // Set up keyboard listener
            setFocusable(true);
            addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    handleKeyPress(e.getKeyCode());
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }
            });

            // Request focus for keyboard input
            requestFocus();
        }

        /**
         * Initializes the canvas with random balls.
         */
        private void initializeBalls() {
            balls.clear();
            for (int i = 0; i < 15; i++) {
                addRandomBall(-1, -1, false);
            }
        }

        /**
         * Adds a ball with random properties.
         * 
         * @param x              X position (-1 for random)
         * @param y              Y position (-1 for random)
         * @param randomVelocity Whether to add random velocity
         */
        private void addRandomBall(int x, int y, boolean randomVelocity) {
            int width = getWidth();
            int height = getHeight();

            if (width <= 0 || height <= 0) {
                width = 800;
                height = 600;
            }

            int radius = 10 + random.nextInt(20); // 10-30 pixels

            double posX = x < 0 ? radius + random.nextDouble() * (width - 2 * radius) : x;
            double posY = y < 0 ? radius + random.nextDouble() * (height - 2 * radius) : y;

            Color color = new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256));

            if (randomVelocity) {
                double vx = -200 + random.nextDouble() * 400; // -200 to 200
                double vy = -300 + random.nextDouble() * 200; // -300 to -100
                balls.add(new Ball(posX, posY, vx, vy, radius, color));
            } else {
                balls.add(new Ball(posX, posY, radius, color));
            }
        }

        /**
         * Handles keyboard input.
         * 
         * @param keyCode The key code that was pressed
         */
        private void handleKeyPress(int keyCode) {
            switch (keyCode) {
                case KeyEvent.VK_SPACE:
                    paused = !paused;
                    break;
                case KeyEvent.VK_C:
                    balls.clear();
                    break;
                case KeyEvent.VK_R:
                    initializeBalls();
                    break;
                case KeyEvent.VK_T:
                    showTrails = !showTrails;
                    break;
                case KeyEvent.VK_V:
                    showVelocityVectors = !showVelocityVectors;
                    break;
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                    addRandomBall(-1, -1, false);
                    break;
                case KeyEvent.VK_MINUS:
                    if (!balls.isEmpty()) {
                        balls.remove(balls.size() - 1);
                    }
                    break;
            }
        }

        /**
         * Starts the animation loop.
         */
        public void startAnimation() {
            if (running) {
                return;
            }

            running = true;
            animationThread = new Thread(this);
            animationThread.start();
        }

        /**
         * Stops the animation loop.
         */
        public void stopAnimation() {
            running = false;
            if (animationThread != null) {
                try {
                    animationThread.join();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        @Override
        public void run() {
            long lastTime = System.currentTimeMillis();

            while (running) {
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastTime;

                if (!paused) {
                    // Update physics
                    double deltaTime = elapsed / 1000.0;
                    updatePhysics(deltaTime);
                }

                // Render
                repaint();
                fpsCounter.frame();

                lastTime = currentTime;

                // Sleep to target frame rate
                try {
                    long sleepTime = FRAME_TIME_MS - elapsed;
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        /**
         * Updates physics simulation for all balls.
         * 
         * @param deltaTime Time elapsed since last update (in seconds)
         */
        private void updatePhysics(double deltaTime) {
            int width = getWidth();
            int height = getHeight();

            for (Ball ball : balls) {
                ball.update(deltaTime, width, height);
            }
        }

        @Override
        public void paint(Graphics g) {
            int width = getWidth();
            int height = getHeight();

            // Create offscreen buffer if needed
            if (offscreenImage == null ||
                    offscreenImage.getWidth(null) != width ||
                    offscreenImage.getHeight(null) != height) {
                offscreenImage = createImage(width, height);
                if (offscreenImage != null) {
                    offscreenGraphics = offscreenImage.getGraphics();
                }
            }

            // Use offscreen graphics if available, otherwise draw directly
            Graphics drawGraphics = offscreenGraphics != null ? offscreenGraphics : g;

            // Clear background (with trails effect if enabled)
            if (showTrails) {
                // Semi-transparent overlay for motion trails
                drawGraphics.setColor(new Color(240, 240, 240, 30));
                drawGraphics.fillRect(0, 0, width, height);
            } else {
                // Solid background
                drawGraphics.setColor(BACKGROUND_COLOR);
                drawGraphics.fillRect(0, 0, width, height);
            }

            // Draw all balls
            for (Ball ball : balls) {
                ball.draw(drawGraphics);
            }

            // Draw velocity vectors if enabled
            if (showVelocityVectors) {
                for (Ball ball : balls) {
                    ball.drawVelocityVector(drawGraphics);
                }
            }

            // Draw FPS counter
            drawFPSCounter(drawGraphics);
            
            // Draw WASM diagnostics (if available)
            drawWasmDiagnostics(drawGraphics);

            // Draw controls
            drawControls(drawGraphics);

            // Draw ball count
            drawBallCount(drawGraphics);

            // Draw paused indicator
            if (paused) {
                drawPausedIndicator(drawGraphics, width, height);
            }

            // Copy offscreen buffer to screen
            if (offscreenImage != null) {
                g.drawImage(offscreenImage, 0, 0, null);
            }
        }

        /**
         * Draws the FPS counter in the top-left corner.
         */
        private void drawFPSCounter(Graphics g) {
            double fps = fpsCounter.getFPS();

            // Background
            g.setColor(Color.WHITE);
            g.fillRect(5, 5, 100, 25);

            // Border
            g.setColor(Color.BLACK);
            g.drawRect(5, 5, 100, 25);

            // Text
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString(String.format("FPS: %.1f", fps), 10, 22);
        }
        
        /**
         * Draws WASM diagnostics below the FPS counter (if available).
         */
        private void drawWasmDiagnostics(Graphics g) {
            WasmDiagnostics diag = AnimationDemo.getWasmDiagnostics();
            if (diag == null) {
                return; // Not running with WASM backend
            }
            
            int y = 65; // Position below ball count
            
            // Background
            g.setColor(Color.WHITE);
            g.fillRect(5, y, 150, 55);
            
            // Border
            g.setColor(Color.BLACK);
            g.drawRect(5, y, 150, 55);
            
            // Text
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(String.format("Surf: %d/%d", 
                    diag.getActiveSurfaceCount(), diag.getMaxSurfaces()), 10, y + 15);
            g.drawString(String.format("Ctx: %d/%d", 
                    diag.getActiveContextCount(), diag.getMaxContexts()), 10, y + 30);
            g.drawString(String.format("Mem: %.1f KB", diag.getAllocatedKB()), 10, y + 45);
        }

        /**
         * Draws the controls reference in the top-right corner.
         */
        private void drawControls(Graphics g) {
            int width = getWidth();
            int x = width - 220;
            int y = 5;

            String[] controls = {
                    "Controls:",
                    "SPACE: Pause",
                    "C: Clear",
                    "R: Reset",
                    "T: Trails",
                    "V: Vectors",
                    "+/-: Add/Remove",
                    "Click: Add ball"
            };

            // Background
            g.setColor(new Color(255, 255, 255, 230));
            g.fillRect(x, y, 210, controls.length * 18 + 10);

            // Border
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 210, controls.length * 18 + 10);

            // Text
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            for (int i = 0; i < controls.length; i++) {
                if (i == 0) {
                    g.setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                }
                g.drawString(controls[i], x + 8, y + 18 + i * 18);
            }
        }

        /**
         * Draws the ball count.
         */
        private void drawBallCount(Graphics g) {
            // Background
            g.setColor(Color.WHITE);
            g.fillRect(5, 35, 100, 25);

            // Border
            g.setColor(Color.BLACK);
            g.drawRect(5, 35, 100, 25);

            // Text
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.drawString("Balls: " + balls.size(), 10, 52);
        }

        /**
         * Draws paused indicator overlay.
         */
        private void drawPausedIndicator(Graphics g, int width, int height) {
            // Semi-transparent overlay
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect(0, 0, width, height);

            // "PAUSED" text
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            String text = "PAUSED";

            // Center the text
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textX = (width - textWidth) / 2;
            int textY = height / 2;

            // Draw text with shadow
            g.setColor(Color.BLACK);
            g.drawString(text, textX + 2, textY + 2);
            g.setColor(Color.WHITE);
            g.drawString(text, textX, textY);
        }

        @Override
        public void update(Graphics g) {
            // Override to prevent default clear behavior (for smoother rendering)
            paint(g);
        }
    }
}
