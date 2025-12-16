package me.mdbell.awtea.examples.helloworld;

import me.mdbell.awtea.classlib.java.applet.AppletLauncher;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Main entry point for the Hello World example using the applet launcher.
 * This class is referenced by TeaVM and exports functions as ES2015 modules.
 */
public class HelloWorldLauncher {
    
    public static void main(String[] args) {
        // Configure logging
        LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        
        // Configure graphics backend
        System.setProperty("me.mdbell.awtea.gfx.backend", "java");
        
        // Ensure HelloWorldApplet is loaded and registered
        // This triggers the static block in HelloWorldApplet
        try {
            Class.forName("me.mdbell.awtea.examples.helloworld.HelloWorldApplet");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load HelloWorldApplet", e);
        }
        
        // Launch applets using the automatic discovery mechanism
        AppletLauncher.main(args);
    }
}
