package me.mdbell.awtea.classlib.javax.sound.sampled;

/**
 * @see javax.sound.sampled.SourceDataLine
 */
public interface TSourceDataLine extends TDataLine {

	void open(TAudioFormat format, int bufferSize) throws TLineUnavailableException;

	void open(TAudioFormat format) throws TLineUnavailableException;

	int write(byte[] b, int off, int len);

}
