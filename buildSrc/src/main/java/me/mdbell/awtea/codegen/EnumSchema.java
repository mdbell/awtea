package me.mdbell.awtea.codegen;

import java.util.List;

/**
 * Represents an enum schema loaded from YAML
 */
public class EnumSchema {
    private String name;
    private String description;
    private String c_prefix;
    private String java_prefix;
    private String java_name;
    private String java_package;
    private List<EnumValue> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getC_prefix() {
        return c_prefix;
    }

    public void setC_prefix(String c_prefix) {
        this.c_prefix = c_prefix;
    }

    public String getJava_prefix() {
        return java_prefix;
    }

    public void setJava_prefix(String java_prefix) {
        this.java_prefix = java_prefix;
    }

    public String getJava_name() {
        return java_name;
    }

    public void setJava_name(String java_name) {
        this.java_name = java_name;
    }

    public String getJava_package() {
        return java_package;
    }

    public void setJava_package(String java_package) {
        this.java_package = java_package;
    }

    public List<EnumValue> getValues() {
        return values;
    }

    public void setValues(List<EnumValue> values) {
        this.values = values;
    }

    /**
     * Represents a single enum value
     */
    public static class EnumValue {
        private String name;
        private Integer value;
        private String description;
        private Boolean c_only;
        private Boolean java_only;
        private Boolean ts_only;
        private Boolean java_skip;
        private String c_name;
        private String java_name;
        private String ts_name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getC_only() {
            return c_only != null && c_only;
        }

        public void setC_only(Boolean c_only) {
            this.c_only = c_only;
        }

        public Boolean getJava_only() {
            return java_only != null && java_only;
        }

        public void setJava_only(Boolean java_only) {
            this.java_only = java_only;
        }

        public Boolean getTs_only() {
            return ts_only != null && ts_only;
        }

        public void setTs_only(Boolean ts_only) {
            this.ts_only = ts_only;
        }

        public Boolean getJava_skip() {
            return java_skip != null && java_skip;
        }

        public void setJava_skip(Boolean java_skip) {
            this.java_skip = java_skip;
        }

        public String getC_name() {
            return c_name;
        }

        public void setC_name(String c_name) {
            this.c_name = c_name;
        }

        public String getJava_name() {
            return java_name;
        }

        public void setJava_name(String java_name) {
            this.java_name = java_name;
        }

        public String getTs_name() {
            return ts_name;
        }

        public void setTs_name(String ts_name) {
            this.ts_name = ts_name;
        }
    }
}
