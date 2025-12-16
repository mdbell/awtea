package me.mdbell.awtea.examples.guidemo;

import me.mdbell.awtea.classlib.java.applet.AppletLauncher;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Main entry point for the GUI Demo example using the applet launcher.
 * This class is referenced by TeaVM and exports functions as ES2015 modules.
 */
public class GuiDemoLauncher {
    
    public static void main(String[] args) {
        // Configure logging
        LoggerFactory.setGlobalLevel(LogLevel.TRACE);
        
        // System properties should be set from JavaScript using setProperty()
        // This allows runtime configuration without recompiling
        
        // Launch applets using the automatic discovery mechanism
        AppletLauncher.main(args);
    }
}
