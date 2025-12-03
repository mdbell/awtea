package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;

public interface TDataLine extends TLine {
	void flush();

	@Getter
	class Info extends TLine.Info {
		private TAudioFormat format;
		private int bufferSize;
	    public Info(Class<? extends TDataLine> lineClass, TAudioFormat format, int bufferSize) {
			super(lineClass);
	        this.format = format;
	        this.bufferSize = bufferSize;
	    }

		public TAudioFormat[] getFormats() {
	        return new TAudioFormat[] { format };
	    }
	}
}
