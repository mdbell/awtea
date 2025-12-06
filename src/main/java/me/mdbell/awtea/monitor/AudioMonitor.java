package me.mdbell.awtea.monitor;

import me.mdbell.awtea.sound.AbstractDataLine;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class AudioMonitor {

	private static final AudioMonitor INSTANCE = new AudioMonitor();

	public static AudioMonitor get() {
		return INSTANCE;
	}

	/** How much "slack" we want in the buffer, in ms, for backlog calculation. */
	private static final int TARGET_SLACK_MS = 40;

	public static final class LineSnapshot {
		public final int id;
		public final String name;
		public final boolean output;
		public final boolean open;
		public final boolean running;
		public final AudioFormat format;

		public final int bufferSizeBytes;
		public final int usedBytes;
		public final int freeBytes;

		public final long totalWrittenBytes;

		public final double writeRateBytesPerSec;
		public final double drainRateBytesPerSec;

		/** How many bytes above the "target slack" we are. */
		public final int backlogBytes;
		/** Target slack in bytes (for given format). */
		public final int targetSlackBytes;

		LineSnapshot(
			int id,
			String name,
			boolean output,
			boolean open,
			boolean running,
			AudioFormat format,
			int bufferSizeBytes,
			int usedBytes,
			int freeBytes,
			long totalWrittenBytes,
			double writeRateBytesPerSec,
			double drainRateBytesPerSec,
			int backlogBytes,
			int targetSlackBytes
		) {
			this.id = id;
			this.name = name;
			this.output = output;
			this.open = open;
			this.running = running;
			this.format = format;
			this.bufferSizeBytes = bufferSizeBytes;
			this.usedBytes = usedBytes;
			this.freeBytes = freeBytes;
			this.totalWrittenBytes = totalWrittenBytes;
			this.writeRateBytesPerSec = writeRateBytesPerSec;
			this.drainRateBytesPerSec = drainRateBytesPerSec;
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

	private static final class LineInfo {
		final AbstractDataLine line;
		final String name;
		final boolean output;
		final int id;

		long totalWrittenBytes;
		int lastUsedBytes;
		int lastFreeBytes;

		long lastWriteTimeMs;
		double writeRateBytesPerSec;

		long lastDrainTimeMs;
		double drainRateBytesPerSec;

		LineInfo(AbstractDataLine line, String name, boolean output, int id) {
			this.line = line;
			this.name = name;
			this.output = output;
			this.id = id;
		}
	}

	private final Map<AbstractDataLine, LineInfo> byLine = new IdentityHashMap<>();
	private final List<LineInfo> lines = new ArrayList<>();

	private final Map<AbstractDataLine, PcmEnvelopeBuffer> pcmBuffers = new IdentityHashMap<>();

	private int nextId = 1;

	private AudioMonitor() {}

	public void registerOutputLine(AbstractDataLine line) {
		registerOutputLine(line, line.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(line)));
	}

	public synchronized void registerOutputLine(AbstractDataLine line, String name) {
		if (byLine.containsKey(line)) {
			return;
		}
		int id = nextId++;
		LineInfo info = new LineInfo(line, name, true, id);
		byLine.put(line, info);
		lines.add(info);

		AudioFormat format = line.getFormat();

		int channels = format.getChannels();
		int slots = 256; // ~256 time slices per line
		pcmBuffers.put(line, new PcmEnvelopeBuffer(info.id, true, name, channels, slots));
	}

	public synchronized void unregisterLine(AbstractDataLine line) {
		LineInfo info = byLine.remove(line);
		if (info != null) {
			lines.remove(info);
		}
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
		LineInfo info = byLine.get(line);
		if (info == null) {
			return;
		}

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
		LineInfo info = byLine.get(line);
		if (info == null) {
			return;
		}

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

	public synchronized List<LineSnapshot> snapshot() {
		List<LineSnapshot> result = new ArrayList<>(lines.size());
		for (LineInfo info : lines) {
			AbstractDataLine line = info.line;

			AudioFormat fmt = line.getFormat();
			int bufferSize = 0;
			int used = info.lastUsedBytes;
			int free = info.lastFreeBytes;

			try {
				bufferSize = line.getBufferSize();
				// If lastUsedBytes/lastFreeBytes are 0, we can lazily compute:
				if (bufferSize > 0 && used == 0 && free == 0) {
					free = line.available();
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
					targetSlackBytes = (int) (bytesPerSecond * TARGET_SLACK_MS / 1000.0);
				}
			}
			int backlogBytes = 0;
			if (targetSlackBytes > 0) {
				backlogBytes = Math.max(0, used - targetSlackBytes);
			}

			result.add(new LineSnapshot(
				info.id,
				info.name,
				info.output,
				line.isOpen(),
				line.isActive(),
				fmt,
				bufferSize,
				used,
				free,
				info.totalWrittenBytes,
				info.writeRateBytesPerSec,
				info.drainRateBytesPerSec,
				backlogBytes,
				targetSlackBytes
			));
		}
		return result;
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
