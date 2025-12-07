package me.mdbell.awtea.classlib.java.net;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.monitor.NetworkMonitor;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.Registration;
import org.teavm.jso.function.JSConsumer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.websocket.WebSocket;
import me.mdbell.awtea.util.jso.JSRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ExtensionMethod({JSObjectsExtensions.class})
public class TSocket {

	private static final JSRecord PORT_TO_ROUTE_MAPPING = JSRecord.create();

	static {
		PORT_TO_ROUTE_MAPPING.put(43594, "js5");
		PORT_TO_ROUTE_MAPPING.put(43595, "game");
	}

	private final String host;
	private final int port;

	private final WebSocket socket;
	private final SocketInputStream inputStream;
	private final SocketOutputStream outputStream;

	private final List<Registration> registrations = new ArrayList<>();

	public TSocket(TInetAddress address, int port) throws TUnknownHostException {
		this.host = address.getHost();
		this.port = port;
		String route = PORT_TO_ROUTE_MAPPING.has(port) ? PORT_TO_ROUTE_MAPPING.get(port) : null;

		// register with monitor in "connecting" state
		NetworkMonitor.get().register(this, host, port, route);

		this.socket = this.connect(host, port).await();

		// cleanup on close
		registrations.cleanup();

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

	public void setSoTimeout(int timeout) throws SocketException {
	}

	public void setTcpNoDelay(boolean on) throws SocketException {
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
		return new JSPromise<>((resolve, reject) -> {
			//String url = createWebsocketUrl(server, port);
			String url = "wss://play.fifthage.io/js5";
			System.out.println("Connecting to " + server + ":" + port + "(" + url + ") via WebSocket");
			WebSocket ws = new WebSocket(url, "binary");
			ws.setBinaryType("arraybuffer");

			NetworkMonitor.get().onConnecting(TSocket.this);
			ws.onOpen(e -> {
				NetworkMonitor.get().onOpen(TSocket.this);
				resolve.accept(ws);
			}).track(registrations);

			ws.onError(e -> {
				NetworkMonitor.get().onError(TSocket.this, e.toString());
				reject.accept(new IOException("WebSocket error during connect"));
			}).track(registrations);
		});
	}

	private String createWebsocketUrl(String server, int port) {
		String protocol = Window.current().getLocation().getProtocol();
//		if (protocol.equals("https:") || Settings.SECURE) {
//			protocol = "wss";
//		} else {
//			protocol = "ws";
//		}
		protocol = "wss";
		if (!PORT_TO_ROUTE_MAPPING.has(port)) {
			return protocol + "://" + server + ":" + port;
		}
		String route = PORT_TO_ROUTE_MAPPING.get(port);
		if ("localhost".equals(server)) {
			return protocol + "://" + server + ":" + port + "/" + route;
		}
		return protocol + "://" + server + "/" + route;
	}

	class SocketInputStream extends InputStream {
		private final List<byte[]> buffers = new LinkedList<>();
		private byte[] curr = null;
		private int index = 0;

		private boolean eof = false;
		private Throwable failure;

		// A single waiter for "new data or closed"
		private JSConsumer<Void> pendingResolve;
		private JSConsumer<Object> pendingReject;

		private int availableBytes = 0;

		public synchronized void pushBuffer(Int8Array buf) {
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

		public synchronized void signalClosed() {
			eof = true;
			wakeWaiter();
		}

		public synchronized void fail(Throwable t) {
			failure = t;
			wakeWaiter();
		}

		private void wakeWaiter() {
			if (pendingResolve != null || pendingReject != null) {
				var r = pendingResolve;
				var rej = pendingReject;
				pendingResolve = null;
				pendingReject = null;
				if (failure != null && rej != null) {
					rej.accept(failure);
				} else if (r != null) {
					r.accept(null);
				}
			}
		}

		@Override
		public synchronized int available() {
			if (failure != null || eof && buffers.isEmpty() && (curr == null || index >= curr.length)) {
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

				// No data currently available
				synchronized (this) {
					if (failure != null) {
						throw new IOException("Socket read failed", failure);
					}
					if (eof && buffers.isEmpty() && (curr == null || index >= curr.length)) {
						return -1;
					}
				}

				// Wait for more data or closure
				waitForData().await();
				// loop back and try again
			}
		}

		private synchronized int drainInto(byte[] b, int off, int len) {
			if (failure != null) {
				return 0;
			}
			// Ensure we have a current buffer
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
			synchronized (this) {
				// If we already have data or are at EOF/failure, don't actually wait
				if (failure != null ||
					!buffers.isEmpty() ||
					(curr != null && index < curr.length) ||
					eof) {
					return JSPromise.resolve(null);
				}

				return new JSPromise<>((resolve, reject) -> {
					pendingResolve = resolve;
					pendingReject = reject;
				});
			}
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
			Uint8ClampedArray view = new Uint8ClampedArray(nativeArr.getBuffer().slice(off, off + len));
			write(view);
		}

		private void write(Uint8ClampedArray arr) throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				throw new IOException("Closed socket");
			}
			NetworkMonitor.get().onBytesOut(TSocket.this, arr.getLength());
			socket.send(arr);
		}
	}
}
