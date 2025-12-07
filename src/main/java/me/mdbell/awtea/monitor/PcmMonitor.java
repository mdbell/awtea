package me.mdbell.awtea.monitor;

import lombok.Getter;
import me.mdbell.awtea.sound.AbstractDataLine;

public class PcmMonitor extends AbstractMonitor<PcmMonitor.Entry, PcmMonitor.Snapshot>{

	private static PcmMonitor instance = new PcmMonitor();

	public static PcmMonitor get() {
		return instance;
	}

	private PcmMonitor() {

	}

	public void onPcmEnvelope(Object target, float[] peaks) {
		Entry entry = ensureEntry(target);
		entry.push(peaks);
	}

	public void onClose(AbstractDataLine line) {
		unregister(line);
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		AbstractDataLine line = (AbstractDataLine) target;
		boolean output = true; //line instanceof TSourceDataLine;
		int channels = line.getFormat().getChannels();
		// For now, we fix the capacity to 256 frames
		return new Entry(id, label, output, channels, 256);
	}

	@Override
	protected Snapshot buildSnapshot(Entry entry) {
		return new Snapshot(entry);
	}

	@Getter
	public static class Entry extends MonitorEntry {
		private final boolean output;
		private final int channels;

		final float[] data;
		final int capacity;       // number of time slots
		int writePos = 0;
		boolean filled = false;

		Entry(int id, String label, boolean output, int channels, int capacity) {
			super(id, label);
			this.output = output;
			this.channels = channels;
			this.capacity = capacity;
			this.data = new float[channels * capacity];
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
	}

	@Getter
	public static class Snapshot extends MonitorSnapshot<Entry> {
		private final int channels;
		private final float[] peaks;   // [time][channel] flattened
		private final int length;      // number of time slots

		public Snapshot(Entry e) {
			super(e);

			boolean filled = e.isFilled();
			this.channels = e.getChannels();
			int capacity = e.getCapacity();
			int writePos = e.getWritePos();

			this.length = e.isFilled() ? e.getCapacity() : e.getWritePos();
			this.peaks = new float[length * e.getChannels()];

			// de-ring into chronological order: oldest -> newest
			int start = filled ? writePos : 0;
			for (int i = 0; i < length; i++) {
				int srcIdx = ((start + i) % capacity) * channels;
				int dstIdx = i * channels;
				System.arraycopy(e.getData(), srcIdx, peaks, dstIdx, channels);
			}
		}
	}
}
