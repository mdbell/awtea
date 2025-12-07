package me.mdbell.awtea.classlib.javax.sound.sampled;

import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.sound.AbstractDataLine;
import me.mdbell.awtea.sound.pcm.AudioWorkletLine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;

public class TAudioSystem {

    public static Line getLine(Line.Info info) throws LineUnavailableException {
        DataLine.Info dataInfo = (DataLine.Info) info;
		AudioFormat format = dataInfo.getFormats()[0];
		return createLine(format);
    }

	public static TSourceDataLine getSourceDataLine(AudioFormat format) throws LineUnavailableException {
		return (TSourceDataLine) createLine(format);
	}

	private static AbstractDataLine createLine(AudioFormat format) throws LineUnavailableException {
		return new AudioWorkletLine(format);
	}
}
