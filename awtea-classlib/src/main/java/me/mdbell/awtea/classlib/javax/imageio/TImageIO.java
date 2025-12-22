package me.mdbell.awtea.classlib.javax.imageio;

import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Browser-compatible stub for javax.imageio.ImageIO.
 * Provides basic image reading functionality using browser image loading.
 * Writing images is not supported in browser environments.
 * 
 * @see javax.imageio.ImageIO
 */
public class TImageIO {
    
    private static final Logger log = LoggerFactory.getLogger(TImageIO.class);
    
    /**
     * Returns a BufferedImage as the result of decoding a supplied File.
     * 
     * @param input a File to read from
     * @return a BufferedImage containing the decoded contents, or null
     * @throws IOException if an error occurs during reading
     */
    public static TBufferedImage read(File input) throws IOException {
        log.warn("ImageIO.read(File) called - limited support in browser environment");
        throw new UnsupportedOperationException("ImageIO.read(File) is not fully supported in browser environment. Use Image loading via URL or InputStream instead.");
    }
    
    /**
     * Returns a BufferedImage as the result of decoding a supplied InputStream.
     * 
     * @param input an InputStream to read from
     * @return a BufferedImage containing the decoded contents, or null
     * @throws IOException if an error occurs during reading
     */
    public static TBufferedImage read(InputStream input) throws IOException {
        log.warn("ImageIO.read(InputStream) called - limited support in browser environment");
        // Could potentially implement using browser Image API with data URLs
        throw new UnsupportedOperationException("ImageIO.read(InputStream) is not fully supported in browser environment. Use TImage or URL-based loading instead.");
    }
    
    /**
     * Returns a BufferedImage as the result of decoding a supplied URL.
     * 
     * @param input a URL to read from
     * @return a BufferedImage containing the decoded contents, or null
     * @throws IOException if an error occurs during reading
     */
    public static TBufferedImage read(URL input) throws IOException {
        log.warn("ImageIO.read(URL) called - limited support in browser environment");
        // Could potentially implement using browser Image API
        throw new UnsupportedOperationException("ImageIO.read(URL) is not fully supported in browser environment. Use TImage or browser image loading instead.");
    }
    
    /**
     * Writes an image using an arbitrary ImageWriter.
     * 
     * @param im a RenderedImage to be written
     * @param formatName a String containing the informal name of the format
     * @param output a File to be written to
     * @return false if no appropriate writer is found
     * @throws IOException if an error occurs during writing
     */
    public static boolean write(Object im, String formatName, File output) throws IOException {
        log.warn("ImageIO.write() called - not supported in browser environment (file system write access restricted)");
        throw new UnsupportedOperationException("ImageIO.write() is not supported in browser environment. Browser applications cannot write arbitrary files to the local filesystem.");
    }
}
