package me.mdbell.awtea.sound;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

public class CircularAudioBuffer {

	private static final Logger log = LoggerFactory.getLogger(CircularAudioBuffer.class);

	private final float[] buf;
    private final int size;
    private int writeIndex = 0;
    private int readIndex = 0;

    public CircularAudioBuffer(int size) {
        this.size = size;
        this.buf = new float[size];
    }

    public int availableToWrite() {
        return size - (writeIndex - readIndex + size) % size;
    }

    public int availableToRead() {
        return (writeIndex - readIndex + size) % size;
    }

    public void write(float data) {
        if (availableToWrite() > 0) {
            buf[writeIndex] = data;
            writeIndex = (writeIndex + 1) % size;
        } else {
			log.warn("Audio buffer overrun!");
		}
    }

    public void write(float[] data, int offset, int length) {
        int samplesToWrite = Math.min(length, availableToWrite());
        for (int i = 0; i < samplesToWrite; i++) {
            buf[writeIndex] = data[offset + i];
            writeIndex = (writeIndex + 1) % size;
        }
    }

    public int read(float[] dst, int length) {
        int available = Math.min(length, availableToRead());
        for (int i = 0; i < available; i++) {
            dst[i] = buf[readIndex];
            readIndex = (readIndex + 1) % size;
        }
        return available;
    }

    public void reset() {
        writeIndex = readIndex = 0;
    }
}
