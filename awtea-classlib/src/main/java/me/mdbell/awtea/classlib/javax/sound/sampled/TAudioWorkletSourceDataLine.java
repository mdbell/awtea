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

		backend.init();

		backend.addDrainListener(((framesDrained, framesRemaining) -> {
			LineMonitor.get().onDrain(this, framesDrained * frameSizeBytes);
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

	@Override
	protected int drainInternal(int framesToDrain) {
		return drainAsync(framesToDrain).await();
	}

	private JSPromise<Integer> drainAsync(int framesToDrain) {
		if (backend == null || !isActive()) {
			return JSPromise.resolve(0);
		}
		return new JSPromise<>((resolve, reject) -> {

			int initialQueued = backend.getQueuedFrames();
			if (initialQueued == 0) {
				resolve.accept(0);
				return;
			}

			if (framesToDrain < 0) {
				backend.addDrainListener((framesDrained, framesRemaining) -> {
					if (framesRemaining == 0) {
						resolve.accept(initialQueued);
						return true;
					}
					return false;
				});
				return;
			}
			AtomicInteger remaining = new AtomicInteger(framesToDrain);
			backend.addDrainListener((framesDrained, framesRemaining) -> {
				int rem = remaining.addAndGet(-framesDrained);

				if (rem <= 0) {
					resolve.accept(framesToDrain - rem);
					return true; // remove listener
				}

				if (framesRemaining == 0) {
					resolve.accept(initialQueued);
					return true; // remove listener
				}

				return false;
			});
		});
	}
}
