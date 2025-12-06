package me.mdbell.awtea.classlib.java.net;

import me.mdbell.awtea.monitor.NetworkMonitor;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.websocket.WebSocket;
import me.mdbell.awtea.util.jso.JSRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TSocket {

	private static final JSRecord PORT_TO_ROUTE_MAPPING = JSRecord.create();

	static {
		PORT_TO_ROUTE_MAPPING.put(43594, "js5");
		PORT_TO_ROUTE_MAPPING.put(43595, "game");
	}

	private final String host;
	private final int port;
	private final String route;

	private WebSocket socket;
	private SocketInputStream in;
	private SocketOutputStream out;
	private boolean connected = false;

	public TSocket(TInetAddress address, int port) throws TUnknownHostException {
		this.host = address.getHost();
		this.port = port;
		this.route = PORT_TO_ROUTE_MAPPING.has(port) ? PORT_TO_ROUTE_MAPPING.get(port) : null;

		this.in = new SocketInputStream();
		this.out = new SocketOutputStream();

		// register with monitor in "connecting" state
		NetworkMonitor.get().register(this, host, port, route);

		this.connect(host, port);
	}

	public void setSoTimeout(int timeout) throws SocketException {
	}

	public void setTcpNoDelay(boolean on) throws SocketException {
	}

	public InputStream getInputStream() throws IOException {
		return this.in;
	}

	public OutputStream getOutputStream() throws IOException {
		return this.out;
	}

	public void close() throws IOException {
		if (socket != null && socket.getReadyState() <= 1) {
			NetworkMonitor.get().onClosing(this);

			socket.close();
			NetworkMonitor.get().onClosed(this);
		}
	}

	@Async
	private native void connect(String server, int port);

	private void connect(String server, int port, AsyncCallback<Void> callback) {
		String url = "wss://play.fifthage.io/js5";//createWebsocketUrl(server, port);

		System.out.println("Connecting to " + url);
		WebSocket ws = new WebSocket(url, "binary");
		ws.setBinaryType("arraybuffer");

		ws.onOpen(e -> {
			this.socket = ws;  // Set socket after connection opens
			this.connected = true;

			NetworkMonitor.get().onOpen(TSocket.this);


			// Set up handlers AFTER socket is assigned
			socket.onMessage(evt -> {
				Uint8ClampedArray arr = new Uint8ClampedArray(evt.getDataAsArray());
				in.buffers.add(arr);
				NetworkMonitor.get().onBytesIn(TSocket.this, arr.getLength());
				in.completeAndDelete(CallbackResult.READ);
			});

			socket.onError(event -> {
				try {
					close();
				} catch (IOException ignored) {
				}
			});

			callback.complete(null);
		});

		ws.onError(e -> {
			NetworkMonitor.get().onError(TSocket.this, "connect error");
			callback.error(new IOException("Unable to open socket"));
		});
	}

	private static String createWebsocketUrl(String server, int port) {
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
		private final List<Uint8ClampedArray> buffers = new LinkedList<>();
		private Uint8ClampedArray curr = null;
		private int index = 0;
		AsyncCallback<CallbackResult> callback = null;

		public int available() throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				return 0;
			}
			int i = curr != null ? curr.getLength() - index : 0;
			for (int j = 0; j < buffers.size(); j++) {
				i += buffers.get(j).getLength();
			}
			return i;
		}

		@Override
		public int read() throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				return -1;
			}
			if (curr != null && index < curr.getLength()) {
				return curr.get(index++);
			}
			if (!buffers.isEmpty()) {
				curr = buffers.remove(0);
				index = 0;
				return read();
			}
			CallbackResult result = awaitBuffer();
			if (result == CallbackResult.READ) {
				return read();
			}
			return -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				return -1;
			}
			if (curr != null && index < curr.getLength()) {
				int count = Math.min(curr.getLength() - index, len);
				for (int i = 0; i < count; i++) {
					b[i + off] = (byte) curr.get(index++);
				}
				return count;
			}
			if (!buffers.isEmpty()) {
				curr = buffers.remove(0);
				index = 0;
				return read(b, off, len);
			}
			CallbackResult result = awaitBuffer();
			if (result == CallbackResult.READ) {
				return read(b, off, len);
			}
			return -1;
		}

		@Async
		private native CallbackResult awaitBuffer() throws IOException;

		private void awaitBuffer(AsyncCallback<CallbackResult> callback) {
			this.callback = callback;
		}

		private void errorAndDelete(Throwable t) {
			AsyncCallback<CallbackResult> c = this.callback;
			if (c != null) {
				this.callback = null;
				c.error(t);
			}
		}

		private void completeAndDelete(CallbackResult res) {
			AsyncCallback<CallbackResult> c = this.callback;
			if (c != null) {
				this.callback = null;
				c.complete(res);
			}
		}
	}

	class SocketOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b});
		}

		@Override
		public void write(byte[] b) throws IOException {
			if (socket == null || socket.getReadyState() > 1) {  // Add null check
				throw new IOException("Closed socket");
			}
			Uint8Array arr = new Uint8Array(b.length);
			arr.set(b);
			socket.send(arr);

			NetworkMonitor.get().onBytesOut(TSocket.this, b.length);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {

			NetworkMonitor.get().onBytesOut(TSocket.this, len);

			write(Arrays.copyOfRange(b, off, len + off));
		}
	}

	private enum CallbackResult {
		READ, CLOSED
	}
}
