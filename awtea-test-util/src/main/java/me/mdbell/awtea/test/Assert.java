package me.mdbell.awtea.test;

/**
 * Simple assertion utilities for tests.
 * Throws AssertionError when assertions fail, which Deno's test framework will catch.
 */
public class Assert {
    
    /**
     * Make an assertion that actual and expected are equal.
     */
    public static void assertEquals(int actual, int expected, String msg) {
        if (actual != expected) {
            throw new AssertionError(msg + " - expected: " + expected + " but was: " + actual);
        }
    }
    
    /**
     * Make an assertion that actual and expected are equal (objects).
     */
    public static void assertEquals(Object actual, Object expected, String msg) {
        if (actual == null && expected == null) {
            return;
        }
        if (actual == null || !actual.equals(expected)) {
            throw new AssertionError(msg + " - expected: " + expected + " but was: " + actual);
        }
    }
    
    /**
     * Make an assertion that expr is true.
     */
    public static void assertTrue(boolean expr, String msg) {
        if (!expr) {
            throw new AssertionError(msg);
        }
    }
    
    /**
     * Make an assertion that expr is false.
     */
    public static void assertFalse(boolean expr, String msg) {
        if (expr) {
            throw new AssertionError(msg);
        }
    }
    
    /**
     * Make an assertion that obj is not null.
     */
    public static void assertNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new AssertionError(msg);
        }
    }
    
    /**
     * Make an assertion that obj is null.
     */
    public static void assertNull(Object obj, String msg) {
        if (obj != null) {
            throw new AssertionError(msg);
        }
    }
}
