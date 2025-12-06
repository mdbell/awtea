package me.mdbell.awtea.classlib.javax.sound.sampled;

import me.mdbell.awtea.impl.Debug;
import org.teavm.jso.JSBody;
import me.mdbell.awtea.sound.AbstractDataLine;
import me.mdbell.awtea.sound.BufferedAudioLine;
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
		if(isSecureContext()) {
			Debug.trigger();
			// better code path - but requires more permissions
			// moves most of the audio logic off the main thread, so we just stream PCM samples to it
			// without needing to buffer in the main thread
			return new AudioWorkletLine(format);
		}else {
			// plays audio only using the main thread, where we schedule audio by ourselves
			// less efficient as we need to buffer the audio in memory and then send it to WebAudio
			// which has their own buffering code
			return new BufferedAudioLine(format);
		}

		// we have a potential 3rd option, making using of SharedArrayBuffers to have a circular buffer
		// shared between the main and audio thread, so no extra memory allocations, or array copies
		// but they only work when CORS is strict
	}

	@JSBody(script = "return window.isSecureContext;")
	private static native boolean isSecureContext();
}
