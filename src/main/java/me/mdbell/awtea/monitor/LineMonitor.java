package me.mdbell.awtea.monitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.sound.AbstractDataLine;

import javax.sound.sampled.AudioFormat;

public final class LineMonitor extends AbstractMonitor<LineMonitor.Entry,
	LineMonitor.Snapshot> {

	private static final LineMonitor INSTANCE = new LineMonitor();

	public static LineMonitor get() {
		return INSTANCE;
	}

	/** How much "slack" we want in the buffer, in ms, for backlog calculation. */
	public static final int DEFAULT_TARGET_SLACK_MS = 100;

	@Getter
	@Setter
	private int targetSlackMs = DEFAULT_TARGET_SLACK_MS;

	@Getter
	@Setter(AccessLevel.PACKAGE)
	public static final class Entry extends MonitorEntry {

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

		Entry(int id, String label) {
			super(id, label);
		}
	}

	@Getter
	public static final class Snapshot extends MonitorSnapshot<Entry> {
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

		Snapshot(Entry entry, int backlogBytes, int targetSlackBytes) {
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
		Entry info = ensureEntry(line);
		info.setOutput(true);
		info.setFormat(line.getFormat());
		info.setOpen(line.isOpen());
		info.setRunning(line.isActive());
		info.setBufferSizeBytes(line.getBufferSize());
	}

	public void onStart(Object line) {
		Entry e = ensureEntry(line);
		e.setRunning(true);
	}

	public void onStop(Object line) {
		Entry e = ensureEntry(line);
		e.setRunning(false);
	}

	public void onAvailable(AbstractDataLine abstractDataLine, int available) {
		Entry info = ensureEntry(abstractDataLine);
		info.setAvailable(available);
	}

	public void onFlush(Object line) {
		Entry e = ensureEntry(line);
		// Reset write/drain rates
		e.writeRateBytesPerSec = 0;
		e.drainRateBytesPerSec = 0;
	}

	public synchronized void onWrite(AbstractDataLine line, int bytesPushed) {
		Entry info = ensureEntry(line);

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
		Entry info = ensureEntry(line);

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
	protected Entry createEntry(int id, Object target, String label) {
		return new Entry(id, label);
	}

	@Override
	protected Snapshot buildSnapshot(Entry info) {

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

		return new Snapshot(info,
			backlogBytes,
			targetSlackBytes
		);
	}
}
