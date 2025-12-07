package me.mdbell.awtea.monitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.sound.AbstractDataLine;

import javax.sound.sampled.AudioFormat;
import java.util.*;
import java.util.function.Supplier;

public final class LineMonitor extends AbstractMonitor<LineMonitor.LineEntry,
	LineMonitor.LineSnapshot> {

	private static final LineMonitor INSTANCE = new LineMonitor();

	public static LineMonitor get() {
		return INSTANCE;
	}

	/** How much "slack" we want in the buffer, in ms, for backlog calculation. */
	public static final int DEFAULT_TARGET_SLACK_MS = 100;

	@Getter
	@Setter
	private int targetSlackMs = DEFAULT_TARGET_SLACK_MS;

	private final Map<Object, PcmEnvelopeBuffer> pcmBuffers = new WeakHashMap<>();

	@Getter
	@Setter(AccessLevel.PACKAGE)
	public static final class LineEntry extends MonitorEntry {

		private int available;

		private boolean output;
		private boolean open;
		private boolean running;
		private AudioFormat format;

		private int bufferSizeBytes;

		private long totalWrittenBytes;
		private int lastUsedBytes;
		private int lastFreeBytes;

		private long lastWriteTimeMs;
		private double writeRateBytesPerSec;

		private long lastDrainTimeMs;
		private double drainRateBytesPerSec;

		LineEntry(int id, String label) {
			super(id, label);
		}
	}

	@Getter
	public static final class LineSnapshot extends MonitorSnapshot<LineEntry> {
		private final boolean output;
		private final boolean open;
		private final boolean running;
		private final AudioFormat format;

		private final int bufferSizeBytes;
		private final int usedBytes;
		private final int freeBytes;

		private final long totalWrittenBytes;

		private final double writeRateBytesPerSec;
		private final double drainRateBytesPerSec;

		/** How many bytes above the "target slack" we are. */
		private final int backlogBytes;
		/** Target slack in bytes (for given format). */
		private final int targetSlackBytes;

		LineSnapshot(LineEntry entry, int backlogBytes, int targetSlackBytes) {
			super(entry);
			this.output = entry.output;
			this.open = entry.isOpen();
			this.running = entry.isActive();
			this.format = entry.getFormat();
			this.bufferSizeBytes = entry.getBufferSizeBytes();
			this.usedBytes = entry.lastUsedBytes;
			this.freeBytes = entry.lastFreeBytes;
			this.totalWrittenBytes = entry.totalWrittenBytes;
			this.writeRateBytesPerSec = entry.writeRateBytesPerSec;
			this.drainRateBytesPerSec = entry.drainRateBytesPerSec;
			this.backlogBytes = backlogBytes;
			this.targetSlackBytes = targetSlackBytes;
		}

		public int fillPercent() {
			if (bufferSizeBytes <= 0) return 0;
			return (int) Math.round(usedBytes * 100.0 / bufferSizeBytes);
		}

		public String formatSummary() {
			if (format == null) return "(unknown)";
			return format.getSampleRate() + " Hz, " +
				format.getSampleSizeInBits() + " bit, " +
				format.getChannels() + " ch";
		}
	}

	private LineMonitor() {}

	public synchronized void registerOutputLine(AbstractDataLine line) {
		LineEntry info = ensureEntry(line);
		info.setOutput(true);
		info.setFormat(line.getFormat());
		info.setOpen(line.isOpen());
		info.setRunning(line.isActive());
		info.setBufferSizeBytes(line.getBufferSize());

		AudioFormat format = line.getFormat();

		int channels = format.getChannels();
		int slots = 256; // ~256 time slices per line
		pcmBuffers.put(line, new PcmEnvelopeBuffer(info.getId(), info.isOutput(), info.getLabel()
			, channels, slots));
	}

	public void onStart(Object line) {
		LineEntry e = ensureEntry(line);
		e.setRunning(true);
	}

	public void onStop(Object line) {
		LineEntry e = ensureEntry(line);
		e.setRunning(false);
	}

	public void onAvailable(AbstractDataLine abstractDataLine, int available) {
		LineEntry info = ensureEntry(abstractDataLine);
		info.setAvailable(available);
	}

	public void onClose(Object line) {
		LineEntry e = ensureEntry(line);
		e.setOpen(false);
		e.setRunning(false);

		pcmBuffers.remove(line);
	}

	public void onFlush(Object line) {
		LineEntry e = ensureEntry(line);
		// Reset write/drain rates
		e.writeRateBytesPerSec = 0;
		e.drainRateBytesPerSec = 0;
	}

	public void onPcmEnvelope(AbstractDataLine line, float[] peaks) {
		PcmEnvelopeBuffer buf = pcmBuffers.get(line);
		if (buf == null) {
			return;
		}
		buf.push(peaks);
	}

	public List<PcmSnapshot> snapshotPcm() {
		List<PcmSnapshot> res = new java.util.ArrayList<>();
		for (PcmEnvelopeBuffer buf : pcmBuffers.values()) {
			res.add(buf.snapshot());
		}
		return res;
	}


	public synchronized void onWrite(AbstractDataLine line, int bytesPushed) {
		LineEntry info = ensureEntry(line);

		info.totalWrittenBytes += bytesPushed;

		long now = System.currentTimeMillis();
		if (info.lastWriteTimeMs != 0 && bytesPushed > 0) {
			long dt = now - info.lastWriteTimeMs;
			if (dt > 0) {
				double instRate = (bytesPushed * 1000.0) / dt;
				double alpha = 0.2; // EMA smoothing
				info.writeRateBytesPerSec =
					info.writeRateBytesPerSec * (1.0 - alpha) + instRate * alpha;
			}
		}
		info.lastWriteTimeMs = now;

		// Update used/free snapshot
		try {
			int bufferSize = line.getBufferSize();
			int free = line.available();
			if (free < 0) free = 0;
			int used = Math.max(0, bufferSize - free);
			info.lastFreeBytes = free;
			info.lastUsedBytes = used;
		} catch (Throwable ignored) {
		}
	}

	/** Optional: call from backend when bytes are actually drained. */
	public synchronized void onDrain(AbstractDataLine line, int bytesDrained) {
		LineEntry info = ensureEntry(line);

		long now = System.currentTimeMillis();
		if (info.lastDrainTimeMs != 0 && bytesDrained > 0) {
			long dt = now - info.lastDrainTimeMs;
			if (dt > 0) {
				double instRate = (bytesDrained * 1000.0) / dt;
				double alpha = 0.2;
				info.drainRateBytesPerSec =
					info.drainRateBytesPerSec * (1.0 - alpha) + instRate * alpha;
			}
		}
		info.lastDrainTimeMs = now;

		// You can optionally refresh used/free here as well, if you have easy access
		try {
			int bufferSize = line.getBufferSize();
			int free = line.available();
			if (free < 0) free = 0;
			int used = Math.max(0, bufferSize - free);
			info.lastFreeBytes = free;
			info.lastUsedBytes = used;
		} catch (Throwable ignored) {
		}
	}

	@Override
	protected LineEntry createEntry(int id, Object target, String label) {
		return new LineEntry(id, label);
	}

	@Override
	protected LineSnapshot buildSnapshot(LineEntry info) {

		AudioFormat fmt = info.getFormat();
		int bufferSize = 0;
		int used = info.lastUsedBytes;
		int free = info.lastFreeBytes;

		try {
			bufferSize = info.getBufferSizeBytes();
			// If lastUsedBytes/lastFreeBytes are 0, we can lazily compute:
			if (bufferSize > 0 && used == 0 && free == 0) {
				free = info.getAvailable();
				if (free < 0) free = 0;
				used = Math.max(0, bufferSize - free);
			}
		} catch (Throwable ignored) {
		}

		// Compute target slack in bytes from format
		int targetSlackBytes = 0;
		if (fmt != null) {
			int sampleBytes = fmt.getSampleSizeInBits() / 8;
			if (sampleBytes > 0) {
				double bytesPerSecond =
					fmt.getSampleRate() * fmt.getChannels() * sampleBytes;
				targetSlackBytes = (int) (bytesPerSecond * getTargetSlackMs() / 1000.0);
			}
		}

		int backlogBytes = 0;
		if (targetSlackBytes > 0) {
			backlogBytes = Math.max(0, used - targetSlackBytes);
		}

		return new LineSnapshot(info,
			backlogBytes,
			targetSlackBytes
		);
	}

	// inside AudioMonitor

	public static final class PcmEnvelopeBuffer {
		final int id;
		final boolean output;
		final String name;
		final int channels;

		// ring buffer of [time][channel] peaks, flattened
		final float[] data;
		final int capacity;       // number of time slots
		int writePos = 0;
		boolean filled = false;

		PcmEnvelopeBuffer(int id, boolean output, String name, int channels, int capacity) {
			this.id = id;
			this.output = output;
			this.name = name;
			this.channels = channels;
			this.capacity = capacity;
			this.data = new float[capacity * channels];
		}

		void push(float[] peaks) {
			int base = writePos * channels;
			for (int ch = 0; ch < channels; ch++) {
				float v = ch < peaks.length ? peaks[ch] : 0f;
				// clamp to [0,1]
				if (v < 0f) v = 0f;
				if (v > 1f) v = 1f;
				data[base + ch] = v;
			}
			writePos = (writePos + 1) % capacity;
			if (writePos == 0) {
				filled = true;
			}
		}

		PcmSnapshot snapshot() {
			int len = filled ? capacity : writePos;
			float[] out = new float[len * channels];

			// de-ring into chronological order: oldest -> newest
			int start = filled ? writePos : 0;
			for (int i = 0; i < len; i++) {
				int srcIdx = ((start + i) % capacity) * channels;
				int dstIdx = i * channels;
				System.arraycopy(data, srcIdx, out, dstIdx, channels);
			}

			return new PcmSnapshot(id, output, name, channels, out, len);
		}
	}

	public static final class PcmSnapshot {
		public final int id;
		public final boolean output;
		public final String name;
		public final int channels;
		public final float[] peaks;   // [time][channel] flattened
		public final int length;      // number of time slots

		public PcmSnapshot(int id, boolean output, String name,
						   int channels, float[] peaks, int length) {
			this.id = id;
			this.output = output;
			this.name = name;
			this.channels = channels;
			this.peaks = peaks;
			this.length = length;
		}
	}

}
