package me.mdbell.awtea.util;

public class NormalizedPoint {

    private float x, y;

    public NormalizedPoint() {
    }

    public NormalizedPoint(int x, int y, int width, int height) {
        this.x = (float) x / width;
        this.y = (float) y / height;
    }

    public NormalizedPoint(float x, float y) {
        this.x = clamp(x, 0, 1);
        this.y = clamp(y, 0, 1);
    }

    public float getNormalizedX() {
        return x;
    }

    public float getNormalizedY() {
        return y;
    }

    public int getX(int width) {
        return (int) (x * width);
    }

    public int getY(int height) {
        return (int) (y * height);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
