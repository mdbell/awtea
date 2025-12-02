package me.mdbell.awtea.sound;

import lombok.Getter;
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

	/**
	 * Mostly used to signal if a write should be interrupted - bit of a hack,
	 * but we need some way to make writes sync
	 */
	private int writeEpoch = 0;

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
		writeEpoch++;

		openBackend(framesHint);
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
	}

	@Override
	public void stop() {
		running = false;
		writeEpoch++;
	}

	@Override
	public void flush() {
		writeEpoch++;
	}

	@Override
	public void close() {
		open = false;
		running = false;
		writeEpoch++;
	}

	@Override
	public void drain() {

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
			return 0;
		}
		int freeFrames = getFreeFrames();
		if (freeFrames < 0) freeFrames = 0;
		return freeFrames * frameSizeBytes;
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


	@Async
	@Override
	public final native int write(byte[] b, int off, int len);

	@SuppressWarnings("unused")
	private void write(byte[] b, int off, int len, AsyncCallback<Integer> cb) {

		if (!open || getMaxFrames() <= 0) {
			cb.complete(0);
			return;
		}

		final int frameSizeBytes = this.frameSizeBytes;
		final int channels = this.channels;
		final int sampleBytes = this.sampleSizeBytes;
		final int bits = this.sampleSizeBits;
		final boolean be = this.bigEndian;
		final float scale = this.floatScale;

		int framesRequested = len / frameSizeBytes;
		if (framesRequested == 0) {
			cb.complete(0);
			return;
		}
		final int bytesRequested = framesRequested * frameSizeBytes;

		final int startEpoch = writeEpoch;
		final int[] writtenBytes = {0};

		class Writer {
			void attempt() {
				if (!open || writeEpoch != startEpoch) {
					cb.complete(writtenBytes[0]);
					return;
				}

				int remainingBytes = bytesRequested - writtenBytes[0];
				if (remainingBytes <= 0) {
					cb.complete(writtenBytes[0]);
					return;
				}

				int remainingFrames = remainingBytes / frameSizeBytes;

				// Backpressure from backend
				int freeFrames = getFreeFrames();
				if (freeFrames <= 0) {
					Window.setTimeout(this::attempt, 0);
					return;
				}

				int maxScratchFrames = sampleScratch.length / channels;
				int framesToConvert = Math.min(remainingFrames, maxScratchFrames);
				framesToConvert = Math.min(framesToConvert, freeFrames);
				if (framesToConvert <= 0) {
					Window.setTimeout(this::attempt, 0);
					return;
				}

				int start = off + writtenBytes[0];
				int end = start + framesToConvert * frameSizeBytes;
				int si = 0;

				// Convert bytes -> float samples (interleaved)
				for (int i = start; i < end; i += frameSizeBytes) {
					for (int ch = 0; ch < channels; ch++) {
						int sampleOffset = i + ch * sampleBytes;
						int sample = AudioUtils.getSample(b, sampleOffset, bits, be);
						sampleScratch[si++] = sample * scale;
					}
				}

				int framesEnqueued = enqueue(sampleScratch, framesToConvert);
				if (framesEnqueued <= 0) {
					Window.setTimeout(this::attempt, 0);
					return;
				}

				int bytesPushed = framesEnqueued * frameSizeBytes;
				writtenBytes[0] += bytesPushed;

				if (writtenBytes[0] >= bytesRequested) {
					cb.complete(writtenBytes[0]);
				} else {
					Window.setTimeout(this::attempt, 0);
				}
			}
		}

		new Writer().attempt();
	}
}
