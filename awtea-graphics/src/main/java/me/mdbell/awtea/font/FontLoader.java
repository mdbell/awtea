package me.mdbell.awtea.font;

import me.mdbell.awtea.util.FetchAPI;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles loading fonts from external URLs via fetch (for browser environments)
 * or from resources (for JVM/test environments).
 * <p>
 * This loader caches loaded fonts to avoid repeated network requests and
 * leverages browser HTTP caching when available. It also caches font loading
 * failures to prevent repeated performance hits from retrying failed loads.
 */
public final class FontLoader {

	private static final Logger log = LoggerFactory.getLogger(FontLoader.class);

	private static final String FONT_BASE_URL = getFontBaseUrlFromProperty();

	// Cache for loaded font data (byte arrays) - thread-safe for concurrent access
	private static final Map<String, byte[]> fontCache = new ConcurrentHashMap<>();

	// Cache for parsed TrueTypeFont objects - thread-safe for concurrent access
	private static final Map<String, TrueTypeFont> parsedFontCache = new ConcurrentHashMap<>();

	// Cache for failed font load attempts to avoid repeated failures - thread-safe for concurrent access
	// Using a ConcurrentHashMap with dummy values as a thread-safe set
	private static final Map<String, Boolean> failureCache = new ConcurrentHashMap<>();

	private FontLoader() {
	}

	/**
	 * Gets the font base URL from the system property.
	 * The system property "me.mdbell.awtea.font.base_url" can be used to configure
	 * the base URL for loading fonts. If not set, defaults to "fonts/".
	 *
	 * @return the configured font base URL
	 */
	private static String getFontBaseUrlFromProperty() {
		String baseUrl = System.getProperty("me.mdbell.awtea.font.base_url", "fonts/");
		// Ensure URL ends with /
		if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}
		return baseUrl;
	}

	/**
	 * Gets the current font base URL.
	 *
	 * @return the font base URL
	 */
	public static String getFontBaseUrl() {
		return FONT_BASE_URL;
	}

	/**
	 * Clears the font cache (both success and failure caches).
	 */
	public static void clearCache() {
		fontCache.clear();
		parsedFontCache.clear();
		failureCache.clear();
	}

	/**
	 * Clears only the failure cache.
	 * Useful for development/debugging when you want to retry previously failed loads.
	 */
	public static void clearFailureCache() {
		failureCache.clear();
	}

	/**
	 * Checks if a font name is in the failure cache.
	 * 
	 * @param fontName the font name to check
	 * @return true if this font has previously failed to load
	 */
	public static boolean isFailureCached(String fontName) {
		return failureCache.containsKey(fontName);
	}

	/**
	 * Loads a TrueTypeFont for the given font name.
	 * Caches parsed fonts to avoid repeated parsing.
	 * Also caches failures to avoid repeated load attempts.
	 *
	 * @param fontName the font name (without .ttf extension)
	 * @return the parsed TrueTypeFont
	 * @throws IOException if the font cannot be loaded
	 */
	public static TrueTypeFont loadFont(String fontName) throws IOException {
		// Check failure cache first - fail fast if we know this font doesn't exist
		if (failureCache.containsKey(fontName)) {
			log.debug("Font '{}' is in failure cache, not retrying", fontName);
			throw new IOException("Font previously failed to load: " + fontName);
		}

		// Check parsed font cache
		TrueTypeFont cached = parsedFontCache.get(fontName);
		if (cached != null) {
			log.debug("Font '{}' loaded from parsed cache", fontName);
			return cached;
		}

		log.debug("Loading font '{}'", fontName);

		try {
			// Load and parse font
			byte[] data = loadFontBytes(fontName);
			TrueTypeFont font = TrueTypeFont.read(data);

			// Cache parsed font
			parsedFontCache.put(fontName, font);

			return font;
		} catch (IOException e) {
			// Cache the failure to avoid repeated attempts
			failureCache.put(fontName, Boolean.TRUE);
			log.debug("Font '{}' failed to load, added to failure cache", fontName);
			throw e;
		}
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
		String url = FONT_BASE_URL + fontName + ".ttf";

		try {
			FetchAPI.Response response = FetchAPI.fetch(url).await();

			if (!response.isOk()) {
				// If fetch fails, try loading from resources as fallback
				log.warn("Fetch failed for font '{}': HTTP {}, trying resource fallback ", fontName, response.getStatus());
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
			log.warn("Fetch exception for font '{}': {}, trying resource fallback", fontName, e.getMessage());
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
