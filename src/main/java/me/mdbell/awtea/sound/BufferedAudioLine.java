package me.mdbell.awtea.sound;

import org.teavm.jso.browser.Window;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

public class BufferedAudioLine extends AbstractDataLine {

	/**
	 * The time in milliseconds between each flush of the audio buffer, or how often we check to see if there is any
	 * audio for us to play. higher value = lower cpu usage, but greater audio delay.
	 */
	private static final int FLUSH_TIME_MS = 10;

	/**
	 * The max amount of audio we can queue ahead of time.
	 * Note: bigger values != better - it will introduce audio lag (on top of the existing lag that is part of WebAudio)
	 */
	private static final double MAX_QUEUE_SECONDS = 0.075;

	private static final AudioContext context = AudioUtils.getGlobalContext();
	private static final GainNode gainNode;

	static {
		gainNode = context.createGain();
		gainNode.getGain().setValueAtTime(SFX_GAIN, context.getCurrentTime());
		gainNode.connect(AudioUtils.getGlobalGain());
	}

	private int lastTimeout = 0;
	private CircularAudioBuffer audioBuffer;
	private final List<AudioBufferSourceNode> sources = new ArrayList<>();

	private double nextStartTime = 0;
	private int bufferFramesCapacity;

	public BufferedAudioLine(AudioFormat fmt) throws LineUnavailableException {
		super(fmt);
	}

	@Override
	public void start() {
		super.start();
		// inital startup
		scheduleDrain(0);
	}

	@Override
	public void stop() {
		super.stop();
		// immediately stop the next drain if scheduled
		if (lastTimeout != 0) {
			Window.clearTimeout(lastTimeout);
			lastTimeout = 0;
		}
	}

	@Override
	public void flush() {
		super.flush();

		if (audioBuffer != null) {
			audioBuffer.reset();
		}
		sources.forEach(n -> {
			try {
				n.stop(0);
			} catch (Throwable ignored) {
			}
			n.disconnect();
		});
		sources.clear();
		nextStartTime = context.getCurrentTime();
	}

	@Override
	protected void openBackend(int bufferFrames) {
		this.bufferFramesCapacity = bufferFrames > 0 ? bufferFrames : (int) (sampleRate * 0.1);
		int samplesCapacity = bufferFramesCapacity * channels;
		this.audioBuffer = new CircularAudioBuffer(samplesCapacity);
		this.nextStartTime = context.getCurrentTime();
		scheduleDrain(0);
	}

	@Override
	protected int getFreeFrames() {
		if (audioBuffer == null || !isOpen()) {
			return 0;
		}
		int samplesFree = audioBuffer.availableToWrite();
		return samplesFree / channels;
	}

	@Override
	protected int getMaxFrames() {
		return bufferFramesCapacity;
	}

	@Override
	protected int enqueue(float[] samples, int frames) {
		if (audioBuffer == null) {
			return 0;
		}
		int framesFree = getFreeFrames();
		int framesToWrite = Math.min(frames, framesFree);
		int samplesToWrite = framesToWrite * channels;

		audioBuffer.write(samples, 0, samplesToWrite);
		return framesToWrite;
	}


	private void scheduleDrain(int delayMs) {
		if (!isActive()) {
			return;
		}
		if (lastTimeout != 0) {
			Window.clearTimeout(lastTimeout);
		}
		lastTimeout = Window.setTimeout(this::drain, delayMs);
	}

	@Override
	public void drain() {
		super.drain();
		try {
			if (audioBuffer == null) {
				return;
			}

			int availableSamples = audioBuffer.availableToRead();
			if (availableSamples == 0) {
				return;
			}

			double now = context.getCurrentTime();
			double queuedAhead = nextStartTime - now;
			if (queuedAhead > MAX_QUEUE_SECONDS) {
				// too much scheduled already, wait a bit
				return;
			}

			int framesToPlay = availableSamples / channels;
			if (framesToPlay <= 0) {
				return;
			}

			AudioBuffer buffer = context.createBuffer(channels, framesToPlay, sampleRate);
			float[] tmp = new float[availableSamples];
			audioBuffer.read(tmp, availableSamples);

			if (channels == 1) {
				// mono audio, fast copy
				Float32Array ch0 = buffer.getChannelData(0);
				//TODO: can we just ch0.set(tmp)?
				for (int i = 0; i < framesToPlay; i++) {
					ch0.set(i, tmp[i]);
				}
			} else if (channels == 2) {
				// stereo audio, need to de-interlace
				Float32Array ch0 = buffer.getChannelData(0);
				Float32Array ch1 = buffer.getChannelData(1);
				int si = 0;
				for (int frame = 0; frame < framesToPlay; frame++) {
					ch0.set(frame, tmp[si++]);
					ch1.set(frame, tmp[si++]);
				}
			} else {
				// n channels - generic slow path (client doesn't use > 2 anyways)
				Float32Array[] chans = new Float32Array[channels];
				for (int ch = 0; ch < channels; ch++) {
					chans[ch] = buffer.getChannelData(ch);
				}
				int si = 0;
				for (int f = 0; f < framesToPlay; f++) {
					for (int ch = 0; ch < channels; ch++) {
						chans[ch].set(f, tmp[si++]);
					}
				}
			}

			double startTime = Math.max(now, nextStartTime);
			double duration = buffer.getDuration();

			AudioBufferSourceNode src = context.createBufferSource();
			if (src == null) {
				System.err.println("Failed to allocate audio src node - skipping.");
				return;
			}

			src.setBuffer(buffer);
			src.connect(gainNode);
			sources.add(src);
			src.onEnded(evt -> sources.remove(src));
			src.start(startTime);

			nextStartTime = startTime + duration;
		} finally {
			// always schedule the next drain
			scheduleDrain(FLUSH_TIME_MS);
		}
	}
}
