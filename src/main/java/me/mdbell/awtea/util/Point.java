package me.mdbell.awtea.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Point {

    private int x, y;

    public NormalizedPoint normalize(int width, int height) {
        return new NormalizedPoint(x, y, width, height);
    }

    public void translate(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public void scale(double sx, double sy) {
        double tmpX = x;
        double tmpY = y;

        x = (int) (sx * tmpX);
        y = (int) (sy * tmpY);
    }
}
