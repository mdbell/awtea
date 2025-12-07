package me.mdbell.awtea.sound;

import lombok.Getter;
import me.mdbell.awtea.monitor.LineMonitor;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;

import javax.sound.sampled.*;

public abstract class AbstractDataLine implements SourceDataLine, AudioConstants {

	protected int channels;
	protected int sampleRate;
	protected int sampleSizeBits;
	protected int sampleSizeBytes;
	protected int frameSizeBytes;
	protected boolean bigEndian;
	protected float floatScale;

	@Getter
	private AudioFormat format;

	@Getter
	private boolean running = false;

	@Getter
	private boolean open = true;

	private float[] sampleScratch;

	protected AbstractDataLine(AudioFormat fmt) throws LineUnavailableException {
		open(fmt);
	}

	/**
	 * Called from open() after basic format fields are initialized. bufferFrames is a hint/capacity.
	 */
	protected abstract void openBackend(int bufferFrames) throws LineUnavailableException;

	/**
	 * How many frames can be queued *now* without blocking?
	 */
	protected abstract int getFreeFrames();

	/**
	 * Maximum queue capacity in frames (for getBufferSize/available).
	 */
	protected abstract int getMaxFrames();

	/**
	 * Enqueue up to `frames` frames from the `samples` array.
	 * Samples are interleaved [-1, 1].
	 *
	 * @return The number of frames accepted or 0 if full
	 */
	protected abstract int enqueue(float[] samples, int frames);

	/**
	 * Drain up to `framesToDrain` frames from the backend.
	 *
	 * @param framesToDrain number of frames to drain, or -1 to wait until all drained
	 * @return number of frames actually drained - may be less than requested
	 */
	protected abstract int drainInternal(int framesToDrain);

	@Override
	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		this.format = format;
		this.channels = format.getChannels();
		this.sampleRate = (int) format.getSampleRate();
		this.sampleSizeBits = format.getSampleSizeInBits();
		this.sampleSizeBytes = this.sampleSizeBits / 8;
		this.frameSizeBytes = this.sampleSizeBytes * this.channels;
		this.bigEndian = format.isBigEndian();
		this.floatScale = (float) (1.0 / Math.pow(2, this.sampleSizeBits - 1));

		int framesHint = bufferSize / this.channels;
		if (framesHint <= 0) {
			framesHint = (int) (sampleRate * 0.1); // ~100ms as a fallback
		}

		// scratch buffer for converting bytes -> float PCM
		int scratchFrames = Math.max(framesHint, 2048);
		if (sampleScratch == null || sampleScratch.length < scratchFrames * channels) {
			sampleScratch = new float[scratchFrames * channels];
		}

		open = true;
		running = false;

		System.out.println("Opening AbstractDataLine with buffer size hint: " + framesHint + " frames");

		openBackend(framesHint);

