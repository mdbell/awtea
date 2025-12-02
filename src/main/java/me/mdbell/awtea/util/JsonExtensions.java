package me.mdbell.awtea.util;

public class JsonExtensions {

    public static StringBuilder appendValue(StringBuilder builder, String key, int value, boolean debug) {
        return builder.append(debug ? "  \"" : "\"").append(key).append(debug ? "\": " : "\":").append(value);
    }

    public static StringBuilder separator(StringBuilder builder, boolean debug) {
        return builder.append(debug ? ",\n" : ",");
    }

    public static StringBuilder finishObject(StringBuilder builder, boolean debug) {
        return builder.append(debug ? "\n  }" : "}");
    }

    public static StringBuilder beginRecord(StringBuilder builder, char key, boolean debug) {
        return builder.append("\"").append(escape(key)).append(debug ? "\": {\n" : "\":{");
    }

    private static String escape(char c) {
        switch (c) {
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            case '\b':
                return "\\b";
            case '\f':
                return "\\f";
            case '\"':
                return "\\\"";
            case '\\':
                return "\\\\";
            default:
                return Character.toString(c);
        }
    }
}
