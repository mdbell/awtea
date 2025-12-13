package me.mdbell.awtea.classlib.javax.sound.midi;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mdbell.awtea.sound.AudioConstants;
import me.mdbell.awtea.sound.AudioUtils;
import me.mdbell.awtea.sound.midi.MIDI;

/**
 * @see javax.sound.midi.Sequencer
 */
public class TMidiJsSequencer implements TReceiver, TSequencer, TTransmitter, MIDI.MidiCallbacks, AudioConstants {

    @Getter
    private boolean connected;

    @Getter
    @Setter
    private TReceiver receiver;

    @Getter
    @Setter
    private byte[] buffer;

    @Getter
    @Setter
    private int loopCount = 0;

    private MIDI midi;

    private final Channel[] channels = new Channel[16];

    public TMidiJsSequencer(boolean connected) {
        this.connected = connected;
        if (connected) {
            open();
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel();
        }
    }

    @Override
    public void start() {
        midi.play(this.buffer, MIDI.REUSE_VOLUME);
    }

    @Override
    public void open() {
        this.midi = MIDI.create(this);
        connected = true;
    }

    @Override
    public void stop() {
        midi.stop();
    }

    @Override
    public void setSequence(TSequence sequence) {
        this.setBuffer(sequence.getData());
    }

    @Override
    public TTransmitter getTransmitter() {
        return this;
    }

    @Override
    public void close() throws Exception {
        connected = false;
        stop();
        midi.close();
        midi = null;
    }

    @SneakyThrows
    @Override
    public void onEnd() {
        if (loopCount > 0 || loopCount == TSequencer.LOOP_CONTINUOUSLY) {
            midi.play(this.buffer, MIDI.REUSE_VOLUME);
        }

        if (loopCount > 0) {
            loopCount--;
        }
    }

    @Override
    public void send(TMidiMessage message, long timeStamp) {
        TShortMessage msg = (TShortMessage) message;
        switch (msg.getCommand()) {
            case TShortMessage.CONTROL_CHANGE:
                handleControlChange(msg.getChannel(), msg.getData1(), msg.getData2());
                break;
            case TShortMessage.PROGRAM_CHANGE:
                channels[msg.getChannel()].setProgram(msg.getData1());
                break;
            default:
                System.err.println("[MIDI] Unhandled message: " + msg.getCommand());
        }
    }

    private void handleControlChange(int channel, int ctrlNum, int value) {
        switch (ctrlNum) {
            case 0x0: // bank select (MSB)
                channels[channel].setBankMSB(value);
                break;
            case 0x7: // volume (MSB)
                channels[channel].setVolumeMSB(value);
                midi.setVolume(calculateVolume());
                break;
            case 0x20: // bank select (LSB)
                channels[channel].setBankLSB(value);
                break;
            case 0x27: // volume (LSB)
                channels[channel].setVolumeLSB(value);
                midi.setVolume(calculateVolume());
                break;
            case 0x78: // all sound off
                channels[channel].setOn(false);
                for (Channel ch : channels) {
                    if (ch.isOn()) {
                        return;
                    }
                }
                midi.stop();
                break;
            case 0x79:
                channels[channel].setOn(true);
                break;
            case 0x7b: // all notes off
                midi.stop();
                break;
            default:
                System.err.println("[MIDI] Unhandled Control Change: " + channel + " " + ctrlNum + " " + value);
        }
    }

    /**
     * This method will average the volume of all channels and then
     * calculate the corresponding volume that can be used with the GainNode
     *
     * @return the volume
     */
    private float calculateVolume() {
        // we average the volume of all channels, since we can't set the volume of individual channels
        int volume = 0;
        for (Channel channel : channels) {
            volume += channel.getVolume();
        }

        if (volume == 0) {
            return 0;
        }
        return AudioUtils.normalizeMidiVolume(volume / channels.length);
    }

    @Getter
    private static class Channel {

        @Setter
        private int program;

        private int bank;

        private int volume;

        @Setter
        private boolean on;

        public Channel() {
            reset();
        }

        public void setBankMSB(int bank) {
            this.bank = (this.bank & 0x7f) | (bank << 7);
        }

        public void setBankLSB(int bank) {
            this.bank = (this.bank & 0x3f80) | bank;
        }

        public void setVolumeMSB(int volume) {
            this.volume = (this.volume & 0x7f) | (volume << 7);
        }

        public void setVolumeLSB(int volume) {
            this.volume = (this.volume & 0x3f80) | volume;
        }

        public void reset() {
            volume = MAX_MIDI_VOLUME;
            on = true;
        }
    }
}
