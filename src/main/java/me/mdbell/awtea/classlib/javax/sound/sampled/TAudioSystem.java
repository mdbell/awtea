package me.mdbell.awtea.classlib.javax.sound.sampled;


public class TAudioSystem {

	public static final int NOT_SPECIFIED = -1;

	public static TLine getLine(TLine.Info info) throws TLineUnavailableException {
		TDataLine.Info dataInfo = (TDataLine.Info) info;
		Class<?> lineClass = info.getLineClass();
		if (!lineClass.isAssignableFrom(dataInfo.getLineClass())) {
			throw new TLineUnavailableException("Unsupported line class: " + lineClass.getName());
		}
		return new TAudioWorkletSourceDataLine(dataInfo);
	}

	public static TSourceDataLine getSourceDataLine(TAudioFormat format) throws TLineUnavailableException {
		TDataLine.Info info = new TDataLine.Info(TSourceDataLine.class, format);
		return (TSourceDataLine) getLine(info);
	}
}
