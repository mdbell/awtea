package me.mdbell.awtea.classlib.java.awt;

import org.teavm.jso.JSBody;

class TToolKitFactory {

	private TToolKitFactory() {}

	public static TToolkit createToolkit() {
		if (isWorkerContext()) {
			return new TWorkerToolkit();
		}
		return new TAWTeaToolkit();
	}

	@JSBody(script = "return typeof DedicatedWorkerGlobalScope !== 'undefined' && self instanceof DedicatedWorkerGlobalScope;")
	private static native boolean isWorkerContext();
}
