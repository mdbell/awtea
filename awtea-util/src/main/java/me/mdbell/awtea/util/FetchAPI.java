package me.mdbell.awtea.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * Utility class for making fetch requests in the browser.
 * Provides access to the Fetch API for loading external resources.
 */
public final class FetchAPI {

	private FetchAPI() {}

	/**
	 * Fetch a resource from the given URL and return a promise that resolves to the Response.
	 * 
	 * @param url the URL to fetch
	 * @return a promise that resolves to the Response object
	 */
	@JSBody(params = {"url"}, script = "return fetch(url);")
	public static native JSPromise<Response> fetch(String url);

	/**
	 * Represents a fetch Response object.
	 */
	public interface Response extends JSObject {
		/**
		 * Returns a promise that resolves with the response body as an ArrayBuffer.
		 * 
		 * @return a promise resolving to an ArrayBuffer
		 */
		@JSBody(script = "return this.arrayBuffer();")
		JSPromise<ArrayBuffer> arrayBuffer();

		/**
		 * Returns the status code of the response.
		 * 
		 * @return the HTTP status code
		 */
		@JSBody(script = "return this.status;")
		int getStatus();

		/**
		 * Returns whether the response was successful (status in range 200-299).
		 * 
		 * @return true if the response was successful
		 */
		@JSBody(script = "return this.ok;")
		boolean isOk();
	}
}
