package me.mdbell.awtea.sound.worklet;

import me.mdbell.awtea.sound.pcm.messages.*;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.workers.MessagePort;

import java.util.*;


public class PcmProcessor {

    private static final Logger log = LoggerFactory.getLogger(PcmProcessor.class);
    private static final int MAX_POOL_SIZE = 10;

    private static int nextId = 0;
    private static Map<Integer, PcmProcessor> instances = new HashMap<>();

    private int instanceId;

    private MessagePort port;

    private static final int IDLE_TIMEOUT_FRAMES = 48000 * 5; // 5 seconds at 48kHz
    private int idleFrameCount = 0;

    private boolean shutdown = false;

    private Queue<QueuedAudioData> audioQueue;
    private List<QueuedAudioData> dataPool = new LinkedList<>();

    private int channels = 0;
    private int queuedFrames = 0;
    
    // Format metadata from InitMessage
    private int sampleRate = 0;
    private int sampleSizeBits = 0;
    private boolean bigEndian = false;
    private int frameSizeBytes = 0;

    public static void main(String[] args) {
        log.info("Registering PCM Processor");
        registerProcessorFactory();
    }

    private static void registerProcessorFactory() {
        JSObject ctor = createProcessorConstructor(
                PcmProcessor::createInstance,
                PcmProcessor::processInstance
        );
        registerProcessor("pcm-processor", ctor);
    }

    @JSBody(params = {"createCallback", "processCallback"}, script =
            "return (class extends AudioWorkletProcessor {" +
                    "   constructor() { " +
                    "       super();" +
                    "       this.instanceId = createCallback(this.port);" +
                    "   }" +
                    "   process(inputs, outputs, parameters) {" +
                    "       return processCallback(this.instanceId, inputs, outputs, parameters);" +
                    "   }" +
                    "})")
    private static native JSObject createProcessorConstructor(
            CreateInstanceCallback createCallback,
            ProcessInstanceCallback processCallback
    );

    @JSBody(params = {"name", "ctor"}, script = "registerProcessor(name, ctor);")
    private static native void registerProcessor(String name, JSObject ctor);

    // Factory callback - creates new instance and returns its ID
    private static int createInstance(MessagePort port) {
        int id = nextId++;
        PcmProcessor processor = new PcmProcessor();
        processor.instanceId = id;
        processor.init(port);
        instances.put(id, processor);
        log.info("Created PCM Processor instance {}", id);
        return id;
    }

    private static boolean processInstance(int id,
                                           JSArray<JSArray<Float32Array>> inputs,
                                           JSArray<JSArray<Float32Array>> outputs,
                                           JSObject parameters) {
        PcmProcessor processor = instances.get(id);
        if (processor == null) {
            log.error("No processor found for instance {}", id);
            return false;
        }
        boolean result = processor.process(inputs, outputs, parameters);

        // Cleanup if processor shuts down
        if (!result) {
            instances.remove(id);
            log.info("Removed PCM Processor instance {}", id);
        }

        return result;
    }

    private void init(MessagePort port) {
        this.port = port;
        this.audioQueue = new LinkedList<>();
        this.channels = 2;
        this.queuedFrames = 0;

        setOnMessage(port, this::handleMessage);

    }

    private QueuedAudioData getPooledData() {
        if (!dataPool.isEmpty()) {
            return dataPool.remove(0);
        } else {
            return new QueuedAudioData();
        }
    }

    private void releasePooledData(QueuedAudioData data) {
        if (dataPool.size() < MAX_POOL_SIZE) {
            data.data = null; // help GC
            dataPool.add(data);
        }
    }

    private void handleMessage(MessageEvent event) {
        LineMessage msg = (LineMessage) event.getData();
        log.debug("PCM Processor: Received message from main thread. Msg: {}", msg);

        if (shutdown) {
            log.warn("PCM Processor: Received message after shutdown. Ignoring.");
            return;
        }

        String type = msg.getType();
        switch (type) {
            case "init":
                InitMessage initMsg = (InitMessage) msg;
                this.channels = initMsg.getChannels();
                this.sampleRate = initMsg.getSampleRate();
                this.sampleSizeBits = initMsg.getSampleSizeBits();
                this.bigEndian = initMsg.isBigEndian();
                this.frameSizeBytes = (sampleSizeBits / 8) * channels;
                log.debug("PCM Processor: Initialized with {} channels, {} Hz, {} bits, {} endian, {} bytes/frame.", 
                         this.channels, this.sampleRate, this.sampleSizeBits, 
                         this.bigEndian ? "big" : "little", this.frameSizeBytes);
                break;
            case "pcm":
                AudioSegmentMessage pcmMsg = (AudioSegmentMessage) msg;
                QueuedAudioData data = getPooledData();
                ArrayBuffer buffer = pcmMsg.getData();
                int frames = pcmMsg.getFrames();
                
                // Convert raw PCM bytes to float samples
                byte[] pcmBytes = new Int8Array(buffer).toJavaArray();
                float[] floatSamples = new float[frames * channels];
                PcmConverter.convertPcmToFloat(pcmBytes, floatSamples, frames, channels, sampleSizeBits, bigEndian);
                
                data.data = floatSamples;
                data.frames = frames;
                data.channels = channels;
                data.offsetFrames = 0;
                this.audioQueue.add(data);
                this.queuedFrames += data.frames;
                log.trace("PCM Processor: Queued {} frames of PCM data ({} frames total in queue).", data.frames, this.queuedFrames);
                break;
            case "keepalive":
                // just a keepalive message, reset the idle counter
                this.idleFrameCount = 0;
                log.trace("PCM Processor: Keepalive received, resetting idle counter.");
                // acknowledge the keepalive
                postBasicMessage("keepalive-ack");
                break;
            case "shutdown":
                shutdown();
                break;
            default:
                log.warn("PCM Processor: Unknown message type: {}", type);
                break;
        }
    }

