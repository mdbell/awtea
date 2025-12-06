package me.mdbell.awtea.sound.pcm;

import me.mdbell.awtea.sound.AbstractDataLine;
import me.mdbell.awtea.sound.AudioMonitor;
import me.mdbell.awtea.sound.DrainListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

public class AudioWorkletLine extends AbstractDataLine {


	private PcmProcessorClient backend;
	private int maxQueuedFrames;

	public AudioWorkletLine(AudioFormat fmt) throws LineUnavailableException {
		super(fmt);
	}

	@Override
	protected void openBackend(int bufferFrames) {
		maxQueuedFrames = bufferFrames > 0 ? bufferFrames : (int) (sampleRate * 0.1);

		backend = new PcmProcessorClient(sampleRate, channels, maxQueuedFrames);

		backend.init();

		backend.addDrainListener(frames -> AudioMonitor.get().onDrain(this, frames * frameSizeBytes));
	}

	@Override
	public void close() {
		super.close();
		if (backend != null) {
			backend.close();
			backend = null;
		}
	}

	@Override
	protected int getFreeFrames() {
		if (backend == null || !isActive()) {
			return 0;
		}
		int queued = backend.getQueuedFrames();
		int max = backend.getMaxQueuedFrames();
		return Math.max(0, max - queued);
	}

	@Override
	protected int getMaxFrames() {
		return backend != null ? backend.getMaxQueuedFrames() : maxQueuedFrames;
	}

	@Override
	protected int enqueue(float[] samples, int frames) {
		if (backend == null) return 0;
		return backend.enqueue(samples, frames);
	}
}
