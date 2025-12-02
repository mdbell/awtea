package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TAudioFormat {

    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final boolean signed;
    private final boolean bigEndian;

    public TAudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public int getFrameSize() {
        return (sampleSizeInBits + 7) / 8 * channels;
    }

}

