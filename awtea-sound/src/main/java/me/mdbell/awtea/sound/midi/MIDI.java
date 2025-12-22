package me.mdbell.awtea.sound.midi;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;
import me.mdbell.awtea.sound.AudioConstants;
import me.mdbell.awtea.sound.AudioUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MIDI implements TinyMidiPCM.Options.OnPcmDataCallback, AutoCloseable, AudioConstants {

    public static final String SOUNDFONT_FILE = "/sound/RLNDGM.sf2";

    /**
     * The number of audio channels.
     * <p>
     * Generally should be 2 for stereo.
     */
    public static final int CHANNELS = 2;

    /**
     * The sample rate of the audio context.
     */
    public static final int SAMPLERATE = 44100;

    /**
     * Time in milliseconds to flush audio.
     */
    public static final int FLUSH_TIME = 250;

    /**
     * How much audio we want to render at a given time.
     */
    public static final int RENDER_INTERVAL = 1;

    /**
     * When provided as the volume, the volume will be reused instead of set.
     */
    public static final int REUSE_VOLUME = -1;

    public static final int BUFFER_SIZE = 1024 * 100;

    private static final AudioContext ctx = AudioUtils.getGlobalContext();

    private final List<AudioBufferSourceNode> bufferSources = new ArrayList<>();

    private TinyMidiPCM pcm;

    private Float32Array samples = new Float32Array(0);

    private double lastTime;

    private int flushInterval;

    private static MIDI instance;

    private final MidiCallbacks callbacks;

    @Getter
    private boolean rendering;

    @Getter
    private final GainNode midiGain = ctx.createGain();

    @Getter
    private final GainNode musicGain = ctx.createGain();

    public static MIDI create(MidiCallbacks callbacks) {
        if (instance != null) {
            return instance;
        }

        // initialize audio stuff
        instance = new MIDI(callbacks);

        GainNode musicGain = instance.musicGain;
        musicGain.getGain().setValueAtTime(MUSIC_GAIN, ctx.getCurrentTime());
        musicGain.connect(AudioUtils.getGlobalGain());

        GainNode gainNode = instance.midiGain;
        gainNode.getGain().setValueAtTime(1.0f, ctx.getCurrentTime());
        gainNode.connect(musicGain);

        instance.lastTime = ctx.getCurrentTime();


        TinyMidiPCM.Options options = JSObjects.create();

        options.setRenderInterval(RENDER_INTERVAL);
        options.setOnPcmData(instance);
        options.setOnRenderEnd(ms -> {
            instance.rendering = false;
        });
        options.setBufferSize(BUFFER_SIZE);

        byte[] soundfont = getSoundfont();

        instance.pcm = init(options, soundfont);

        return instance;
    }

    @SneakyThrows
    private static byte[] getSoundfont() {
        try (InputStream in = MIDI.class.getResourceAsStream(SOUNDFONT_FILE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            try {
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return out.toByteArray();
        }
    }

    @Async
    private static native TinyMidiPCM init(TinyMidiPCM.Options options, byte[] soundfont);

    private static void init(TinyMidiPCM.Options options, byte[] soundfont, AsyncCallback<TinyMidiPCM> callback) {
        TinyMidiPCM pcm = new TinyMidiPCM(options);
        pcm.init().then((undef) -> {
            pcm.setSoundfont(soundfont);
            callback.complete(pcm);
            return undef;
        }).catchError(err -> {
            callback.error(new RuntimeException("Failed to initialize TinyMidiPCM"));
            return JSUndefined.instance();
        });
    }

    @Override
    public void onPcmData(Uint8Array data) {
        if (flushInterval == 0) {
            return;
        }
        Float32Array float32 = new Float32Array(data.getBuffer());
        Float32Array temp = new Float32Array(this.samples.getLength() + float32.getLength());
        temp.set(this.samples, 0);
        temp.set(float32, this.samples.getLength());
        this.samples = temp;
    }

    // Actual functionality we wanna use
    public void flush() {
        if (this.ctx == null || this.samples.getLength() == 0) {
            return;
        }
        if (ctx.getState().equals("suspended")) {
            samples = new Float32Array(0);
            return;
        }
        AudioBufferSourceNode source = this.ctx.createBufferSource();

        source.addEventListener("ended", evt -> {
            bufferSources.remove(source);
            if (this.callbacks != null && bufferSources.isEmpty() && !rendering) {
                this.callbacks.onEnd();
            }
        });

        int length = samples.getLength() / CHANNELS;
        AudioBuffer buffer = this.ctx.createBuffer(CHANNELS, length, SAMPLERATE);

        for (int channel = 0; channel < CHANNELS; channel++) {
            Float32Array arr = buffer.getChannelData(channel);

            int offset = channel;
            for (int i = 0; i < length; i++) {
                arr.set(i, samples.get(offset));
                offset += CHANNELS;
            }
        }

        if (lastTime < ctx.getCurrentTime()) {
            lastTime = ctx.getCurrentTime();
        }

        source.setBuffer(buffer);
        source.connect(midiGain);
        source.start(lastTime);
        bufferSources.add(source);

        lastTime += buffer.getDuration();
        samples = new Float32Array(0);
    }

    public void stop() {

        if (flushInterval != 0) {
            Window.clearInterval(flushInterval);
            flushInterval = 0;
        }

        if (!bufferSources.isEmpty()) {
            float vol = getVolume();
            setVolume(0);
            for (AudioBufferSourceNode source : bufferSources) {
                source.stop(ctx.getCurrentTime());
            }
            bufferSources.clear();
            setVolume(vol);
        }

        samples = new Float32Array(0);
    }

    public float getVolume() {
        return midiGain.getGain().getValue();
    }

    public void setVolume(float volume) {
        midiGain.getGain().setValue(volume);
    }

    public void start(float volume, byte[] buffer) {
        if (volume != REUSE_VOLUME) {
            setVolume(volume);
        }

        if (flushInterval != 0) {
            Window.clearInterval(flushInterval);
        }

        lastTime = ctx.getCurrentTime();

        flushInterval = Window.setInterval(this::flush, FLUSH_TIME);

        pcm.render(buffer);
        rendering = true;
        if (callbacks != null) {
            callbacks.onPlay();
        }
    }

    public void play(byte[] buffer, float volume) {
        if (buffer == null) {
            return;
        }
        start(volume, buffer);
    }

    public void close() {
        stop();
        if (pcm != null) {
            ctx.close();
        }
    }

    public interface MidiCallbacks {
        default void onEnd() {
        }

        default void onPlay() {
        }
    }
}
