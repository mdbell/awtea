package me.mdbell.awtea.examples.animationdemo;

/**
 * Tracks frame rates over a rolling window to calculate average FPS.
 */
public class FPSCounter {
    private static final int WINDOW_SIZE = 60; // Track last 60 frames
    private final long[] frameTimes;
    private int frameIndex;
    private int frameCount;
    private long lastFrameTime;
    
    public FPSCounter() {
        frameTimes = new long[WINDOW_SIZE];
        frameIndex = 0;
        frameCount = 0;
        lastFrameTime = System.currentTimeMillis();
    }
    
    /**
     * Records a frame timing. Call this once per frame.
     */
    public void frame() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastFrameTime;
        
        frameTimes[frameIndex] = deltaTime;
        frameIndex = (frameIndex + 1) % WINDOW_SIZE;
        
        if (frameCount < WINDOW_SIZE) {
            frameCount++;
        }
        
        lastFrameTime = currentTime;
    }
    
    /**
     * Calculates and returns the current average FPS.
     * 
     * @return Average frames per second over the rolling window
     */
    public double getFPS() {
        if (frameCount == 0) {
            return 0.0;
        }
        
        long totalTime = 0;
        for (int i = 0; i < frameCount; i++) {
            totalTime += frameTimes[i];
        }
        
        if (totalTime == 0) {
            return 0.0;
        }
        
        // FPS = frames / (total time in seconds)
        return (frameCount * 1000.0) / totalTime;
    }
}
