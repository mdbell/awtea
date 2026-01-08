package me.mdbell.awtea.classlib.javax.sound.sampled;

import me.mdbell.awtea.monitor.LineMonitor;
import me.mdbell.awtea.sound.pcm.PcmProcessorClient;
import org.teavm.jso.core.JSPromise;

import java.util.concurrent.atomic.AtomicInteger;

public class TAudioWorkletSourceDataLine extends TAbstractSourceDataLine {

	private PcmProcessorClient backend;
	private int maxQueuedFrames;

	public TAudioWorkletSourceDataLine(TDataLine.Info info) throws TLineUnavailableException {
		super(info);
	}

	@Override
	protected void openBackend(int bufferFrames) {
		// Determine max queued frames (approx 100ms if not specified)
		maxQueuedFrames = bufferFrames > 0 ? bufferFrames : (int) (sampleRate * 0.1);

		backend = new PcmProcessorClient(sampleRate, channels, maxQueuedFrames);

		backend.init(sampleSizeBits, bigEndian);

		backend.addDrainListener(((bytesDrained, bytesRemaining) -> {
			// Directly use bytes - no conversion needed
			LineMonitor.get().onDrain(this, bytesDrained);
			return !isOpen();
		}));
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
		int queuedBytes = backend.getQueuedBytes();
		int maxBytes = backend.getMaxQueuedFrames() * frameSizeBytes;
		int freeBytes = Math.max(0, maxBytes - queuedBytes);
		return freeBytes / frameSizeBytes;
	}

	@Override
	protected int getMaxFrames() {
		return backend != null ? backend.getMaxQueuedFrames() : maxQueuedFrames;
	}

	@Override
	protected int enqueue(byte[] bytes, int frames) {
		if (backend == null) return 0;
		return backend.enqueue(bytes, frames);
	}

	@Override
	protected int drainInternal(int framesToDrain) {
		return drainAsync(framesToDrain).await();
	}

	private JSPromise<Integer> drainAsync(int framesToDrain) {
		if (backend == null || !isActive()) {
			return JSPromise.resolve(0);
		}
		return new JSPromise<>((resolve, reject) -> {

			int initialQueuedBytes = backend.getQueuedBytes();
			int initialQueuedFrames = initialQueuedBytes / frameSizeBytes;
			if (initialQueuedFrames == 0) {
				resolve.accept(0);
				return;
			}

			if (framesToDrain < 0) {
				backend.addDrainListener((bytesDrained, bytesRemaining) -> {
					if (bytesRemaining == 0) {
						resolve.accept(initialQueuedFrames);
						return true;
					}
					return false;
				});
				return;
			}
			AtomicInteger remainingBytes = new AtomicInteger(framesToDrain * frameSizeBytes);
			backend.addDrainListener((bytesDrained, bytesRemaining) -> {
				int rem = remainingBytes.addAndGet(-bytesDrained);

				if (rem <= 0) {
					int framesDrained = (framesToDrain * frameSizeBytes - rem) / frameSizeBytes;
					resolve.accept(framesDrained);
					return true; // remove listener
				}

				if (bytesRemaining == 0) {
					resolve.accept(initialQueuedFrames);
					return true; // remove listener
				}

				return false;
			});
		});
	}
}
