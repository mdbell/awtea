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


        public Builder define(String name, String value) {
            vars.put(name, value);
            return this;
        }

        public Builder define(String name, float value) {
            vars.put(name, String.valueOf(value));
            return this;
        }

        public Builder define(String name, int value) {
            vars.put(name, String.valueOf(value));
            return this;
        }

        public Builder vec3(String name, float x, float y, float z) {
            vars.put(name, String.format("vec3(%.6f, %.6f, %.6f)", x, y, z));
            return this;
        }

        public Builder fork() {
            Builder newBuilder = new Builder();
            newBuilder.vars.putAll(this.vars);
            return newBuilder;
        }

        public String build(String source) {
            return process(source, vars);
        }
    }
}
