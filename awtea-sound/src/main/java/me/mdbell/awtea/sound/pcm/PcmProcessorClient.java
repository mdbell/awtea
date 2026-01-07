package me.mdbell.awtea.sound.pcm;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.sound.DrainListener;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.json.JSON;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.workers.MessagePort;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@ExtensionMethod({JSObjectsExtensions.class})
public class PcmProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(PcmProcessorClient.class);

    // worklet needs to be a JS file, so we embed it in the JS source as a resource
    public static final String MODULE_PATH = "/js/pcm-processor.js";

    private static final String moduleUrl = getModuleUrl();

    @Getter
    private final int sampleRate;
    @Getter
    private final int channels;
    @Getter
    private final int maxQueuedFrames;
    private final AudioContext context;

    private AudioWorkletNode node;

    @Getter
    private int queuedFrames = 0;

    private final Set<DrainListener> drainListenerSet = new HashSet<>();

    public PcmProcessorClient() {
        this(44100); // 44Khz
    }

    public PcmProcessorClient(int sampleRate) {
        this(sampleRate, 2); // stereo audio
    }

    public PcmProcessorClient(int sampleRate, int channels) {
        this(sampleRate, channels, sampleRate / 10); //~ 100ms
    }

    public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames) {
        this(sampleRate, channels, maxQueuedFrames, createContext(sampleRate));
    }

    public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames, AudioContext context) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.maxQueuedFrames = maxQueuedFrames;
        this.context = context;

        this.queuedFrames = 0;
    }

    public void addDrainListener(DrainListener listener) {
        this.drainListenerSet.add(listener);
    }

    public void init() {

        addAudioModule(this.context, moduleUrl).await();

        AudioWorkletNode.Options opts = JSObjects.create();
        opts.setNumberOfInputs(0);
        opts.setNumberOfOutputs(1);
        opts.setOutputChannelCount(new int[]{this.channels});

        this.node = AudioWorkletNode.create(this.context, "pcm-processor", opts);

        // can't use this.node.getPort().setOnMessage()
        // for some reason. Why? idk.
        setOnMessage(this.node.getPort(), evt -> {
            log.debug("PCM Client: Message event received from processor.");
            LineMessage msg = (LineMessage) evt.getData();
            if (msg.nullish()) {
                return;
            }
            String type = msg.getType();
            log.trace("PCM Client: Received message from processor. Msg: {}", JSON.stringify(msg));
            if (type.equals("consumed")) {
                int frames = msg.getFrames();
                queuedFrames -= frames;
                if (queuedFrames < 0) {
                    queuedFrames = 0;
                }
                log.trace("PCM Client: Processor consumed {} frames. {} frames remaining in queue.", frames, queuedFrames);
                drainListenerSet.removeIf(l -> l.onDrain(frames, queuedFrames));
            }
        });

        LineMessage message = JSObjects.create();

        // tell the processor to initialize itself
        message.setType("init");
        message.setChannels(this.channels);

        this.node.getPort().postMessage(message);

        this.node.connect(this.context.getDestination());
    }

    public int enqueue(float[] data, int frames) {
        if (this.node.nullish()) {
            return 0;
        }

        int free = this.maxQueuedFrames - this.queuedFrames;
        if (free <= 0) {
            return 0;
        }

        int framesToSend = Math.min(frames, free);

        if (framesToSend <= 0) {
            return 0;
        }

        Float32Array arr = Float32Array.fromJavaArray(data);

        if (framesToSend != frames) {
            arr = arr.subarray(0, framesToSend * this.channels);
        }

        LineMessage message = JSObjects.create();

        message.setType("pcm");
        message.setData(arr.getBuffer());
        message.setFrames(frames);
        message.setChannels(this.channels);

        this.node.getPort().postMessage(message);

        this.queuedFrames += framesToSend;
        return framesToSend;
    }

    public void close() {
        if (!node.nullish()) {

            LineMessage shutdownMsg = JSObjects.create();
            shutdownMsg.setType("shutdown");
            this.node.getPort().postMessage(shutdownMsg);

            this.node.disconnect();
            this.node = null;
        }
        if (!this.context.nullish()) {
            this.context.close();
        }
    }

    @SneakyThrows
    private static String getModuleUrl() {
        String script = "";
        try (InputStream in = PcmProcessorClient.class.getResourceAsStream(MODULE_PATH)) {
            if (in != null) {
                byte[] data = in.readAllBytes();
                script = new String(data);
            }
        }
        return "data:text/javascript;charset=utf-8," + Window.encodeURIComponent(script);
    }

    private static AudioContext createContext(int sr) {
        AudioContextOptions opts = JSObjects.create();
        opts.setSampleRate(sr);
        return createContext(opts);
    }

    @JSBody(params = {"port", "handler"}, script = "port.onmessage = handler")
    private static native void setOnMessage(MessagePort port, EventListener<MessageEvent> handler);

    @JSBody(params = {"options"}, script = "return new AudioContext(options)")
    private static native AudioContext createContext(AudioContextOptions options);


    @JSBody(params = {"context", "module"}, script = "return context.audioWorklet.addModule(module);")
    private static native JSPromise<JSUndefined> addAudioModule(AudioContext context, String module);

    public interface AudioContextOptions extends JSObject {
        @JSProperty("sampleRate")
        void setSampleRate(int sr);
    }

    private interface LineMessage extends JSObject {

        @JSProperty("type")
        String getType();

        @JSProperty("type")
        void setType(String type);

        // only used when type == consumed || type == pcm

        @JSProperty("frames")
        int getFrames();

        @JSProperty("frames")
        void setFrames(int frames);

        // only used when type == init || type == pcm
        @JSProperty("channels")
        JSNumber getChannels();

        @JSProperty("channels")
        void setChannels(int channels);

        // only used when type == pcm

        @JSProperty("data")
        void setData(ArrayBuffer data);

        @JSProperty("data")
        ArrayBuffer getData();
    }
}