    private void shutdown() {
        this.audioQueue.clear();
        this.shutdown = true;
        this.port.close();
        log.info("PCM Processor: Shutdown complete.");
    }

    /**
     * Process audio data (get's called from the audio thread)
     *
     * @param inputs     The input audio buffers
     * @param outputs    The output audio buffers
     * @param parameters The parameters
     * @return True to continue processing, false to stop
     */
    private boolean process(JSArray<JSArray<Float32Array>> inputs, JSArray<JSArray<Float32Array>> outputs, JSObject parameters) {
        if (shutdown) {
            return false;
        }

        JSArray<Float32Array> output = outputs.get(0);
        int channelCount = output.getLength();
        int framesRequested = output.get(0).getLength();

        int framesFilled = 0;
        int framesConsumedTotal = 0;

        float[][] outputChannels = new float[channelCount][];
        for (int ch = 0; ch < channelCount; ch++) {
            outputChannels[ch] = output.get(ch).toJavaArray();
            Arrays.fill(outputChannels[ch], 0.0f);
        }

        while (framesFilled < framesRequested && !audioQueue.isEmpty()) {
            QueuedAudioData chunk = audioQueue.peek();
            int framesAvailable = chunk.frames - chunk.offsetFrames;

            if (chunk.channels != channelCount) {
                log.warn("PCM Processor: Channel count mismatch. Expected {}, got {}. Dropping chunk.", channelCount, chunk.channels);
                queuedFrames -= framesAvailable;
                releasePooledData(audioQueue.poll());
                continue;
            }

            if (framesAvailable <= 0) {
                releasePooledData(audioQueue.poll());
                continue;
            }

            int framesToCopy = Math.min(framesAvailable, framesRequested - framesFilled);
            float[] src = chunk.data;
            int startFrame = chunk.offsetFrames;

            for (int f = 0; f < framesToCopy; f++) {
                int srcBase = (startFrame + f) * channelCount;
                int dstIndex = framesFilled + f;
                for (int ch = 0; ch < channelCount; ch++) {
                    outputChannels[ch][dstIndex] = src[srcBase + ch];
                }
            }

            chunk.offsetFrames += framesToCopy;
            framesFilled += framesToCopy;
            framesConsumedTotal += framesToCopy;

            if (chunk.offsetFrames >= chunk.frames) {
                releasePooledData(audioQueue.poll());
            }
        }

        queuedFrames -= framesConsumedTotal;
        if (queuedFrames < 0) {
            queuedFrames = 0;
        }

        // Track idle time
        if (framesFilled == 0) {
            idleFrameCount += framesRequested;
            if (idleFrameCount >= IDLE_TIMEOUT_FRAMES) {
                log.warn("PCM Processor: Idle timeout reached.  Shutting down.");
                postBasicMessage("timeout");
                return false; // Stop the processor
            }
        } else {
            idleFrameCount = 0; // Reset on activity
        }

        if (framesConsumedTotal > 0) {
            log.trace("PCM Processor: Filled {} frames ({} frames remaining in queue).", framesFilled, queuedFrames);
            notifyBytesConsumed(framesConsumedTotal);
        }

        return true;
    }

    private void notifyBytesConsumed(int frames) {
        int bytesConsumed = frames * frameSizeBytes;
        ConsumedMessage message = JSObjects.create();
        message.setType("consumed");
        message.setBytes(bytesConsumed);
        this.port.postMessage(message);
        log.trace("PCM Processor: Notified main thread of {} bytes consumed ({} frames).", bytesConsumed, frames);
    }

    private void postBasicMessage(String type) {
        LineMessage message = JSObjects.create();
        message.setType(type);
        this.port.postMessage(message);
    }

    @JSBody(params = {"port", "handler"}, script = "port.onmessage = handler")
    private static native void setOnMessage(MessagePort port, EventListener<MessageEvent> handler);

    @JSFunctor
    @FunctionalInterface
    private interface CreateInstanceCallback extends JSObject {
        int create(MessagePort port);
    }

    // Callback interface for processing
    @JSFunctor
    @FunctionalInterface
    private interface ProcessInstanceCallback extends JSObject {
        boolean process(int instanceId,
                        JSArray<JSArray<Float32Array>> inputs,
                        JSArray<JSArray<Float32Array>> outputs,
                        JSObject parameters);
    }
}
