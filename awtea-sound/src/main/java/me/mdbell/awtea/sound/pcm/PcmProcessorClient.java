package me.mdbell.awtea.sound.pcm;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.sound.AudioUtils;
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
import me.mdbell.awtea.util.TypedArrays;
import me.mdbell.awtea.util.RawTimers;
import me.mdbell.awtea.util.PlatformSupport;

@ExtensionMethod({ JSObjectsExtensions.class })
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

    /**
     * Total bytes posted to the worklet / total bytes it has confirmed
     * consuming via "consumed" messages. The difference is what's queued —
     * but see {@link #getQueuedBytes()}: confirmations need main-thread
     * event-loop turns, which a busy render loop starves, so the live value
     * is estimated from the audio clock and these totals only anchor it.
     */
    private long sentBytes;
    private long ackedBytes;

    /** Audio-clock time ({@code context.currentTime}) of the last ack. */
    private double lastAckTime;

    private int sampleSizeBits;
    private boolean bigEndian;
    private int frameSizeBytes;

    private int keepAliveTimeout = -1;

    /** wasm-gc keepalive: green-thread driven, see startKeepAlive(). */
    private volatile boolean keepAliveRunning;

    /** wasm-gc: JS-side message queue (see attachJsMessageQueue) polled by
     * the worklet-pump green thread. */
    private org.teavm.jso.JSObject jsMessageQueue;
    private volatile boolean workletPumpRunning;

    @org.teavm.jso.JSBody(params = { "port" }, script = ""
            + "var q = []; port.onmessage = function(e) { q.push(e.data); }; return q;")
    private static native org.teavm.jso.JSObject attachJsMessageQueue(MessagePort port);

    @org.teavm.jso.JSBody(params = { "q" }, script = "return q.length > 0 ? q.shift() : null;")
    private static native LineMessage pollJsMessageQueue(org.teavm.jso.JSObject q);

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
        this(sampleRate, channels, sampleRate / 10); // ~ 100ms
    }

    public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames) {
        this(sampleRate, channels, maxQueuedFrames, createContext(sampleRate));
    }

    public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames, AudioContext context) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.maxQueuedFrames = maxQueuedFrames;
        this.context = context;
    }

    /**
     * Bytes currently queued in the worklet, estimated from the audio clock.
     *
     * <p>The worklet's "consumed" confirmations are main-thread message
     * events, and a render loop that only yields via MessageChannel
     * micro-yields (sub-target-FPS frames) starves them — with a pure
     * message-driven counter the queue reads phantom-full, {@link #enqueue}
     * bounces every write, and the worklet underruns while the client
     * believes the line is full. The worklet consumes at exactly
     * {@link #sampleRate} while it has data and {@code context.currentTime}
     * advances on the audio thread's own clock, so consumption since the
     * last confirmed ack is estimated as {@code elapsed * byteRate}, clamped
     * to what was actually sent. Acks re-anchor the estimate whenever they
     * do get through; during a worklet underrun the clock over-estimates
     * drain, which reads as free space — exactly when pushing more audio is
     * the right call.</p>
     */
    public int getQueuedBytes() {
        long unacked = sentBytes - ackedBytes;
        if (unacked <= 0) {
            return 0;
        }
        double elapsed = context.getCurrentTime() - lastAckTime;
        if (elapsed > 0) {
            long estimatedDrain = (long) (elapsed * sampleRate) * frameSizeBytes;
            unacked -= estimatedDrain;
            if (unacked < 0) {
                unacked = 0;
            }
        }
        return (int) unacked;
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
        opts.setOutputChannelCount(new int[] { this.channels });

        this.node = AudioWorkletNode.create(this.context, "pcm-processor", opts);

        // can't use this.node.getPort().setOnMessage()
        // for some reason. Why? idk.
        // wasm-gc: NO Java onmessage functor at all — every JS->wasm
        // callback variant tried (even enqueue-only bodies) ends up
        // CPS-tainted in the full client build and traps in
        // Fiber.isResuming. Instead a pure-JS handler queues the data and
        // the worklet-pump green thread polls it from fiber context.
        if (PlatformSupport.isWebAssemblyGC()) {
            this.jsMessageQueue = attachJsMessageQueue(this.node.getPort());
            startWorkletPump();
        } else {
            setOnMessage(this.node.getPort(), evt -> {
                LineMessage msg = (LineMessage) evt.getData();
                if (!msg.nullish()) {
                    processWorkletMessage(msg);
                }
            });
        }

        startKeepAlive();

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

        int bytesQueued = getQueuedBytes();
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
        Int8Array arr = TypedArrays.from(data);
        ArrayBuffer buf = arr.getBuffer();

        int arrayOffset = arr.getByteOffset();

        ArrayBuffer slice = buf.slice(arrayOffset + offset, arrayOffset + offset + bytesToSend);

        AudioSegmentMessage message = JSObjects.create();

        message.setType("pcm");
        message.setData(slice);
        message.setFrames(frames);

        postWithTransfer(this.node.getPort(), message, slice);

        if (sentBytes == ackedBytes) {
            // Queue was (estimated) empty: anchor the clock now so the
            // estimate starts draining from this write, not from a stale ack.
            lastAckTime = context.getCurrentTime();
        }
        this.sentBytes += bytesToSend;
        return frames;
    }

    @JSBody(params = { "port", "message", "transfer" }, script = "port.postMessage(message, transfer);")
    private static native void postWithTransfer(MessagePort port, LineMessage message, JSObject... transfer);

    public void close() {
        if (!node.nullish()) {

            ShutdownMessage shutdownMsg = JSObjects.create();
            shutdownMsg.setType("shutdown");
            this.node.getPort().postMessage(shutdownMsg);

            this.node.disconnect();
            this.node = null;
        }

        keepAliveRunning = false;
        workletPumpRunning = false;
        if (this.keepAliveTimeout != -1) {
            RawTimers.clearTimeout(this.keepAliveTimeout);
            this.keepAliveTimeout = -1;
        }
    }

    /**
     * wasm-gc: the keepalive runs on a green thread (fiber context) instead of
     * a setTimeout chain — every timer-callback variant we tried gets
     * CPS-tainted through the postMessage path and traps in Fiber.isResuming
     * (no current fiber on the JS event loop). JS backend keeps the raw timer
     * chain.
     */
    private void startKeepAlive() {
        if (PlatformSupport.isWebAssemblyGC()) {
            if (keepAliveRunning) {
                return;
            }
            keepAliveRunning = true;
            Thread t = new Thread(() -> {
                while (keepAliveRunning && this.node != null) {
                    try {
                        Thread.sleep(KEEP_ALIVE_TIMEOUT_MS);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (keepAliveRunning) {
                        sendKeepAlive();
                    }
                }
            }, "pcm-keepalive");
            t.setDaemon(true);
            t.start();
        } else {
            this.keepAliveTimeout = RawTimers.setTimeout(this::handleKeepAliveTick, KEEP_ALIVE_TIMEOUT_MS);
        }
    }

    private void processWorkletMessage(LineMessage msg) {
        String type = msg.getType();
        log.trace("PCM Client: Received message from processor. Msg: {}", JSON.stringify(msg));
        if (type.equals("consumed")) {
            ConsumedMessage consumedMsg = (ConsumedMessage) msg;
            int bytesConsumed = consumedMsg.getBytes();
            // Authoritative reconciliation: the confirmed totals advance
            // and the clock estimate re-anchors at this instant.
            ackedBytes += bytesConsumed;
            if (ackedBytes > sentBytes) {
                ackedBytes = sentBytes;
            }
            lastAckTime = context.getCurrentTime();
            int remaining = getQueuedBytes();
            log.trace("PCM Client: Processor consumed {} bytes. {} bytes remaining in queue.",
                    bytesConsumed, remaining);
            drainListenerSet.removeIf(l -> l.onDrain(bytesConsumed, remaining));
        }
    }

    private void startWorkletPump() {
        if (!PlatformSupport.isWebAssemblyGC() || workletPumpRunning) {
            return;
        }
        workletPumpRunning = true;
        Thread t = new Thread(() -> {
            while (workletPumpRunning) {
                LineMessage msg;
                while (jsMessageQueue != null && (msg = pollJsMessageQueue(jsMessageQueue)) != null && !msg.nullish()) {
                    processWorkletMessage(msg);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "pcm-worklet-pump");
        t.setDaemon(true);
        t.start();
    }

    private void sendKeepAlive() {
        if (this.node != null) {
            KeepaliveMessage pingMsg = JSObjects.create();
            pingMsg.setType("keepalive");
            this.node.getPort().postMessage(pingMsg);
        }
    }

    private void handleKeepAliveTick() {
        sendKeepAlive();
        this.keepAliveTimeout = RawTimers.setTimeout(this::handleKeepAliveTick, KEEP_ALIVE_TIMEOUT_MS);
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
        AudioUtils.register(ctx);
        contextCache.put(sr, ctx);
        addAudioModule(ctx, moduleUrl).await();
        return ctx;
    }

    @JSBody(params = { "port", "handler" }, script = "port.onmessage = handler")
    private static native void setOnMessage(MessagePort port, EventListener<MessageEvent> handler);

    @JSBody(params = { "options" }, script = "return new AudioContext(options)")
    private static native AudioContext createContext(AudioContextOptions options);

    @JSBody(params = { "context", "module" }, script = "return context.audioWorklet.addModule(module);")
    private static native JSPromise<JSUndefined> addAudioModule(AudioContext context, String module);

    public interface AudioContextOptions extends JSObject {
        @JSProperty("sampleRate")
        void setSampleRate(int sr);
    }
}
