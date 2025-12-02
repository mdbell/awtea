package me.mdbell.awtea.classlib.javax.sound.midi;

/**
 * @see javax.sound.midi.Sequencer
 */
public interface TSequencer extends AutoCloseable {

    public static int LOOP_CONTINUOUSLY = -1;

    void setLoopCount(int count);

    void start();

    void open();

    void stop();

    void setSequence(TSequence sequence);

    TTransmitter getTransmitter();
}
