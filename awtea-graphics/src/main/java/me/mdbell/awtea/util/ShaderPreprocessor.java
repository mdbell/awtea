package me.mdbell.awtea.util;

import java.util.HashMap;
import java.util.Map;

public class ShaderPreprocessor {

    /**
     * Process shader source with variable substitutions
     * Variables are defined in the file using #define VAR_NAME value
     */
    public static String process(String shaderSource, Map<String, String> variables) {
        StringBuilder defines = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            defines.append("#define ").append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }
        // check if the shader source already has a #version directive, if so insert defines after it, otherwise insert at the top
        int versionIndex = shaderSource.indexOf("#version");
        if (versionIndex != -1) {
            int lineEndIndex = shaderSource.indexOf("\n", versionIndex);
            if (lineEndIndex != -1) {
                shaderSource = shaderSource.substring(0, lineEndIndex + 1) + defines.toString() + shaderSource.substring(lineEndIndex + 1);
            } else {
                shaderSource = shaderSource + "\n" + defines.toString();
            }
        }
        return shaderSource;
    }

    /**
     * Builder for cleaner syntax
     */
    public static class Builder {
        private Map<String, String> vars = new HashMap<>();

        /**
         * Define a string variable (not quoted - use quotes in value if needed)
         *
         * @param name  The variable name
         * @param value The string value
         * @return The Builder instance
         */
        public Builder define(String name, String value) {
            vars.put(name, value);
            return this;
        }

        /**
         * Define a boolean flag variable (1 if enabled, not defined if disabled)
         *
         * @param name    The variable name
         * @param enabled Whether the flag is enabled
         * @return The Builder instance
         */
        public Builder defineFlag(String name, boolean enabled) {
            if (enabled) {
                vars.put(name, "1");
            }
            return this;
        }


        /**
         * Define a float variable
         *
         * @param name  The variable name
         * @param value The float value
         * @return The Builder instance
         */
        public Builder define(String name, float value) {
            vars.put(name, String.valueOf(value));
            return this;
        }

        /**
         * Define an integer variable
         *
         * @param name  The variable name
         * @param value The integer value
         * @return The Builder instance
         */
        public Builder define(String name, int value) {
            vars.put(name, String.valueOf(value));
            return this;
        }

        /**
         * Define a vec3 variable
         *
         * @param name The variable name
         * @param x    The x component
         * @param y    The y component
         * @param z    The z component
         * @return The Builder instance
         */
        public Builder vec3(String name, float x, float y, float z) {
            vars.put(name, String.format("vec3(%.6f, %.6f, %.6f)", x, y, z));
            return this;
        }
      
      public Builder defineVec3(String name, float x, float y, float z) {
        return vec3(name, x, y, z);
      }

        /**
         * Define a vec4 variable
         *
         * @param name The variable name
         * @param x    The x component
         * @param y    The y component
         * @param z    The z component
         * @param w    The w component
         * @return The Builder instance
         */
        public Builder defineVec4(String name, float x, float y, float z, float w) {
            vars.put(name, String.format("vec4(%.6f, %.6f, %.6f, %.6f)", x, y, z, w));
            return this;
        }

        /**
         * Define a mat4 variable
         *
         * @param name The variable name
         * @param m    The 16-element float array representing the matrix
         * @return The Builder instance
         */
        public Builder mat4(String name, float[] m) {
            if (m.length != 16) throw new IllegalArgumentException("mat4 requires 16 elements");
            vars.put(name, String.format(
                    "mat4(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f)",
                    m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11], m[12], m[13], m[14], m[15]
            ));
            return this;
        }

        /**
         * Create a fork of this builder with the same variables
         *
         * @return A new Builder instance with copied variables
         */
        public Builder fork() {
            Builder newBuilder = new Builder();
            newBuilder.vars.putAll(this.vars);
            return newBuilder;
        }

        /**
         * Build the processed shader source
         *
         * @param source The original shader source
         * @return The processed shader source with variables applied
         */
        public String build(String source) {
            return process(source, vars);
        }
    }
}
