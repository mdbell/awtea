package me.mdbell.awtea.sound.pcm.messages;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Base interface for all messages passed between the main thread and audio worklet.
 */
public interface LineMessage extends JSObject {

    /**
     * Message type discriminator.
     * @return the message type
     */
    @JSProperty("type")
    String getType();

    /**
     * Sets the message type.
     * @param type the message type
     */
    @JSProperty("type")
    void setType(String type);
}
