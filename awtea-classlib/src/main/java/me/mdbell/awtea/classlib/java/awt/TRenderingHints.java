package me.mdbell.awtea.classlib.java.awt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * TeaVM implementation of java.awt.RenderingHints.
 * The RenderingHints class defines and manages collections of keys and associated values which
 * allow an application to provide input into the choice of algorithms used by other classes
 * which perform rendering and image manipulation services.
 * 
 * @see java.awt.RenderingHints
 */
public class TRenderingHints implements Map<Object, Object>, Cloneable {
    
    /**
     * Defines the base type of all keys used to control rendering and image processing.
     */
    public abstract static class Key {
        private final int privateKey;
        
        protected Key(int privateKey) {
            this.privateKey = privateKey;
        }
        
        public abstract boolean isCompatibleValue(Object val);
        
        protected final int intKey() {
            return privateKey;
        }
        
        @Override
        public final int hashCode() {
            return System.identityHashCode(this);
        }
        
        @Override
        public final boolean equals(Object o) {
            return this == o;
        }
    }
    
    // Rendering hint keys
    public static final int KEY_ANTIALIASING = 1;
    public static final int KEY_RENDERING = 2;
    public static final int KEY_DITHERING = 3;
    public static final int KEY_TEXT_ANTIALIASING = 4;
    public static final int KEY_FRACTIONALMETRICS = 5;
    public static final int KEY_INTERPOLATION = 6;
    public static final int KEY_ALPHA_INTERPOLATION = 7;
    public static final int KEY_COLOR_RENDERING = 8;
    public static final int KEY_STROKE_CONTROL = 9;
    
    // Rendering hint values
    public static final Object VALUE_ANTIALIAS_ON = new Object();
    public static final Object VALUE_ANTIALIAS_OFF = new Object();
    public static final Object VALUE_ANTIALIAS_DEFAULT = new Object();
    
    public static final Object VALUE_RENDER_SPEED = new Object();
    public static final Object VALUE_RENDER_QUALITY = new Object();
    public static final Object VALUE_RENDER_DEFAULT = new Object();
    
    public static final Object VALUE_DITHER_ENABLE = new Object();
    public static final Object VALUE_DITHER_DISABLE = new Object();
    public static final Object VALUE_DITHER_DEFAULT = new Object();
    
    public static final Object VALUE_TEXT_ANTIALIAS_ON = new Object();
    public static final Object VALUE_TEXT_ANTIALIAS_OFF = new Object();
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = new Object();
    
    public static final Object VALUE_FRACTIONALMETRICS_ON = new Object();
    public static final Object VALUE_FRACTIONALMETRICS_OFF = new Object();
    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT = new Object();
    
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = new Object();
    public static final Object VALUE_INTERPOLATION_BILINEAR = new Object();
    public static final Object VALUE_INTERPOLATION_BICUBIC = new Object();
    
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED = new Object();
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY = new Object();
    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT = new Object();
    
    public static final Object VALUE_COLOR_RENDER_SPEED = new Object();
    public static final Object VALUE_COLOR_RENDER_QUALITY = new Object();
    public static final Object VALUE_COLOR_RENDER_DEFAULT = new Object();
    
    public static final Object VALUE_STROKE_DEFAULT = new Object();
    public static final Object VALUE_STROKE_NORMALIZE = new Object();
    public static final Object VALUE_STROKE_PURE = new Object();
    
    private final Map<Object, Object> hintMap = new HashMap<>();
    
    /**
     * Constructs a new object with keys and values initialized from the specified Map object.
     * 
     * @param init a map of key/value pairs to initialize the hints
     */
    public TRenderingHints(Map<?, ?> init) {
        if (init != null) {
            hintMap.putAll(init);
        }
    }
    
    /**
     * Constructs a new object with the specified key/value pair.
     * 
     * @param key the key of the particular hint property
     * @param value the value of the hint property specified with key
     */
    public TRenderingHints(Key key, Object value) {
        hintMap.put(key, value);
    }
    
    @Override
    public int size() {
        return hintMap.size();
    }
    
    @Override
    public boolean isEmpty() {
        return hintMap.isEmpty();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return hintMap.containsKey(key);
    }
    
    @Override
    public boolean containsValue(Object value) {
        return hintMap.containsValue(value);
    }
    
    @Override
    public Object get(Object key) {
        return hintMap.get(key);
    }
    
    @Override
    public Object put(Object key, Object value) {
        return hintMap.put(key, value);
    }
    
    @Override
    public Object remove(Object key) {
        return hintMap.remove(key);
    }
    
    @Override
    public void putAll(Map<?, ?> m) {
        hintMap.putAll(m);
    }
    
    @Override
    public void clear() {
        hintMap.clear();
    }
    
    @Override
    public Set<Object> keySet() {
        return hintMap.keySet();
    }
    
    @Override
    public java.util.Collection<Object> values() {
        return hintMap.values();
    }
    
    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return hintMap.entrySet();
    }
    
    @Override
    public TRenderingHints clone() {
        TRenderingHints copy = new TRenderingHints(null);
        copy.hintMap.putAll(this.hintMap);
        return copy;
    }
    
    @Override
    public String toString() {
        return "TRenderingHints" + hintMap.toString();
    }
}
