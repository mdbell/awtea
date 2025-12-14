package me.mdbell.awtea.codegen;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates C header files from enum schemas
 */
public class CHeaderGenerator {
    
    public void generate(EnumSchema schema, Path outputDir) throws IOException {
        String filename = toSnakeCase(schema.getName()) + ".h";
        Path outputFile = outputDir.resolve(filename);
        
        Files.createDirectories(outputDir);
        
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writeHeader(writer, schema);
            writeEnum(writer, schema);
        }
        
        System.out.println("Generated C header: " + outputFile);
    }
    
    private void writeHeader(Writer writer, EnumSchema schema) throws IOException {
        writer.write("/**\n");
        writer.write(" * AUTO-GENERATED FILE - DO NOT EDIT\n");
        writer.write(" * Generated from: schemas/" + toKebabCase(schema.getName()) + ".yaml\n");
        writer.write(" * \n");
        writer.write(" * " + schema.getDescription() + "\n");
        writer.write(" */\n");
        writer.write("#pragma once\n\n");
        writer.write("#include <stdint.h>\n\n");
    }
    
    private void writeEnum(Writer writer, EnumSchema schema) throws IOException {
        writer.write("typedef enum {\n");
        
        int currentValue = 0;
        for (EnumSchema.EnumValue value : schema.getValues()) {
            if (value.getJava_only() || value.getTs_only()) {
                continue;
            }
            
            String cName = getCName(schema, value);
            
            // Write comment if description exists
            if (value.getDescription() != null && !value.getDescription().isEmpty()) {
                writer.write("    // " + value.getDescription() + "\n");
            }
            
            writer.write("    " + cName);
            
            // Write explicit value if specified
            if (value.getValue() != null) {
                writer.write(" = " + value.getValue());
                currentValue = value.getValue() + 1;
            } else {
                currentValue++;
            }
            
            writer.write(",\n");
            
            // Add blank line after this value if it has a custom value (indicates grouping)
            if (value.getValue() != null && schema.getValues().indexOf(value) < schema.getValues().size() - 1) {
                writer.write("\n");
            }
        }
        
        writer.write("} " + schema.getName() + ";\n");
    }
    
    private String getCName(EnumSchema schema, EnumSchema.EnumValue value) {
        if (value.getC_name() != null) {
            return value.getC_name();
        }
        
        String prefix = schema.getC_prefix() != null ? schema.getC_prefix() : "";
        return prefix + value.getName();
    }
    
    private String toSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    private String toKebabCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
