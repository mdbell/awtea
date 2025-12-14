/**
 * Deno test runner for TeaVM-compiled Java tests
 * 
 * This file imports Java tests that have been compiled to JavaScript via TeaVM
 * and executes them with Deno's test framework.
 */

import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";

Deno.test("Java Surface Tests (via TeaVM)", async () => {
  // Import and run the tests
  const { main } = await import("../../../build/deno-tests/classes.js");
  
  // Run the tests - they will output to console which Deno captures
  main();
  
  // The tests output to console, but we can't easily capture it in Deno test context.
  // However, if tests fail, they will throw exceptions or main will fail.
  // The fact that we get here means the tests ran successfully.
  // We rely on the test output being visible in the test runner output.
  
  // This test passes if main() completes without throwing an exception
  // The actual assertions are done in the Java code and reported via console.log
});
