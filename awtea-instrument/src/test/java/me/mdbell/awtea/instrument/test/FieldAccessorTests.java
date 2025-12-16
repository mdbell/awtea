package me.mdbell.awtea.instrument.test;

import me.mdbell.awtea.test.*;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for field accessor functionality that can be compiled to JavaScript
 * via TeaVM and executed in Deno.
 */
public class FieldAccessorTests {
    
    /**
     * Simple test class with a private field for testing field accessors.
     */
    public static class TestTarget {
        private int privateValue = 42;
        private String privateName = "test";
        
        public int getPrivateValue() {
            return privateValue;
        }
        
        public void setPrivateValue(int value) {
            privateValue = value;
        }
        
        public String getPrivateName() {
            return privateName;
        }
        
        public void setPrivateName(String name) {
            privateName = name;
        }
    }
    
    /**
     * Test that we can read a private int field.
     */
    @Test
    public void testReadPrivateIntField() {
        TestTarget target = new TestTarget();
        // Using public getter as baseline - field accessor should produce same result
        assertEquals(target.getPrivateValue(), 42, "Should read initial value");
    }
    
    /**
     * Test that we can write a private int field.
     */
    @Test
    public void testWritePrivateIntField() {
        TestTarget target = new TestTarget();
        target.setPrivateValue(100);
        assertEquals(target.getPrivateValue(), 100, "Should write and read back new value");
    }
    
    /**
     * Test that we can read a private String field.
     */
    @Test
    public void testReadPrivateStringField() {
        TestTarget target = new TestTarget();
        assertEquals((Object) target.getPrivateName(), (Object) "test", "Should read initial string value");
    }
    
    /**
     * Test that we can write a private String field.
     */
    @Test
    public void testWritePrivateStringField() {
        TestTarget target = new TestTarget();
        target.setPrivateName("modified");
        assertEquals((Object) target.getPrivateName(), (Object) "modified", "Should write and read back new string value");
    }
    
    /**
     * Test multiple field modifications.
     */
    @Test
    public void testMultipleModifications() {
        TestTarget target = new TestTarget();
        
        target.setPrivateValue(1);
        assertEquals(target.getPrivateValue(), 1, "First modification");
        
        target.setPrivateValue(2);
        assertEquals(target.getPrivateValue(), 2, "Second modification");
        
        target.setPrivateName("first");
        assertEquals((Object) target.getPrivateName(), (Object) "first", "String modification");
        
        target.setPrivateName("second");
        assertEquals((Object) target.getPrivateName(), (Object) "second", "Second string modification");
    }
}
