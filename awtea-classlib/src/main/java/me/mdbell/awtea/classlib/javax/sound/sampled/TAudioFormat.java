package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * @see javax.sound.sampled.AudioFormat
 */
@Getter
@ToString
@AllArgsConstructor
public class TAudioFormat {

	protected Encoding encoding;

	private final float sampleRate;
	private final int sampleSizeInBits;
	private final int channels;
	private final int frameSize;
	private final float frameRate;
	private final boolean bigEndian;

	public TAudioFormat(Encoding encoding, float sampleRate,
						int sampleSizeInBits, int channels,
						int frameSize, float frameRate,
						boolean bigEndian, Map<String, Object> properties) {
		this(encoding, sampleRate, sampleSizeInBits, channels,
			frameSize, frameRate, bigEndian);
		//this.properties = new HashMap<>(properties);
	}

	public TAudioFormat(float sampleRate, int sampleSizeInBits,
						int channels, boolean signed, boolean bigEndian) {

		this((signed ? TAudioFormat.Encoding.PCM_SIGNED : TAudioFormat.Encoding.PCM_UNSIGNED),
			sampleRate,
			sampleSizeInBits,
			channels,
			(channels == TAudioSystem.NOT_SPECIFIED || sampleSizeInBits == TAudioSystem.NOT_SPECIFIED) ?
				TAudioSystem.NOT_SPECIFIED :
				((sampleSizeInBits + 7) / 8) * channels,
			sampleRate,
			bigEndian);
	}

	public boolean matches(TAudioFormat format) {
		return format.getEncoding().equals(getEncoding())
			&& (format.getChannels() == TAudioSystem.NOT_SPECIFIED
			|| format.getChannels() == getChannels())
			&& (format.getSampleRate() == (float) TAudioSystem.NOT_SPECIFIED
			|| format.getSampleRate() == getSampleRate())
			&& (format.getSampleSizeInBits() == TAudioSystem.NOT_SPECIFIED
			|| format.getSampleSizeInBits() == getSampleSizeInBits())
			&& (format.getFrameRate() == (float) TAudioSystem.NOT_SPECIFIED
			|| format.getFrameRate() == getFrameRate())
			&& (format.getFrameSize() == TAudioSystem.NOT_SPECIFIED
			|| format.getFrameSize() == getFrameSize())
			&& (getSampleSizeInBits() <= 8
			|| format.isBigEndian() == isBigEndian());
	}

	@AllArgsConstructor
	@Getter
	@ToString
	@EqualsAndHashCode
	public static class Encoding {
		public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
		public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");
		public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");
		public static final Encoding ULAW = new Encoding("ULAW");
		public static final Encoding ALAW = new Encoding("ALAW");

		private final String name;
	}

}

