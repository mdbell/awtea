package me.mdbell.awtea.net;

import org.teavm.jso.JSBody;

public class DefaultSocketResolverFactory extends SocketResolverFactory {

	@Override
	public SocketResolver createSocketResolver() {
		return (hostname, port) -> {
			// self.location is available in both main thread and dedicated workers
			String protocol = selfLocationProtocol();
			if (protocol.equals("https:")) {
				protocol = "wss";
			} else {
				protocol = "ws";
			}
			return protocol + "://" + hostname + ":" + port;
		};
	}

	@JSBody(script = "return self.location.protocol;")
	private static native String selfLocationProtocol();
}
