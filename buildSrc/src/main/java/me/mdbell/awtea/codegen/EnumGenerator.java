package me.mdbell.awtea.codegen;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Main entry point for enum code generation
 */
public class EnumGenerator {
    
    public static void main(String[] args) {
        if (args.length < 4 || args.length % 2 != 0) {
            System.err.println("Usage: EnumGenerator --schemas <dir> --output-c <dir> --output-java <dir> --output-ts <dir>");
            System.exit(1);
        }
        
        Path schemasDir = null;
        Path outputCDir = null;
        Path outputJavaDir = null;
        Path outputTsDir = null;
        
        for (int i = 0; i < args.length; i += 2) {
            String flag = args[i];
            String value = args[i + 1];
            
            switch (flag) {
                case "--schemas":
                    schemasDir = Paths.get(value);
                    break;
                case "--output-c":
                    outputCDir = Paths.get(value);
                    break;
                case "--output-java":
                    outputJavaDir = Paths.get(value);
                    break;
                case "--output-ts":
                    outputTsDir = Paths.get(value);
                    break;
                default:
                    System.err.println("Unknown flag: " + flag);
                    System.exit(1);
            }
        }
        
        if (schemasDir == null || outputCDir == null || outputJavaDir == null || outputTsDir == null) {
            System.err.println("Missing required arguments");
            System.exit(1);
        }
        
        try {
            new EnumGenerator().generate(schemasDir, outputCDir, outputJavaDir, outputTsDir);
        } catch (Exception e) {
            System.err.println("Error generating enums: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void generate(Path schemasDir, Path outputCDir, Path outputJavaDir, Path outputTsDir) throws IOException {
        System.out.println("Generating enums from schemas in: " + schemasDir);
        
        List<EnumSchema> schemas = loadSchemas(schemasDir);
        
        if (schemas.isEmpty()) {
            System.out.println("No schema files found in " + schemasDir);
            return;
        }
        
        CHeaderGenerator cGenerator = new CHeaderGenerator();
        JavaEnumGenerator javaGenerator = new JavaEnumGenerator("me.mdbell.awtea.gfx.generated");
        TypeScriptEnumGenerator tsGenerator = new TypeScriptEnumGenerator();
        
        for (EnumSchema schema : schemas) {
            System.out.println("Processing schema: " + schema.getName());
            
            cGenerator.generate(schema, outputCDir);
            javaGenerator.generate(schema, outputJavaDir);
            tsGenerator.generate(schema, outputTsDir);
        }
        
        System.out.println("Code generation complete!");
    }
    
    private List<EnumSchema> loadSchemas(Path schemasDir) throws IOException {
        List<EnumSchema> schemas = new ArrayList<>();
        LoaderOptions loaderOptions = new LoaderOptions();
        Constructor constructor = new Constructor(EnumSchema.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);
        
        if (!Files.exists(schemasDir) || !Files.isDirectory(schemasDir)) {
            throw new IOException("Schemas directory does not exist: " + schemasDir);
        }
        
        try (Stream<Path> paths = Files.walk(schemasDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                 .forEach(path -> {
                     try (InputStream input = Files.newInputStream(path)) {
                         EnumSchema schema = yaml.load(input);
                         schemas.add(schema);
                         System.out.println("Loaded schema: " + path.getFileName());
                     } catch (IOException e) {
                         throw new RuntimeException("Failed to load schema: " + path, e);
                     }
                 });
        }
        
        return schemas;
    }
}
