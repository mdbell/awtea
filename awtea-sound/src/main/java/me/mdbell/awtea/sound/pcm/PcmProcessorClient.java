package me.mdbell.awtea.sound.pcm;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.sound.DrainListener;
import me.mdbell.awtea.sound.pcm.messages.*;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.jso.JSRecord;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.json.JSON;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
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

    private static boolean moduleLoaded = false;

    public static final int KEEP_ALIVE_TIMEOUT_MS = 2000; // 2 seconds

    private static final String moduleUrl = getModuleUrl();

    private static final JSRecord contextCache = JSRecord.create();

    @Getter
    private final int sampleRate;
    @Getter
    private final int channels;
    @Getter
    private final int maxQueuedFrames;
    private final AudioContext context;

    private AudioWorkletNode node;

    @Getter
    private int queuedBytes;

    private int sampleSizeBits;
    private boolean bigEndian;
    private int frameSizeBytes;

    private int keepAliveTimeout = -1;

    private final Set<DrainListener> drainListenerSet = new HashSet<>();
    
    public int getMaxQueuedBytes() {
        return maxQueuedFrames * frameSizeBytes;
    }

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

        this.queuedBytes = 0;
    }

    public void addDrainListener(DrainListener listener) {
        this.drainListenerSet.add(listener);
    }

    public void init(int sampleSizeBits, boolean bigEndian) {
        this.sampleSizeBits = sampleSizeBits;
        this.bigEndian = bigEndian;
        this.frameSizeBytes = (sampleSizeBits / 8) * channels;

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
                ConsumedMessage consumedMsg = (ConsumedMessage) msg;
                int bytesConsumed = consumedMsg.getBytes();
                queuedBytes -= bytesConsumed;
                if (queuedBytes < 0) {
                    queuedBytes = 0;
                }
                log.trace("PCM Client: Processor consumed {} bytes. {} bytes remaining in queue.",
                        bytesConsumed, queuedBytes);
                drainListenerSet.removeIf(l -> l.onDrain(bytesConsumed, queuedBytes));
            }
        });

        this.keepAliveTimeout = Window.setTimeout(this::handleKeepAliveTick, KEEP_ALIVE_TIMEOUT_MS);

        InitMessage message = JSObjects.create();

        // tell the processor to initialize itself with format metadata
        message.setType("init");
        message.setChannels(this.channels);
        message.setSampleRate(this.sampleRate);
        message.setSampleSizeBits(this.sampleSizeBits);
        message.setBigEndian(this.bigEndian);

        this.node.getPort().postMessage(message);

        this.node.connect(this.context.getDestination());
    }


    public int enqueue(byte[] data, int offset, int frames) {
        if (this.node.nullish()) {
            return 0;
        }

        int bytesQueued = queuedBytes;
        int maxBytes = maxQueuedFrames * frameSizeBytes;
        int freeBytes = maxBytes - bytesQueued;
        if (freeBytes <= 0) {
            return 0;
        }

        int bytesToSend = frames * frameSizeBytes;
        if (bytesToSend > freeBytes) {
            int framesToSend = freeBytes / frameSizeBytes;
            if (framesToSend <= 0) {
                return 0;
            }
            bytesToSend = framesToSend * frameSizeBytes;
            frames = framesToSend;
        }

        // Convert Java byte array to JS Int8Array
        Int8Array arr = Int8Array.fromJavaArray(data);
        ArrayBuffer buf = arr.getBuffer();

        int arrayOffset = arr.getByteOffset();

        ArrayBuffer slice = buf.slice(arrayOffset + offset, arrayOffset + offset + bytesToSend);

        AudioSegmentMessage message = JSObjects.create();

        message.setType("pcm");
        message.setData(slice);
        message.setFrames(frames);

        postWithTransfer(this.node.getPort(), message, slice);

        this.queuedBytes += bytesToSend;
        return frames;
    }

    @JSBody(params = {"port", "message", "transfer"}, script = "port.postMessage(message, transfer);")
    private static native void postWithTransfer(MessagePort port, LineMessage message, JSObject... transfer);

    public void close() {
        if (!node.nullish()) {

            ShutdownMessage shutdownMsg = JSObjects.create();
            shutdownMsg.setType("shutdown");
            this.node.getPort().postMessage(shutdownMsg);

            this.node.disconnect();
            this.node = null;
        }

        if (this.keepAliveTimeout != -1) {
            Window.clearTimeout(this.keepAliveTimeout);
            this.keepAliveTimeout = -1;
        }
    }

    private void handleKeepAliveTick() {

        if (this.node != null) {
            KeepaliveMessage pingMsg = JSObjects.create();
            pingMsg.setType("keepalive");
            this.node.getPort().postMessage(pingMsg);
        }

        this.keepAliveTimeout = Window.setTimeout(this::handleKeepAliveTick, KEEP_ALIVE_TIMEOUT_MS);
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
        // Make use of a cached context if we have one for the given sample rate
        // (this prevents us from loading the module a zillion times)
        if (contextCache.has(sr)) {
            return contextCache.get(sr);
        }
        AudioContextOptions opts = JSObjects.create();
        opts.setSampleRate(sr);
        AudioContext ctx = createContext(opts);
        contextCache.put(sr, ctx);
        addAudioModule(ctx, moduleUrl).await();
        return ctx;
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
}

