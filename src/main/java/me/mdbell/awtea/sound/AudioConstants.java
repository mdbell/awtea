package me.mdbell.awtea.sound;

/**
 * Constants for the audio system.
 * <p>
 * THe audio system is designed around a single AudioContext, and a single GainNode that controls the master volume.
 * <p>
 * Any other audio nodes should be connected to the global gain node, or one of it's children.
 *
 * @see AudioUtils
 */
public interface AudioConstants {
    /**
     * The 'master' gain for the audio system.
     * All volum levels are multiplied by this value, and should be in the range [0, 1].
     * If the values go above 1, they will be amplified, but it can cause clipping.
     * <p>
     * The diagram below shows how the master gain is connected to the music and sound effects gain nodes:
     * <pre>
     *              [ MASTER_GAIN ]
     *                     │
     *        ┌────────────┴────────────┐
     *        │                         │
     * [ MUSIC_GAIN ]              [ SFX_GAIN ]
     *        │
     * [ DYNAMIC_MIDI ] // Derived from MAX_MIDI_VOLUME and MIDI_VOLUME_SCALE_FACTOR
     * </pre>
     */
    float MASTER_GAIN = 0.5f;

    /**
     * The gain for music.
     * <p>
     * Music audio is calculated in a slightly different way from SFX audio, as it is rendered from MIDI to PCM, and
     * then played back. We make use of two gain nodes to control the volume of the music. One is controlled by the MIDI
     * volume (derived from MAX_MIDI_VOLUME and MIDI_VOLUME_SCALE_FACTOR), and the other is controlled by this value.
     *
     * @see #MAX_MIDI_VOLUME
     * @see #MIDI_VOLUME_SCALE_FACTOR
     */
    float MUSIC_GAIN = 1.0f;

    /**
     * The gain for sound effects.
     */
    float SFX_GAIN = 1.0f;

    /**
     * The maximum volume value we can get from MIDI commands.
     */
    int MAX_MIDI_VOLUME = 12800;

    /**
     * The scale factor for MIDI volume values.
     * ~4000 units is approximately 10dB in volume.
     * <p>
     * If you were to be using a 7-bit midi volume, you would want to use
     * something like 40.
     */
    int MIDI_VOLUME_SCALE_FACTOR = 40 * 200;
}
