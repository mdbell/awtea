package me.mdbell.awtea.sound.pcm.messages;

/**
 * Timeout notification message sent from worklet to main thread when idle timeout is reached.
 * Type: "timeout"
 */
public interface TimeoutMessage extends LineMessage {
    // No additional properties - type is sufficient
}
