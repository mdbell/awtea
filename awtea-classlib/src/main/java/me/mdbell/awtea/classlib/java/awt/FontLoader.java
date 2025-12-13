package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.font.TrueTypeFont;
import me.mdbell.awtea.util.FetchAPI;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles loading fonts from external URLs via fetch (for browser environments)
 * or from resources (for JVM/test environments).
 * 
 * This loader caches loaded fonts to avoid repeated network requests and
 * leverages browser HTTP caching when available.
 */
public final class FontLoader {

	private static final String DEFAULT_FONT_BASE_URL = "fonts/";
	
	private static String fontBaseUrl = DEFAULT_FONT_BASE_URL;
	
	// Cache for loaded font data (byte arrays) - thread-safe for concurrent access
	private static final Map<String, byte[]> fontCache = new java.util.concurrent.ConcurrentHashMap<>();
	
	// Cache for parsed TrueTypeFont objects - thread-safe for concurrent access
	private static final Map<String, TrueTypeFont> parsedFontCache = new java.util.concurrent.ConcurrentHashMap<>();

	private FontLoader() {}

	/**
	 * Sets the base URL for loading fonts.
	 * This should be set to the location where fonts are hosted (e.g., CDN or server path).
	 * 
	 * @param baseUrl the base URL (should end with /)
	 */
	public static void setFontBaseUrl(String baseUrl) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			fontBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		}
	}

	/**
	 * Gets the current font base URL.
	 * 
	 * @return the font base URL
	 */
	public static String getFontBaseUrl() {
		return fontBaseUrl;
	}

	/**
	 * Clears the font cache.
	 */
	public static void clearCache() {
		fontCache.clear();
		parsedFontCache.clear();
	}

	/**
	 * Loads a TrueTypeFont for the given font name.
	 * Caches parsed fonts to avoid repeated parsing.
	 * 
	 * @param fontName the font name (without .ttf extension)
	 * @return the parsed TrueTypeFont
	 * @throws IOException if the font cannot be loaded
	 */
	public static TrueTypeFont loadFont(String fontName) throws IOException {
		// Check parsed font cache first
		TrueTypeFont cached = parsedFontCache.get(fontName);
		if (cached != null) {
			return cached;
		}

		// Load and parse font
		byte[] data = loadFontBytes(fontName);
		TrueTypeFont font = TrueTypeFont.read(data);

		// Cache parsed font
		parsedFontCache.put(fontName, font);

		return font;
	}

	/**
	 * Loads font bytes for the given font name.
	 * In browser environments, fonts are fetched from the configured URL.
	 * Falls back to resource loading if fetch fails.
	 * 
	 * @param fontName the font name (without .ttf extension)
	 * @return the font data as a byte array
	 * @throws IOException if the font cannot be loaded
	 */
	public static byte[] loadFontBytes(String fontName) throws IOException {
		// Check cache first
		byte[] cached = fontCache.get(fontName);
		if (cached != null) {
			return cached;
		}

		// Try to load via fetch (browser)
		String url = fontBaseUrl + fontName + ".ttf";

		try {
			FetchAPI.Response response = FetchAPI.fetch(url).await();
			
			if (!response.isOk()) {
				// If fetch fails, try loading from resources as fallback
				System.err.println("Fetch returned non-OK status for font " + fontName + " (HTTP " + response.getStatus() + "), trying resource fallback");
				return loadFontBytesWithFallback(fontName);
			}
			
			// Get the arrayBuffer from response
			ArrayBuffer buffer = response.arrayBuffer().await();
			
			// Convert ArrayBuffer to byte[]
			byte[] bytes = arrayBufferToByteArray(buffer);
			fontCache.put(fontName, bytes);
			return bytes;
			
		} catch (Exception e) {
			// If fetch fails (e.g., network error), try loading from resources
			System.err.println("Fetch failed for font " + fontName + ", trying resource fallback: " + e.getMessage());
			return loadFontBytesWithFallback(fontName);
		}
	}

	/**
	 * Fallback method to load font from resources when fetch fails.
	 * 
	 * @param fontName the font name
	 * @return the font data
	 * @throws IOException if the font cannot be loaded from resources either
	 */
	private static byte[] loadFontBytesWithFallback(String fontName) throws IOException {
		byte[] data = loadFontBytesFromResource(fontName);
		fontCache.put(fontName, data);
		return data;
	}

	/**
	 * Loads font bytes from a resource (for JVM/test environments or fallback).
	 * 
	 * @param fontName the font name
	 * @return the font data
	 * @throws IOException if the font cannot be loaded
	 */
	private static byte[] loadFontBytesFromResource(String fontName) throws IOException {
		String path = "/fonts/" + fontName + ".ttf";
		try (InputStream in = FontLoader.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IOException("Font not found in resources: " + fontName);
			}
			return in.readAllBytes();
		}
	}

	/**
	 * Converts an ArrayBuffer to a byte array.
	 * 
	 * @param buffer the ArrayBuffer
	 * @return the byte array
	 */
	private static byte[] arrayBufferToByteArray(ArrayBuffer buffer) {
		Int8Array int8Array = Int8Array.create(buffer);
		byte[] bytes = new byte[int8Array.getLength()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = int8Array.get(i);
		}
		return bytes;
	}
}
