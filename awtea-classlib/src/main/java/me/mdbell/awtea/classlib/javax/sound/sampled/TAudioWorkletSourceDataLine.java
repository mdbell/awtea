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
	protected void openBackend(int bufferBytes) {
		// Calculate max queued frames from buffer bytes
		int bufferFrames = bufferBytes / frameSizeBytes;
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
	protected int getFreeBytes() {
		if (backend == null || !isActive()) {
			return 0;
		}
		int queuedBytes = backend.getQueuedBytes();
		int maxBytes = backend.getMaxQueuedBytes();
		return Math.max(0, maxBytes - queuedBytes);
	}

	@Override
	protected int getMaxBytes() {
		return backend != null ? backend.getMaxQueuedBytes() : maxQueuedFrames * frameSizeBytes;
	}

	@Override
	protected int enqueue(byte[] bytes, int offset, int length) {
		if (backend == null) return 0;
		int frames = length / frameSizeBytes;
		int framesEnqueued = backend.enqueue(bytes, offset, frames);
		return framesEnqueued * frameSizeBytes;
	}

	@Override
	protected int drainInternal(int bytesToDrain) {
		return drainAsync(bytesToDrain).await();
	}

	private JSPromise<Integer> drainAsync(int bytesToDrain) {
		if (backend == null || !isActive()) {
			return JSPromise.resolve(0);
		}
		return new JSPromise<>((resolve, reject) -> {

			int initialQueuedBytes = backend.getQueuedBytes();
			if (initialQueuedBytes == 0) {
				resolve.accept(0);
				return;
			}

			if (bytesToDrain < 0) {
				backend.addDrainListener((bytesDrained, bytesRemaining) -> {
					if (bytesRemaining == 0) {
						resolve.accept(initialQueuedBytes);
						return true;
					}
					return false;
				});
				return;
			}
			AtomicInteger remainingBytes = new AtomicInteger(bytesToDrain);
			backend.addDrainListener((bytesDrained, bytesRemaining) -> {
				int rem = remainingBytes.addAndGet(-bytesDrained);

				if (rem <= 0) {
					int totalDrained = bytesToDrain - rem;
					resolve.accept(totalDrained);
					return true; // remove listener
				}

				if (bytesRemaining == 0) {
					resolve.accept(initialQueuedBytes);
					return true; // remove listener
				}

				return false;
			});
		});
	}
}
