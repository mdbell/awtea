package me.mdbell.awtea.sound.pcm.messages;

/**
 * Keepalive acknowledgment message sent from worklet to main thread.
 * Type: "keepalive-ack"
 */
public interface KeepaliveAckMessage extends LineMessage {
    // No additional properties - type is sufficient
}
