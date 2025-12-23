package me.mdbell.awtea.gfx.webgl;

import org.teavm.jso.webgl.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a custom shader program with lifecycle management.
 * Provides an API for advanced users to supply and use custom GLSL shaders
 * in the WebGL rendering backend.
 */
public class CustomShaderProgram {

    private final WebGL2RenderingContext gl;
    private final WebGLProgram program;
    private final String name;
    
    // Cached uniform locations
    private final Map<String, WebGLUniformLocation> uniformLocations = new HashMap<>();
    
    // Cached attribute locations
    private final Map<String, Integer> attributeLocations = new HashMap<>();
    
    private boolean disposed = false;

    /**
     * Creates a new custom shader program from vertex and fragment shader sources.
     * 
     * @param gl the WebGL context
     * @param name a descriptive name for this shader program (for debugging)
     * @param vertexSource GLSL vertex shader source code
     * @param fragmentSource GLSL fragment shader source code
     * @throws RuntimeException if shader compilation or linking fails
     */
    public CustomShaderProgram(WebGL2RenderingContext gl, String name, String vertexSource, String fragmentSource) {
        this.gl = gl;
        this.name = name;
        
        WebGLShader vertexShader = compileShader(WebGLRenderingContext.VERTEX_SHADER, vertexSource);
        WebGLShader fragmentShader = compileShader(WebGLRenderingContext.FRAGMENT_SHADER, fragmentSource);
        
        this.program = gl.createProgram();
        gl.attachShader(program, vertexShader);
        gl.attachShader(program, fragmentShader);
        gl.linkProgram(program);
        
        if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
            String log = gl.getProgramInfoLog(program);
            dispose();
            throw new RuntimeException("Failed to link custom shader program '" + name + "': " + log);
        }
        
