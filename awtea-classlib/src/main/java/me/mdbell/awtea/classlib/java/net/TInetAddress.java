package me.mdbell.awtea.classlib.java.net;

import lombok.Getter;

@Getter
public class TInetAddress {

    private final String host;

    // Constructor for mocking IP addresses
    public TInetAddress(String host) {
        this.host = host;
    }

    public static TInetAddress getByName(String host) throws TUnknownHostException {
        // Mock behavior for Web client: return a dummy address with the provided host (which could be an IP)
        if (host == null || host.isEmpty()) {
            throw new TUnknownHostException("Host is null or empty");
        }

        // Return a mock TInetAddress object with the host string as the IP (just for simulation)
        return new TInetAddress(host);
    }

	public String getHostName() {
		return "localhost";
	}
}
