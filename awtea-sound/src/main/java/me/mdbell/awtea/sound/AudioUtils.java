package me.mdbell.awtea.sound;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AudioUtils implements AudioConstants {

    /**
     * Events that count as a user gesture for autoplay purposes.
     * Note: browsers differ on which events grant activation (e.g. Chrome
     * honors touchend but not touchstart), so we listen to all of them.
     */
    public String[] LISTEN_EVENTS = new String[]{
            "keydown",
            "mousedown",
            "pointerdown",
            "touchstart",
            "touchend",
            "click"
    };

    /**
     * Every context created by the sound system. Suspended contexts are
     * resumed on each user gesture; contexts created after the first gesture
     * are resumed immediately on registration.
     */
    private final List<AudioContext> registeredContexts = new ArrayList<>();

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
        registerGestureListeners();
        register(globalContext);
        globalGain = globalContext.createGain();
        globalGain.getGain().setValueAtTime(MASTER_GAIN, globalContext.getCurrentTime());
        globalGain.connect(globalContext.getDestination());
    }

    /**
     * Registers a context so autoplay restrictions get lifted from it.
     * <p>
     * Contexts created before any user interaction start in the
     * {@code suspended} state and stay silent until {@code resume()} is
     * called from a user gesture. Anything that creates its own
     * {@link AudioContext} (rather than using {@link #getGlobalContext()})
     * must register it here, or it may never start.
     * </p>
     *
     * @param ctx The context to register.
     */
    public void register(AudioContext ctx) {
        registeredContexts.add(ctx);
        resumeIfSuspended(ctx);
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

    private void resumeIfSuspended(AudioContext ctx) {
        // resume() outside a gesture is harmless: the returned promise just
        // stays pending until the browser allows playback.
        if (AudioContext.STATE_SUSPENDED.equals(ctx.getState())) {
            ctx.resume();
        }
    }

    /**
     * The listeners stay registered for the lifetime of the page: contexts
     * can be created (and suspended) at any point, and some browsers
     * re-suspend contexts themselves (e.g. iOS audio interruptions), so a
     * one-shot unlock is not enough. Capture phase, so game code stopping
     * propagation can't swallow the gesture.
     */
    private void registerGestureListeners() {
        HTMLDocument document = Window.current().getDocument();
        EventListener<Event> resumer = evt -> registeredContexts.forEach(AudioUtils::resumeIfSuspended);
        for (String event : LISTEN_EVENTS) {
            document.addEventListener(event, resumer, true);
        }
    }
}
