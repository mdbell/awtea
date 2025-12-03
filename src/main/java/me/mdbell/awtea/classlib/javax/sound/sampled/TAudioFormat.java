package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.HashMap;
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
                (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED)?
                        AudioSystem.NOT_SPECIFIED:
                        ((sampleSizeInBits + 7) / 8) * channels,
                sampleRate,
                bigEndian);
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

