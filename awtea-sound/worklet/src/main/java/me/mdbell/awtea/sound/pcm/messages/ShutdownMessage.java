package me.mdbell.awtea.sound.pcm.messages;

/**
 * Shutdown command message sent from main thread to worklet.
 * Type: "shutdown"
 */
public interface ShutdownMessage extends LineMessage {
    // No additional properties - type is sufficient
}