		LineMonitor.get().registerOutputLine(this);
	}

	@Override
	public final void open(AudioFormat format) throws LineUnavailableException {
		// use ~100ms as default buffer
		int bufferSize = (int) (format.getSampleRate() * format.getChannels() * 0.1);
		open(format, bufferSize);
	}

	@Override
	public void start() {
		running = true;
		LineMonitor.get().onStart(this);
	}

	@Override
	public void stop() {
		running = false;
		LineMonitor.get().onStop(this);
	}

	@Override
	public void flush() {
		LineMonitor.get().onFlush(this);
	}

	@Override
	public void close() {
		open = false;
		running = false;
		LineMonitor.get().onClose(this);
	}

	@Override
	public void drain() {
		if (!open || !running) {
			return;
		}
		drainInternal(-1);
	}

	@Override
	public final boolean isActive() {
		return running && open;
	}

	@Override
	public final int getBufferSize() {
		return getMaxFrames() * frameSizeBytes;
	}

	@Override
	public final int available() {
		if (!open || !running) {
			LineMonitor.get().onAvailable(this, 0);
			return 0;
		}
		int freeFrames = getFreeFrames();
		if (freeFrames < 0) freeFrames = 0;

		int result = freeFrames * frameSizeBytes;

		LineMonitor.get().onAvailable(this, result);

		return result;
	}

	@Override
	public long getMicrosecondPosition() {
		// implementations can override if needed
		return 0;
	}

	@Override
	public int getFramePosition() {
		return 0;
	}

	@Override
	public long getLongFramePosition() {
		return 0;
	}

	@Override
	public float getLevel() {
		return 0f;
	}

	@Override
	public Line.Info getLineInfo() {
		return null;
	}

	@Override
	public void open() throws LineUnavailableException {
	}

	@Override
	public Control[] getControls() {
		return new Control[0];
	}

	@Override
	public boolean isControlSupported(Control.Type control) {
		return false;
	}

	@Override
	public Control getControl(Control.Type control) {
		return null;
	}

	@Override
	public void addLineListener(LineListener listener) {
	}

	@Override
	public void removeLineListener(LineListener listener) {
	}

	@Override
	public final int write(byte[] b, int off, int len){

		if (!open || getMaxFrames() <= 0) {
			return 0;
		}

		final int frameSizeBytes = this.frameSizeBytes;
		final int channels       = this.channels;
		final int sampleBytes    = this.sampleSizeBytes;
		final int bits           = this.sampleSizeBits;
		final boolean be         = this.bigEndian;
		final float scale        = this.floatScale;

		int framesRequested = len / frameSizeBytes;
		if (framesRequested == 0) {
			return 0;
		}
		final int bytesRequested = framesRequested * frameSizeBytes;

		int writtenBytes = 0;

		while (writtenBytes < bytesRequested) {
			if (!open || !running) {
				break;
			}

			int remainingBytes  = bytesRequested - writtenBytes;
			int remainingFrames = remainingBytes / frameSizeBytes;
			if (remainingFrames <= 0) {
				break;
			}

			int freeFrames = getFreeFrames();
			if (freeFrames <= 0) {
				// Let backend drain; this will async-wait and then return
				int drained = drainInternal(remainingFrames);
				// If nothing drained and still no free frames, bail out to avoid spinning
				if (drained <= 0 && getFreeFrames() <= 0) {
					break;
				}
				continue;
			}

			int maxScratchFrames = sampleScratch.length / channels;
			int framesToConvert  = Math.min(remainingFrames, maxScratchFrames);
			framesToConvert      = Math.min(framesToConvert, freeFrames);
			if (framesToConvert <= 0) {
				// Shouldn't happen, but be defensive
				int drained = drainInternal(remainingFrames);
				if (drained <= 0) {
					break;
				}
				continue;
			}

			int start = off + writtenBytes;
			int end   = start + framesToConvert * frameSizeBytes;
			int si    = 0;

			// Convert bytes -> float samples (interleaved)
			for (int i = start; i < end; i += frameSizeBytes) {
				for (int ch = 0; ch < channels; ch++) {
					int sampleOffset = i + ch * sampleBytes;
					int sample       = AudioUtils.getSample(b, sampleOffset, bits, be);
					sampleScratch[si++] = sample * scale;
				}
			}

			int framesEnqueued = enqueue(sampleScratch, framesToConvert);
			if (framesEnqueued <= 0) {
				// Backend lied about free frames or is temporarily stuck;
				// try draining some and retry.
				int drained = drainInternal(remainingFrames);
				if (drained <= 0) {
					break;
				}
				continue;
			}

			int bytesPushed = framesEnqueued * frameSizeBytes;
			writtenBytes += bytesPushed;

			LineMonitor.get().onWrite(this, bytesPushed);
			onSamplesChunk(sampleScratch, framesEnqueued);
		}

		return writtenBytes;
	}

	protected void onSamplesChunk(float[] samples, int frames) {
		// compute per-channel peak from this chunk and forward to AudioMonitor
		if (samples == null || frames <= 0) {
			return;
		}
		float[] peaks = new float[channels];
		int idx = 0;
		for (int f = 0; f < frames; f++) {
			for (int ch = 0; ch < channels; ch++) {
				float v = Math.abs(samples[idx++]);
				if (v > peaks[ch]) {
					peaks[ch] = v;
				}
			}
		}
		LineMonitor.get().onPcmEnvelope(this, peaks);
	}

}
