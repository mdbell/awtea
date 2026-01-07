package me.mdbell.awtea.sound.pcm.messages;

/**
 * Keepalive ping message sent from main thread to worklet.
 * Type: "keepalive"
 */
public interface KeepaliveMessage extends LineMessage {
    // No additional properties - type is sufficient
}
