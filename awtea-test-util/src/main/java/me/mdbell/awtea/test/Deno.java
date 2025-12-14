package me.mdbell.awtea.test;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * JavaScript wrapper for Deno's test API.
 * Provides access to Deno.test() from Java code.
 */
public final class Deno {
    
    private Deno() {}
    
    /**
     * Gets the Deno global object.
     */
    @JSBody(script = "return Deno;")
    public static native DenoAPI getInstance();
    
    /**
     * Interface representing the Deno global object.
     */
    public interface DenoAPI extends JSObject {
        /**
         * Register a test with Deno's test framework.
         * @param name Test name
         * @param fn Test function
         */
        void test(String name, TestFunction fn);
    }
    
    /**
     * Functional interface for test functions.
     */
    @JSFunctor
    @FunctionalInterface
    public interface TestFunction extends JSObject {
        void run();
    }
}
