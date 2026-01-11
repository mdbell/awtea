package me.mdbell.awtea.util;

import java.util.HashMap;
import java.util.Map;

public class ShaderPreprocessor {

    /**
     * Process shader source with variable substitutions
     * Variables in shader should be written as:  ${VARIABLE_NAME}
     */
    public static String process(String shaderSource, Map<String, String> variables) {
        String result = shaderSource;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }

    /**
     * Builder for cleaner syntax
     */
    public static class Builder {
        private String source;
        private Map<String, String> vars = new HashMap<>();

        public Builder(String source) {
            this.source = source;
        }

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

        public Builder defineVec3(String name, float x, float y, float z) {
            vars.put(name, String.format("vec3(%.6f, %.6f, %.6f)", x, y, z));
            return this;
        }

        public String build() {
            return process(source, vars);
        }
    }
}
