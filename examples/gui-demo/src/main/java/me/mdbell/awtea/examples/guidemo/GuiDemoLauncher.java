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
        
        // Configure graphics backend and font settings
        System.setProperty("me.mdbell.awtea.wasm.module_path", "/awtea-graphics/build/wasm/awt_raster.wasm");
        System.setProperty("me.mdbell.awtea.font.supersample", "4");
        
        // Ensure GuiDemoApplet is loaded and registered
        // This triggers the static block in GuiDemoApplet
        try {
            Class.forName("me.mdbell.awtea.examples.guidemo.GuiDemoApplet");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load GuiDemoApplet", e);
        }
        
        // Launch applets using the automatic discovery mechanism
        AppletLauncher.main(args);
    }
}
