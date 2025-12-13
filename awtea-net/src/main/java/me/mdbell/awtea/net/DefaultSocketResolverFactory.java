package me.mdbell.awtea.net;

import org.teavm.jso.browser.Window;

public class DefaultSocketResolverFactory extends SocketResolverFactory {

	@Override
	public SocketResolver createSocketResolver() {
		return (hostname, port) -> {
			String protocol = Window.current().getLocation().getProtocol();
			if (protocol.equals("https:")) {
				protocol = "wss";
			} else {
				protocol = "ws";
			}
			return protocol + "://" + hostname + ":" + port;
		};
	}
	
}
