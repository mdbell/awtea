package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

/**
 * @see javax.sound.sampled.DataLine
 */
public interface TDataLine extends TLine {

	void drain();

	void flush();

	void start();

	void stop();

	boolean isRunning();

	boolean isActive();

	TAudioFormat getFormat();

	int getBufferSize();

	int available();

	int getFramePosition();

	long getLongFramePosition();

	long getMicrosecondPosition();

	float getLevel();

	@Getter
	@ToString
	class Info extends TLine.Info {

		private final TAudioFormat[] formats;
		private final int minBufferSize;
		private final int maxBufferSize;

		public Info(Class<? extends TDataLine> lineClass, TAudioFormat[] formats, int minBufferSize, int maxBufferSize) {
			super(lineClass);
			if (formats == null) {
				this.formats = new TAudioFormat[0];
			} else {
				this.formats = Arrays.copyOf(formats, formats.length);
			}
			this.minBufferSize = minBufferSize;
			this.maxBufferSize = maxBufferSize;
		}

		public Info(Class<? extends TDataLine> lineClass, TAudioFormat format, int bufferSize) {
			super(lineClass);
			if (format == null) {
				this.formats = new TAudioFormat[0];
			} else {
				this.formats = new TAudioFormat[]{format};
			}
			this.minBufferSize = bufferSize;
			this.maxBufferSize = bufferSize;
		}

		public Info(Class<? extends TDataLine> lineClass, TAudioFormat format) {
			this(lineClass, format, TAudioSystem.NOT_SPECIFIED);
		}

		public boolean isFormatSupported(TAudioFormat format) {
			for (TAudioFormat f : formats) {
				if (f.matches(format)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean matches(TLine.Info info) {
			if (!super.matches(info)) {
				return false;
			}
			Info dataInfo = (Info) info;

			if (this.maxBufferSize >= 0 && dataInfo.maxBufferSize >= 0) {
				if (this.maxBufferSize > dataInfo.maxBufferSize) {
					return false;
				}
			}

			if (this.minBufferSize >= 0 && dataInfo.minBufferSize >= 0) {
				if (this.minBufferSize < dataInfo.minBufferSize) {
					return false;
				}
			}

			for (TAudioFormat f : this.formats) {
				if (!dataInfo.isFormatSupported(f)) {
					return false;
				}
			}

			return true;
		}
	}
}
