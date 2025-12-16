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
        
        // System properties should be set from JavaScript using setProperty()
        // This allows runtime configuration without recompiling
        
        // Launch applets using the automatic discovery mechanism
        AppletLauncher.main(args);
    }
}
