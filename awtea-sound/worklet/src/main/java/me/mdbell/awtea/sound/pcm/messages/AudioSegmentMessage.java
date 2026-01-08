package me.mdbell.awtea.sound.pcm.messages;

import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * Message containing raw PCM audio data to be processed by the worklet.
 * Type: "pcm"
 */
public interface AudioSegmentMessage extends LineMessage {

    /**
     * Raw PCM byte data.
     * @return the audio data buffer
     */
    @JSProperty("data")
    ArrayBuffer getData();

    /**
     * Sets the raw PCM byte data.
     * @param data the audio data buffer
     */
    @JSProperty("data")
    void setData(ArrayBuffer data);

    /**
     * Number of audio frames in this segment.
     * @return frame count
     */
    @JSProperty("frames")
    int getFrames();

    /**
     * Sets the number of frames.
     * @param frames frame count
     */
    @JSProperty("frames")
    void setFrames(int frames);
}
