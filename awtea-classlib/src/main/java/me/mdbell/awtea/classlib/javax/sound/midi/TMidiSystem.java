package me.mdbell.awtea.classlib.javax.sound.midi;

import lombok.SneakyThrows;

import java.io.InputStream;

/**
 * @see javax.sound.midi.MidiSystem
 */
public class TMidiSystem {

    private static final TMidiJsSequencer sequencer = new TMidiJsSequencer(false);

    public static TReceiver getReceiver() {
        return sequencer;
    }


    @SneakyThrows
    public static TSequence getSequence(InputStream in) {
        return new TSequence(in);
    }

    public static TSequencer getSequencer(boolean connected) {
        return sequencer;
    }
}
