package me.mdbell.awtea.classlib.java.net;

import java.io.IOException;

/**
 * @see java.net.SocketException
 */
public class TSocketException extends IOException {

	public TSocketException() {
		super();
	}

	public TSocketException(String message) {
		super(message);
	}
}
