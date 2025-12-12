precision mediump float;

varying vec2 v_texCoord;
uniform sampler2D u_texture;
uniform int u_swizzleMode;


void main() {
    vec4 tex = texture2D(u_texture, v_texCoord);

    // u_swizzleMode:
    // 0 - RGBA
    // 1 - BGRA
    // 2 - BGR (alpha set to 1.0)
    // 3 - RGB (alpha set to 1.0)

    if (u_swizzleMode == 0) {
        gl_FragColor = tex; // RGBA
    } else if (u_swizzleMode == 1) {
        gl_FragColor = vec4(tex.b, tex.g, tex.r, tex.a); // BGRA
    } else if (u_swizzleMode == 2) {
        gl_FragColor = vec4(tex.bgr, 1.0); // BGR
    } else if (u_swizzleMode == 3) {
        gl_FragColor = vec4(tex.rgb, 1.0); // RGB
    } else {
        gl_FragColor = tex; // Default to RGBA
    }
}
