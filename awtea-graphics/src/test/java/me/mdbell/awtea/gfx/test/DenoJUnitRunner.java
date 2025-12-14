package me.mdbell.awtea.gfx.test;

/**
 * Main class that runs Java tests and outputs results for Deno consumption.
 * This is the entry point when compiled to JavaScript via TeaVM.
 * 
 * Since JUnitCore doesn't work well with TeaVM, we manually invoke test methods
 * and report results.
 */
public class DenoJUnitRunner {
    
    public static void main(String[] args) {
        System.out.println("Starting tests via TeaVM...");
        
        SurfaceTests tests = new SurfaceTests();
        int passed = 0;
        int failed = 0;
        
        // Run each test method manually
        passed += runTest("testPixelFormatConstants", () -> tests.testPixelFormatConstants());
        passed += runTest("testPixelFormatRange", () -> tests.testPixelFormatRange());
        passed += runTest("testPixelFormatValidation", () -> tests.testPixelFormatValidation());
        passed += runTest("testEnumSequentialValues", () -> tests.testEnumSequentialValues());
        passed += runTest("testFormatRangeContinuous", () -> tests.testFormatRangeContinuous());
        
        failed = 5 - passed;
        
        // Output summary
        System.out.println("\n========== Test Results ==========");
        System.out.println("Tests run: 5");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Success: " + (failed == 0));
        System.out.println("========== End Test Results ==========");
    }
    
    private static int runTest(String name, Runnable test) {
        try {
            System.out.println("\nRunning: " + name);
            test.run();
            System.out.println("✓ PASSED: " + name);
            return 1;
        } catch (AssertionError e) {
            System.out.println("✗ FAILED: " + name);
            System.out.println("  Error: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            System.out.println("✗ ERROR: " + name);
            System.out.println("  Exception: " + e.getMessage());
            return 0;
        }
    }
    
    @FunctionalInterface
    interface Runnable {
        void run() throws Exception;
    }
}
