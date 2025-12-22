precision mediump float;

uniform vec4 u_color;
uniform int u_pickingMode;  // 0 = normal, 1 = picking
uniform vec4 u_pickingColor; // Component ID as color (for picking mode)

void main() {
    if (u_pickingMode == 1) {
        // Picking mode: output component ID color where we would have drawn
        // Check alpha to only write to picking buffer where color is visible
        if (u_color.a > 0.0) {
            gl_FragColor = u_pickingColor;
        } else {
            discard;  // Don't write transparent pixels to picking buffer
        }
    } else {
        // Normal mode: output actual color
        gl_FragColor = u_color;
    }
}
