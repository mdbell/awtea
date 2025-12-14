package me.mdbell.awtea.codegen;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates TypeScript enum files from enum schemas
 */
public class TypeScriptEnumGenerator {
    
    public void generate(EnumSchema schema, Path outputDir) throws IOException {
        String filename = toKebabCase(schema.getName()) + ".ts";
        Path outputFile = outputDir.resolve(filename);
        
        Files.createDirectories(outputDir);
        
        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            writeHeader(writer, schema);
            writeEnum(writer, schema);
        }
        
        System.out.println("Generated TypeScript file: " + outputFile);
    }
    
    private void writeHeader(Writer writer, EnumSchema schema) throws IOException {
        writer.write("/**\n");
        writer.write(" * AUTO-GENERATED FILE - DO NOT EDIT\n");
        writer.write(" * Generated from: schemas/" + toKebabCase(schema.getName()) + ".yaml\n");
        writer.write(" * \n");
        writer.write(" * " + schema.getDescription() + "\n");
        writer.write(" */\n\n");
    }
    
    private void writeEnum(Writer writer, EnumSchema schema) throws IOException {
        writer.write("export enum " + schema.getName() + " {\n");
        
        int currentValue = 0;
        for (EnumSchema.EnumValue value : schema.getValues()) {
            if (value.getC_only() || value.getJava_only()) {
                continue;
            }
            
            String tsName = getTsName(schema, value);
            
            // Write comment if description exists
            if (value.getDescription() != null && !value.getDescription().isEmpty()) {
                writer.write("  /** " + value.getDescription() + " */\n");
            }
            
            writer.write("  " + tsName + " = ");
            
            if (value.getValue() != null) {
                writer.write(value.getValue().toString());
                currentValue = value.getValue() + 1;
            } else {
                writer.write(String.valueOf(currentValue));
                currentValue++;
            }
            
            writer.write(",\n");
        }
        
        writer.write("}\n");
    }
    
    private String getTsName(EnumSchema schema, EnumSchema.EnumValue value) {
        if (value.getTs_name() != null) {
            return value.getTs_name();
        }
        
        String prefix = schema.getC_prefix() != null ? schema.getC_prefix() : "";
        return prefix + value.getName();
    }
    
    private String toKebabCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
