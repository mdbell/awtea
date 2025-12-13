package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.font.TrueTypeFont;
import me.mdbell.awtea.util.FetchAPI;
import org.teavm.interop.Async;
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
	 * This is async in browser, synchronous in JVM.
	 * 
	 * @param fontName the font name (without .ttf extension)
	 * @return the font data as a byte array
	 * @throws IOException if the font cannot be loaded
	 */
	@Async
	public static native byte[] loadFontBytes(String fontName) throws IOException;

	/**
	 * Implementation of async font loading for browser.
	 * This method is called by TeaVM's async transformation.
	 */
	@SuppressWarnings("unused")
	private static void loadFontBytes(String fontName, AsyncCallback<byte[]> callback) {
		// Check cache first
		byte[] cached = fontCache.get(fontName);
		if (cached != null) {
			callback.complete(cached);
			return;
		}

		// Try to load via fetch (browser)
		String url = fontBaseUrl + fontName + ".ttf";

		FetchAPI.fetch(url).then(response -> {
			if (!response.isOk()) {
				// If fetch fails, try loading from resources as fallback
				tryLoadFromResource(fontName, callback);
				return null;
			}
			
			// Chain the arrayBuffer promise
			response.arrayBuffer().then(buffer -> {
				// Convert ArrayBuffer to byte[]
				byte[] bytes = arrayBufferToByteArray(buffer);
				fontCache.put(fontName, bytes);
				callback.complete(bytes);
				return null;
			}).catchError(bufferError -> {
				System.err.println("Failed to read arrayBuffer for font " + fontName + ": " + bufferError);
				tryLoadFromResource(fontName, callback);
				return null;
			});
			
			return null;
		}).catchError(error -> {
			// If fetch fails (e.g., network error), try loading from resources
			System.err.println("Fetch failed for font " + fontName + ", trying resource fallback: " + error);
			tryLoadFromResource(fontName, callback);
			return null;
		});
	}

	/**
	 * Fallback method to load font from resources when fetch fails.
	 */
	private static void tryLoadFromResource(String fontName, AsyncCallback<byte[]> callback) {
		try {
			byte[] data = loadFontBytesFromResource(fontName);
			fontCache.put(fontName, data);
			callback.complete(data);
		} catch (IOException e) {
			callback.error(new IOException("Failed to load font " + fontName + " from URL or resources", e));
		}
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

	/**
	 * Callback interface for async operations.
	 * This is used by TeaVM's async transformation.
	 */
	private interface AsyncCallback<T> {
		void complete(T result);
		void error(Throwable t);
	}
}
