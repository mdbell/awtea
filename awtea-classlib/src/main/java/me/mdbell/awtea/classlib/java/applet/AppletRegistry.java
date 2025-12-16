package me.mdbell.awtea.classlib.java.applet;

import java.applet.Applet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for applet factories that allows compile-time registration of applets.
 * This avoids the need for runtime reflection or dynamic class loading, which is
 * not supported in TeaVM.
 * 
 * <p>Applets are registered by name and can be instantiated later by that name.
 * This enables low-code launching from HTML and JavaScript.
 * 
 * <p>Example registration:
 * <pre>
 * public class MyApp {
 *     static {
 *         AppletRegistry.register("my-applet", MyApplet::new);
 *     }
 * }
 * </pre>
 * 
 * <p>Example HTML usage:
 * <pre>
 * &lt;canvas id="app" data-awtea-applet="my-applet"&gt;&lt;/canvas&gt;
 * </pre>
 * 
 * @see AppletFactory
 * @see AppletLauncher
 */
public final class AppletRegistry {
    
    private static final Map<String, AppletFactory> factories = new HashMap<>();
    
    /**
     * Private constructor to prevent instantiation.
     */
    private AppletRegistry() {
        throw new UnsupportedOperationException("AppletRegistry cannot be instantiated");
    }
    
    /**
     * Registers an applet factory with the given name.
     * 
     * @param name the unique name for this applet (typically lowercase-with-dashes)
     * @param factory the factory that creates instances of the applet
     * @throws IllegalArgumentException if name is null or empty
     * @throws IllegalStateException if an applet with this name is already registered
     */
    public static void register(String name, AppletFactory factory) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Applet name cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Applet factory cannot be null");
        }
        if (factories.containsKey(name)) {
            throw new IllegalStateException("Applet already registered with name: " + name);
        }
        factories.put(name, factory);
    }
    
    /**
     * Creates a new instance of the applet with the given name.
     * 
     * @param name the name of the applet to create
     * @return a new instance of the applet
     * @throws IllegalArgumentException if no applet is registered with the given name
     */
    public static Applet createApplet(String name) {
        AppletFactory factory = factories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("No applet registered with name: " + name + 
                ". Available applets: " + String.join(", ", factories.keySet()));
        }
        return factory.createApplet();
    }
    
    /**
     * Checks if an applet with the given name is registered.
     * 
     * @param name the name to check
     * @return true if an applet with this name is registered, false otherwise
     */
    public static boolean isRegistered(String name) {
        return factories.containsKey(name);
    }
    
    /**
     * Gets the set of all registered applet names.
     * 
     * @return an unmodifiable set of registered applet names
     */
    public static Set<String> getRegisteredNames() {
        return java.util.Collections.unmodifiableSet(factories.keySet());
    }
    
    /**
     * Clears all registered applets. This is primarily useful for testing.
     */
    public static void clear() {
        factories.clear();
    }
}
