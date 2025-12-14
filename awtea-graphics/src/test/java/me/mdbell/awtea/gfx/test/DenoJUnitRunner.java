package me.mdbell.awtea.gfx.test;

/**
 * Main class that registers Java tests with Deno's test framework.
 * This is the entry point when compiled to JavaScript via TeaVM.
 * 
 * Uses the Deno JSO wrapper to register tests directly with Deno.test(),
 * providing proper 1-1 mapping with Deno's test infrastructure.
 */
public class DenoJUnitRunner {
    
    public static void main(String[] args) {
        Deno.DenoAPI deno = Deno.getInstance();
        SurfaceTests tests = new SurfaceTests();
        
        // Register each test with Deno's test framework
        deno.test("Java: Pixel format constants", () -> tests.testPixelFormatConstants());
        deno.test("Java: Pixel format range", () -> tests.testPixelFormatRange());
        deno.test("Java: Pixel format validation", () -> tests.testPixelFormatValidation());
        deno.test("Java: Enum sequential values", () -> tests.testEnumSequentialValues());
        deno.test("Java: Format range continuous", () -> tests.testFormatRangeContinuous());
    }
}
