package me.mdbell.awtea.codegen;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates Java enum files from enum schemas
 */
public class JavaEnumGenerator {
    
    private final String packageName;
    
    public JavaEnumGenerator(String packageName) {
        this.packageName = packageName;
    }
    
    public void generate(EnumSchema schema, Path outputDir) throws IOException {
        // Determine if this should be an interface with constants or a proper enum
        boolean isInterface = schema.getJava_prefix() != null;
        
        String className = getJavaClassName(schema);
        String filename = className + ".java";
        Path outputFile = outputDir.resolve(filename);
        
        Files.createDirectories(outputDir);
        
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writeHeader(writer, schema);
            if (isInterface) {
                writeInterface(writer, schema, className);
            } else {
                writeEnum(writer, schema, className);
            }
        }
        
        System.out.println("Generated Java file: " + outputFile);
    }
    
    private void writeHeader(Writer writer, EnumSchema schema) throws IOException {
        writer.write("/**\n");
        writer.write(" * AUTO-GENERATED FILE - DO NOT EDIT\n");
        writer.write(" * Generated from: schemas/" + toKebabCase(schema.getName()) + ".yaml\n");
        writer.write(" * \n");
        writer.write(" * " + schema.getDescription() + "\n");
        writer.write(" */\n");
        writer.write("package " + packageName + ";\n\n");
    }
    
    private void writeInterface(Writer writer, EnumSchema schema, String className) throws IOException {
        writer.write("public interface " + className + " {\n");
        
        int currentValue = 0;
        for (EnumSchema.EnumValue value : schema.getValues()) {
            if (value.getC_only() || value.getTs_only() || value.getJava_skip()) {
                continue;
            }
            
            String javaName = getJavaName(schema, value);
            
            // Write comment if description exists
            if (value.getDescription() != null && !value.getDescription().isEmpty()) {
                writer.write("\t/** " + value.getDescription() + " */\n");
            }
            
            writer.write("\tint " + javaName + " = ");
            
            if (value.getValue() != null) {
                writer.write(value.getValue().toString());
                currentValue = value.getValue() + 1;
            } else {
                writer.write(String.valueOf(currentValue));
                currentValue++;
            }
            
            writer.write(";\n");
        }
        
        writer.write("}\n");
    }
    
    private void writeEnum(Writer writer, EnumSchema schema, String className) throws IOException {
        boolean hasValues = schema.getValues().stream()
            .anyMatch(v -> v.getValue() != null);
        
        writer.write("public enum " + className + " {\n");
        
        int currentValue = 0;
        boolean first = true;
        
        for (EnumSchema.EnumValue value : schema.getValues()) {
            if (value.getC_only() || value.getTs_only() || value.getJava_skip()) {
                continue;
            }
            
            if (!first) {
                writer.write(",\n");
            }
            first = false;
            
            String javaName = getJavaName(schema, value);
            
            // Write comment if description exists
            if (value.getDescription() != null && !value.getDescription().isEmpty()) {
                writer.write("\t/** " + value.getDescription() + " */\n");
            }
            
            writer.write("\t" + javaName);
            
            if (hasValues) {
                if (value.getValue() != null) {
                    writer.write("(" + value.getValue() + ")");
                    currentValue = value.getValue() + 1;
                } else {
                    writer.write("(" + currentValue + ")");
                    currentValue++;
                }
            }
        }
        
        writer.write(";\n");
        
        // Add value field and constructor if needed
        if (hasValues) {
            writer.write("\n\tprivate final int value;\n\n");
            writer.write("\t" + className + "(int value) {\n");
            writer.write("\t\tthis.value = value;\n");
            writer.write("\t}\n\n");
            writer.write("\tpublic int getValue() {\n");
            writer.write("\t\treturn value;\n");
            writer.write("\t}\n");
        }
        
        writer.write("}\n");
    }
    
    private String getJavaClassName(EnumSchema schema) {
        if (schema.getJava_name() != null) {
            return schema.getJava_name();
        }
        return schema.getName();
    }
    
    private String getJavaName(EnumSchema schema, EnumSchema.EnumValue value) {
        if (value.getJava_name() != null) {
            return value.getJava_name();
        }
        
        String prefix = schema.getJava_prefix() != null ? schema.getJava_prefix() : "";
        return prefix + value.getName();
    }
    
    private String toKebabCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
