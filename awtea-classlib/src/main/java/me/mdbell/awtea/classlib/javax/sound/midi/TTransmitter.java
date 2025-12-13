package me.mdbell.awtea.classlib.javax.sound.midi;

/**
 * @see javax.sound.midi.Transmitter
 */
public interface TTransmitter extends AutoCloseable {

    public void setReceiver(TReceiver receiver);
}
