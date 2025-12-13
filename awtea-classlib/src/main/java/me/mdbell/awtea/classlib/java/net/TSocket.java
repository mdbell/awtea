package me.mdbell.awtea.classlib.java.net;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.monitor.NetworkMonitor;
import me.mdbell.awtea.net.SocketResolver;
import me.mdbell.awtea.net.SocketResolverFactory;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.ThreadUtils;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.function.JSConsumer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.websocket.WebSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ExtensionMethod({JSObjectsExtensions.class})
public class TSocket {
	private final String host;
	private final int port;

	private final SocketResolver resolver;

	private final WebSocket socket;
	private final SocketInputStream inputStream;
	private final SocketOutputStream outputStream;

	private final List<Registration> registrations = new ArrayList<>();

	/**
	 * Socket read timeout in milliseconds.
	 * 0 <= no timeout
	 */
	@Getter
	private volatile int soTimeout;

	public TSocket(TInetAddress address, int port) throws TUnknownHostException {
		this.host = address.getHost();
		this.port = port;
		this.resolver = SocketResolverFactory.getInstance().createSocketResolver();

		this.socket = this.connect(host, port).await();

		this.inputStream = new SocketInputStream();
		this.outputStream = new SocketOutputStream();

		socket.onMessage(evt -> {
			Int8Array arr = new Int8Array(evt.getDataAsArray());
			inputStream.pushBuffer(arr);
			NetworkMonitor.get().onUpdateBufferSizes(this, inputStream.available(), 0);
		}).track(registrations);

		socket.onError(event -> {
			try {
				NetworkMonitor.get().onError(TSocket.this, event.toString());
				close();
			} catch (IOException ignored) {
			}
		}).track(registrations);
	}

	public void setSoTimeout(int timeout) throws TSocketException {
		this.soTimeout = timeout;
	}

