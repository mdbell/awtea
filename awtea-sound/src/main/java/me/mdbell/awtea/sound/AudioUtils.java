package me.mdbell.awtea.sound;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;

@UtilityClass
public class AudioUtils implements AudioConstants {

    public String[] LISTEN_EVENTS = new String[]{
            "touchstart",
            "touchend",
            "click"
    };

    @Getter
    private final AudioContext globalContext = new AudioContext();
    /**
     * The global gain node for all audio.
     * <p>
     * Allows us to have a 'master volume' for all audio.
     * </p>
     */
    @Getter
    private final GainNode globalGain;

    static {
        AudioUtils.registerFix();
        globalGain = globalContext.createGain();
        globalGain.getGain().setValueAtTime(MASTER_GAIN, globalContext.getCurrentTime());
        globalGain.connect(globalContext.getDestination());
    }

    /**
     * Reads a sample from a buffer, at a given offset.
     *
     * @param buffer           The buffer to read from.
     * @param offset           The offset to read from.
     * @param sampleSizeInBits The size of the sample in bits.
     * @param bigEndian        Whether the sample is big endian.
     * @return The sample.
     */
    public int getSample(byte[] buffer, int offset, int sampleSizeInBits, boolean bigEndian) {
        int res = 0;
        if (bigEndian) {
            if (sampleSizeInBits == 16) {
                res = (buffer[offset] << 8) | (buffer[offset + 1] & 0xFF);
            } else if (sampleSizeInBits == 24) {
                res = (buffer[offset] << 16) | ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset + 2] & 0xFF);
            }
        } else {
            if (sampleSizeInBits == 16) {
                res = (buffer[offset + 1] << 8) | (buffer[offset] & 0xFF);
            } else if (sampleSizeInBits == 24) {
                res = (buffer[offset + 2] << 16) | ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset] & 0xFF);
            }
        }
        return res;
    }

    /**
     * We can't have an audio context without user interaction, unless
     * we create it with silent audio. So this is a workaround to let
     * us create a valid context and 'play' audio without user interaction.
     */
    public void fixAudioContext(AudioContext ctx) {
        AudioBuffer buffer = ctx.createBuffer(1, 1, 22050);
        AudioBufferSourceNode source = ctx.createBufferSource();
        source.setBuffer(buffer);
        source.connect(ctx.getDestination());

        source.start(0);
    }

    /**
     * Performs an exponetial mapping on a given 14-bit midi volume.
     * <p>
     * This roughly maps the volume to a logarithmic scale, which is more
     * natural to the human ear.
     * Given the following inputs you can expect the following outputs:
     *     <ul>
     *         <li>0 -> 0</li>
     *         <li>8192 -> 0.5</li>
     *         <li>16384 -> 1</li>
     * </p>
     *
     * @param volume The volume to map.
     * @return The gain value, between 0 and 1.
     * @see AudioConstants#MAX_MIDI_VOLUME
     */
    public float normalizeMidiVolume(int volume) {
        return (float) Math.pow(volume / (float) MAX_MIDI_VOLUME, 2);
    }

    private void unregisterEvents(EventListener<?> fix) {
        HTMLDocument document = Window.current().getDocument();
        for (String event : LISTEN_EVENTS) {
            document.removeEventListener(event, fix);
        }
    }


    private void registerFix() {
        HTMLDocument document = Window.current().getDocument();
        ContextFixer fixer = new ContextFixer(AudioUtils.globalContext);
        for (String event : LISTEN_EVENTS) {
            document.addEventListener(event, fixer);
        }
    }

    private class ContextFixer implements EventListener<Event> {
        private final AudioContext ctx;

        public ContextFixer(AudioContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void handleEvent(Event evt) {
            fixAudioContext(ctx);
            unregisterEvents(this);
        }
    }
}