        // Shaders can be deleted after linking
        gl.deleteShader(vertexShader);
        gl.deleteShader(fragmentShader);
    }

    private WebGLShader compileShader(int type, String source) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        
        if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
            String log = gl.getShaderInfoLog(shader);
            String typeName = (type == WebGLRenderingContext.VERTEX_SHADER) ? "vertex" : "fragment";
            gl.deleteShader(shader);
            throw new RuntimeException("Failed to compile " + typeName + " shader for '" + name + "': " + log);
        }
        
        return shader;
    }

    /**
     * Gets the location of a uniform variable in this shader program.
     * Results are cached for performance.
     * 
     * @param uniformName the name of the uniform variable
     * @return the uniform location, or null if not found
     */
    public WebGLUniformLocation getUniformLocation(String uniformName) {
        checkNotDisposed();
        return uniformLocations.computeIfAbsent(uniformName, name -> gl.getUniformLocation(program, name));
    }

    /**
     * Gets the location of an attribute variable in this shader program.
     * Results are cached for performance.
     * 
     * @param attributeName the name of the attribute variable
     * @return the attribute location, or -1 if not found
     */
    public int getAttributeLocation(String attributeName) {
        checkNotDisposed();
        return attributeLocations.computeIfAbsent(attributeName, name -> gl.getAttribLocation(program, name));
    }

    /**
     * Sets a uniform float value.
     * 
     * @param uniformName the name of the uniform
     * @param value the value to set
     */
    public void setUniform1f(String uniformName, float value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniform1f(loc, value);
        }
    }

    /**
     * Sets a uniform vec2 value.
     * 
     * @param uniformName the name of the uniform
     * @param x the x component
     * @param y the y component
     */
    public void setUniform2f(String uniformName, float x, float y) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniform2f(loc, x, y);
        }
    }

    /**
     * Sets a uniform vec3 value.
     * 
     * @param uniformName the name of the uniform
     * @param x the x component
     * @param y the y component
     * @param z the z component
     */
    public void setUniform3f(String uniformName, float x, float y, float z) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniform3f(loc, x, y, z);
        }
    }

    /**
     * Sets a uniform vec4 value.
     * 
     * @param uniformName the name of the uniform
     * @param x the x component
     * @param y the y component
     * @param z the z component
     * @param w the w component
     */
    public void setUniform4f(String uniformName, float x, float y, float z, float w) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniform4f(loc, x, y, z, w);
        }
    }

    /**
     * Sets a uniform int value.
     * 
     * @param uniformName the name of the uniform
     * @param value the value to set
     */
    public void setUniform1i(String uniformName, int value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniform1i(loc, value);
        }
    }

    /**
     * Sets a uniform mat3 value from a Float32Array.
     * 
     * @param uniformName the name of the uniform
     * @param transpose whether to transpose the matrix
     * @param value the matrix values (9 floats in column-major order)
     */
    public void setUniformMatrix3fv(String uniformName, boolean transpose, org.teavm.jso.typedarrays.Float32Array value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniformMatrix3fv(loc, transpose, value);
        }
    }

    /**
     * Sets a uniform mat3 value from a float array.
     * 
     * @param uniformName the name of the uniform
     * @param transpose whether to transpose the matrix
     * @param value the matrix values (9 floats in column-major order)
     */
    public void setUniformMatrix3fv(String uniformName, boolean transpose, float[] value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniformMatrix3fv(loc, transpose, value);
        }
    }

    /**
     * Sets a uniform mat4 value from a Float32Array.
     * 
     * @param uniformName the name of the uniform
     * @param transpose whether to transpose the matrix
     * @param value the matrix values (16 floats in column-major order)
     */
    public void setUniformMatrix4fv(String uniformName, boolean transpose, org.teavm.jso.typedarrays.Float32Array value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniformMatrix4fv(loc, transpose, value);
        }
    }

    /**
     * Sets a uniform mat4 value.
     * 
     * @param uniformName the name of the uniform
     * @param transpose whether to transpose the matrix
     * @param value the matrix values (16 floats in column-major order)
     */
    public void setUniformMatrix4fv(String uniformName, boolean transpose, float[] value) {
        WebGLUniformLocation loc = getUniformLocation(uniformName);
        if (loc != null) {
            gl.uniformMatrix4fv(loc, transpose, value);
        }
    }

    /**
     * Enables a vertex attribute array.
     * 
     * @param attributeName the name of the attribute
     */
    public void enableVertexAttribArray(String attributeName) {
        int loc = getAttributeLocation(attributeName);
        if (loc >= 0) {
            gl.enableVertexAttribArray(loc);
        }
    }

    /**
     * Disables a vertex attribute array.
     * 
     * @param attributeName the name of the attribute
     */
    public void disableVertexAttribArray(String attributeName) {
        int loc = getAttributeLocation(attributeName);
        if (loc >= 0) {
            gl.disableVertexAttribArray(loc);
        }
    }

    /**
     * Sets up a vertex attribute pointer.
     * 
     * @param attributeName the name of the attribute
     * @param size the number of components per attribute (1-4)
     * @param type the data type (e.g., WebGLRenderingContext.FLOAT)
     * @param normalized whether to normalize the data
     * @param stride the byte offset between consecutive attributes
     * @param offset the byte offset to the first attribute
     */
    public void vertexAttribPointer(String attributeName, int size, int type, boolean normalized, int stride, int offset) {
        int loc = getAttributeLocation(attributeName);
        if (loc >= 0) {
            gl.vertexAttribPointer(loc, size, type, normalized, stride, offset);
        }
    }

    /**
     * Activates this shader program for rendering.
     */
    public void use() {
        checkNotDisposed();
        gl.useProgram(program);
    }

    /**
     * Gets the underlying WebGL program object.
     * 
     * @return the WebGL program
     */
    public WebGLProgram getProgram() {
        return program;
    }

    /**
     * Gets the name of this shader program.
     * 
     * @return the shader program name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this shader program has been disposed.
     * 
     * @return true if disposed
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Disposes of this shader program and releases GPU resources.
     * After calling this method, the shader program cannot be used.
     */
    public void dispose() {
        if (!disposed) {
            if (program != null) {
                gl.deleteProgram(program);
            }
            uniformLocations.clear();
            attributeLocations.clear();
            disposed = true;
        }
    }

    private void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException("Shader program '" + name + "' has been disposed");
        }
    }
}
