package me.mdbell.awtea.classlib.java.net;

import java.io.InterruptedIOException;

/**
 * @see java.net.SocketTimeoutException
 */
public class TSocketTimeoutException extends InterruptedIOException {

	public TSocketTimeoutException() {
		super();
	}

	public TSocketTimeoutException(String message) {
		super(message);
	}
}
