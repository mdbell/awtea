package me.mdbell.awtea.classlib.javax.sound.midi;

public interface TReceiver extends AutoCloseable {

    /**
     * Sends a MIDI message and time-stamp to this receiver. If time-stamping is
     * not supported by this receiver, the time-stamp value should be -1.
     *
     * @param message   the MIDI message to send
     * @param timeStamp the time-stamp for the message, in microseconds
     * @throws IllegalStateException if the receiver is closed
     */
    void send(TMidiMessage message, long timeStamp);
}
