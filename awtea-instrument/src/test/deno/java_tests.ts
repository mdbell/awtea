/**
 * Deno test runner for TeaVM-compiled Java field accessor tests
 * 
 * This file imports Java tests that have been compiled to JavaScript via TeaVM.
 * The Java code directly calls Deno.test() via JSO wrappers, so tests are
 * automatically registered when main() is called.
 */

import { main } from "../../../build/deno-tests/classes.js";

// Call main() to register all Java tests with Deno
main();