	public void setTcpNoDelay(boolean on) throws TSocketException {
	}

	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}

	public void close() throws IOException {
		NetworkMonitor.get().onClosing(this);
		if (socket != null && socket.getReadyState() <= 1) {
			socket.close();
		}
		registrations.cleanup();
		inputStream.close();
		NetworkMonitor.get().onClosed(this);
	}

	private JSPromise<WebSocket> connect(String server, int port) {
		String url = this.resolver.resolveUrl(server, port);
		System.out.println("Connecting to " + server + ":" + port + "(" + url + ") via WebSocket");

		NetworkMonitor.get().register(this, host, port, url);
		WebSocket ws = new WebSocket(url, "binary");
		ws.setBinaryType("arraybuffer");

		return new JSPromise<WebSocket>((resolve, reject) -> {
			NetworkMonitor.get().onConnecting(TSocket.this);
			ws.onOpen(e -> {
				NetworkMonitor.get().onOpen(TSocket.this);
				resolve.accept(ws);
			}).track(registrations);

			ws.onError(e -> {
				NetworkMonitor.get().onError(TSocket.this, e.toString());
				reject.accept(new IOException("WebSocket error during connect"));
			}).track(registrations);
		}).onSettled(() -> {
			registrations.cleanup();
			return ws;
		});
	}

	class SocketInputStream extends InputStream {
		private final List<byte[]> buffers = new LinkedList<>();
		private byte[] curr = null;
		private int index = 0;

		private boolean eof = false;
		private volatile Throwable failure;   // touched from multiple threads

		// waiter for "new data / EOF / failure"
		private JSConsumer<Void> pendingResolve;
		private JSConsumer<Object> pendingReject;

		private int availableBytes = 0;

		// generation token to distinguish different waits
		private volatile int waitToken = 0;

		public void pushBuffer(Int8Array buf) {
			if (eof || failure != null) {
				return; // ignore if already closed/failed
			}
			availableBytes += buf.getLength();
			buffers.add(buf.toJavaArray());
			NetworkMonitor.get().onUpdateInBuffer(TSocket.this, availableBytes);
			wakeWaiter();
		}

		@Override
		public void close() throws IOException {
			super.close();
			signalClosed();
			this.buffers.clear();
			this.curr = null;
			availableBytes = 0;
		}

		public void signalClosed() {
			eof = true;
			wakeWaiter();
		}

		private void wakeWaiter() {
			JSConsumer<Void> r = pendingResolve;
			JSConsumer<Object> rej = pendingReject;
			// clear first to avoid double-fire
			pendingResolve = null;
			pendingReject = null;

			if (failure != null && rej != null) {
				rej.accept(failure);
			} else if (r != null) {
				r.accept(null);
			}
		}

		@Override
		public int available() {
			if (failure != null || (eof && buffers.isEmpty() && (curr == null || index >= curr.length))) {
				return 0;
			}
			return availableBytes;
		}

		@Override
		public int read() throws IOException {
			byte[] one = new byte[1];
			int n = read(one, 0, 1);
			return (n == -1) ? -1 : (one[0] & 0xFF);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			}
			if (off < 0 || len < 0 || off + len > b.length) {
				throw new IndexOutOfBoundsException();
			}
			if (len == 0) {
				return 0;
			}

			while (true) {
				int n = drainInto(b, off, len);
				if (n > 0) {
					availableBytes -= n;
					NetworkMonitor.get().onBytesIn(TSocket.this, n);
					return n;
				}

				if (failure != null) {
					if (failure instanceof TSocketTimeoutException) {
						throw (TSocketTimeoutException) failure;
					}
					throw new IOException("Socket read failed", failure);
				}
				if (eof && buffers.isEmpty() && (curr == null || index >= curr.length)) {
					return -1;
				}

				// Wait for more data / EOF / failure / timeout
				waitForData().await();
				// then loop and try again
			}
		}

		private synchronized int drainInto(byte[] b, int off, int len) {
			if (failure != null) {
				return 0;
			}
			if (curr == null || index >= curr.length) {
				if (buffers.isEmpty()) {
					return 0;
				}
				curr = buffers.remove(0);
				index = 0;
			}

			int count = Math.min(len, curr.length - index);
			System.arraycopy(curr, index, b, off, count);
			index += count;
			return count;
		}

		private JSPromise<Void> waitForData() {
			// If we already have data or are at EOF/failure, don't actually wait
			if (failure != null ||
				!buffers.isEmpty() ||
				(curr != null && index < curr.length) ||
				eof) {
				return JSPromise.resolve(null);
			}

			int timeout = TSocket.this.getSoTimeout();

			return new JSPromise<>((resolve, reject) -> {
				// new generation for this wait
				final int myToken = ++waitToken;

				// wrap resolve/reject so they only fire for the current wait
				pendingResolve = v -> {
					if (waitToken != myToken) {
						return; // stale
					}
					pendingResolve = null;
					pendingReject = null;
					resolve.accept(v);
				};
				pendingReject = err -> {
					if (waitToken != myToken) {
						return; // stale
					}
					pendingResolve = null;
					pendingReject = null;
					reject.accept(err);
				};

				if (timeout > 0) {
					ThreadUtils.runOnce("TSocket-ReadTimeout", () -> {
						// runs on scheduler thread
						if (waitToken != myToken) {
							return; // this wait has already completed or been replaced
						}

						// still waiting; mark timeout and wake
						if (failure == null &&
							(pendingResolve != null || pendingReject != null)) {

							failure = new TSocketTimeoutException(
								"Read timed out after " + timeout + " ms");
							wakeWaiter();
						}
					}, timeout);
				}
			});
		}
	}

	class SocketOutputStream extends OutputStream {

		private final Uint8ClampedArray singleByteArray = new Uint8ClampedArray(1);

		@Override
		public void write(int b) throws IOException {
			singleByteArray.set(0, (b & 0xFF));
			write(singleByteArray);
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			Int8Array nativeArr = Int8Array.fromJavaArray(b);
			Uint8ClampedArray view = new Uint8ClampedArray(nativeArr.getBuffer(),
				nativeArr.getByteOffset() + off,
				len);
			write(view);
		}

		private void write(ArrayBufferView arr) throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				throw new IOException("Closed socket");
			}
			NetworkMonitor.get().onBytesOut(TSocket.this, arr.getLength());
			socket.send(arr);
		}
	}
}
