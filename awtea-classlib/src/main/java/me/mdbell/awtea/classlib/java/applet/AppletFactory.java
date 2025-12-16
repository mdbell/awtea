package me.mdbell.awtea.classlib.java.applet;

import java.applet.Applet;

/**
 * Factory interface for creating applet instances.
 * This provides a compile-time safe way to instantiate applets without reflection.
 * 
 * <p>Example usage:
 * <pre>
 * public class MyAppletFactory implements AppletFactory {
 *     public Applet createApplet() {
 *         return new MyApplet();
 *     }
 * }
 * </pre>
 * 
 * @see AppletRegistry
 * @see AppletLauncher
 */
@FunctionalInterface
public interface AppletFactory {
    /**
     * Creates a new instance of an applet.
     * 
     * @return a new Applet instance
     */
    Applet createApplet();
}
