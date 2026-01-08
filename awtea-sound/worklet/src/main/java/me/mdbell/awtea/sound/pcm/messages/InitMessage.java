package me.mdbell.awtea.sound.pcm.messages;

import org.teavm.jso.JSProperty;

/**
 * Initialization message containing audio format metadata.
 * Type: "init"
 */
public interface InitMessage extends LineMessage {

    /**
     * Number of audio channels (e.g., 1 for mono, 2 for stereo).
     * @return channel count
     */
    @JSProperty("channels")
    int getChannels();

    /**
     * Sets the number of audio channels.
     * @param channels channel count
     */
    @JSProperty("channels")
    void setChannels(int channels);

    /**
     * Sample rate in Hz (e.g., 44100, 48000).
     * @return sample rate
     */
    @JSProperty("sampleRate")
    int getSampleRate();

    /**
     * Sets the sample rate.
     * @param sampleRate sample rate in Hz
     */
    @JSProperty("sampleRate")
    void setSampleRate(int sampleRate);

    /**
     * Sample size in bits (e.g., 8, 16, 24, 32).
     * @return sample size in bits
     */
    @JSProperty("sampleSizeBits")
    int getSampleSizeBits();

    /**
     * Sets the sample size in bits.
     * @param sampleSizeBits sample size in bits
     */
    @JSProperty("sampleSizeBits")
    void setSampleSizeBits(int sampleSizeBits);

    /**
     * Whether the PCM data is big-endian (true) or little-endian (false).
     * @return true if big-endian
     */
    @JSProperty("bigEndian")
    boolean isBigEndian();

    /**
     * Sets the endianness.
     * @param bigEndian true if big-endian
     */
    @JSProperty("bigEndian")
    void setBigEndian(boolean bigEndian);
}
