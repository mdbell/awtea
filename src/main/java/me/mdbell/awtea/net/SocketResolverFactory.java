package me.mdbell.awtea.net;

public abstract class SocketResolverFactory {

	private static SocketResolverFactory instance;

	protected SocketResolverFactory() {

	}

	public abstract SocketResolver createSocketResolver();

	public static synchronized SocketResolverFactory getInstance() {
		if (instance == null) {
			instance = new DefaultSocketResolverFactory();
		}
		return instance;
	}

	public static synchronized void setInstance(SocketResolverFactory factory) {
		instance = factory;
	}
}
