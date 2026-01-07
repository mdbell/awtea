package me.mdbell.awtea.sound.pcm.messages;

import org.teavm.jso.JSProperty;

/**
 * Message reporting how many bytes have been consumed by the worklet.
 * Type: "consumed"
 */
public interface ConsumedMessage extends LineMessage {

    /**
     * Number of bytes consumed from the queue.
     * @return bytes consumed
     */
    @JSProperty("bytes")
    int getBytes();

    /**
     * Sets the number of bytes consumed.
     * @param bytes bytes consumed
     */
    @JSProperty("bytes")
    void setBytes(int bytes);
}
