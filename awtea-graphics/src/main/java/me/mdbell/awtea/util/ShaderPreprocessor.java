package me.mdbell.awtea.util;

import java.util.HashMap;
import java.util.Map;

public class ShaderPreprocessor {

    /**
     * Process shader source with variable substitutions
     */
    public static String process(String shaderSource, Map<String, String> variables) {

        StringBuilder defineBlock = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            defineBlock.append("#define ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n");
        }

        int firstNewline = shaderSource.indexOf('\n');
        String result;
        if (shaderSource.startsWith("#version") && firstNewline != -1) {
            String versionLine = shaderSource.substring(0, firstNewline + 1);
            String restOfShader = shaderSource.substring(firstNewline + 1);
            result = versionLine + defineBlock + restOfShader;

        } else {
            result = defineBlock + shaderSource;
        }

        return result;
    }

    /**
     * Builder for cleaner syntax
     */
    public static class Builder {
        private final Map<String, String> vars = new HashMap<>();

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
