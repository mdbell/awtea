attribute vec2 a_position;
attribute vec2 a_texCoord;

uniform vec2 u_resolution;
uniform mat3 u_transform;

varying vec2 v_texCoord;

void main() {
    vec3 pos = u_transform * vec3(a_position, 1.0);
    vec2 zeroToOne = pos.xy / u_resolution;
    vec2 zeroToTwo = zeroToOne * 2.0;
    vec2 clipSpace = zeroToTwo - 1.0;
    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
    v_texCoord = a_texCoord;
}
