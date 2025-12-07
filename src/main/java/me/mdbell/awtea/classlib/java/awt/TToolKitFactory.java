package me.mdbell.awtea.classlib.java.awt;

class TToolKitFactory {

	private TToolKitFactory() {

	}

	public static TToolkit createToolkit() {
		return new TAWTeaToolkit();
	}
}
