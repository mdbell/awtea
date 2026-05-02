package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.SneakyThrows;
import me.mdbell.awtea.classlib.java.awt.awtea.TMainThreadBridge;
import me.mdbell.awtea.sound.DrainListener;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker-mode SourceDataLine that delegates all Web Audio API calls to the
 * main thread via TMainThreadBridge (audio.init / audio.pcm / audio.close /
 * audio.keepalive messages). The main thread creates the AudioContext and
 * AudioWorkletNode and forwards drain notifications back as audio.consumed.
 */
public class TWorkerAudioSourceDataLine extends TAbstractSourceDataLine {

    private static final int KEEP_ALIVE_MS = 2000;

    private int audioId = -1;
    private int queuedBytes = 0;
    private int maxQueuedBytes = 0;
    private int keepAliveTimer = -1;
    private final Set<DrainListener> drainListeners = new HashSet<>();

    public TWorkerAudioSourceDataLine(TDataLine.Info info) throws TLineUnavailableException {
        super(info);
    }

    @Override
    protected void openBackend(int bufferBytes) {
        int bufferFrames = bufferBytes / frameSizeBytes;
        int maxQueuedFrames = bufferFrames > 0 ? bufferFrames : (int) (sampleRate * 0.1);
        this.maxQueuedBytes = maxQueuedFrames * frameSizeBytes;

        TMainThreadBridge.setAudioListener(this::onAudioEvent);

        String script = loadWorkletScript();
        TMainThreadBridge.BridgeResponse resp = TMainThreadBridge.request("audio.init", req -> {
            req.setAudioSampleRate(sampleRate);
            req.setAudioChannels(channels);
            req.setAudioSampleSizeBits(sampleSizeBits);
            req.setAudioBigEndian(bigEndian);
            req.setAudioMaxFrames(maxQueuedFrames);
            req.setAudioScript(script);
        });
        this.audioId = resp.getAudioId();

        keepAliveTimer = workerSetTimeout(this::keepAlive, KEEP_ALIVE_MS);
    }

    private void onAudioEvent(TMainThreadBridge.BridgeResponse resp) {
        if (!"audio.consumed".equals(resp.getType())) return;
        int bytes = resp.getConsumedBytes();
        queuedBytes -= bytes;
        if (queuedBytes < 0) queuedBytes = 0;
        drainListeners.removeIf(l -> l.onDrain(bytes, queuedBytes));
    }

    private void keepAlive() {
        if (audioId >= 0) {
            TMainThreadBridge.sendAudioControl("audio.keepalive", audioId);
        }
        keepAliveTimer = workerSetTimeout(this::keepAlive, KEEP_ALIVE_MS);
    }

    @Override
    protected int getFreeBytes() {
        return Math.max(0, maxQueuedBytes - queuedBytes);
    }

    @Override
    protected int getMaxBytes() {
        return maxQueuedBytes;
    }

    @Override
    protected int enqueue(byte[] bytes, int offset, int length) {
        if (audioId < 0) return 0;
        Int8Array arr = Int8Array.fromJavaArray(bytes);
        ArrayBuffer buf = arr.getBuffer();
        ArrayBuffer slice = buf.slice(arr.getByteOffset() + offset, arr.getByteOffset() + offset + length);
        int frames = length / frameSizeBytes;
        TMainThreadBridge.sendPcm(audioId, slice, frames);
        queuedBytes += length;
        return length;
    }

    @Override
    protected int drainInternal(int bytesToDrain) {
        return drainAsync(bytesToDrain).await();
    }

    private JSPromise<Integer> drainAsync(int bytesToDrain) {
        if (!isActive() || queuedBytes == 0) {
            return JSPromise.resolve(0);
        }
        return new JSPromise<>((resolve, reject) -> {
            int initialQueuedBytes = queuedBytes;
            if (bytesToDrain < 0) {
                drainListeners.add((bytesDrained, bytesRemaining) -> {
                    if (bytesRemaining == 0) {
                        resolve.accept(initialQueuedBytes);
                        return true;
                    }
                    return false;
                });
                return;
            }
            AtomicInteger remaining = new AtomicInteger(bytesToDrain);
            drainListeners.add((bytesDrained, bytesRemaining) -> {
                int rem = remaining.addAndGet(-bytesDrained);
                if (rem <= 0) {
                    resolve.accept(bytesToDrain - rem);
                    return true;
                }
                if (bytesRemaining == 0) {
                    resolve.accept(initialQueuedBytes);
                    return true;
                }
                return false;
            });
        });
    }

    @Override
    public void close() {
        super.close();
        if (keepAliveTimer >= 0) {
            workerClearTimeout(keepAliveTimer);
            keepAliveTimer = -1;
        }
        if (audioId >= 0) {
            TMainThreadBridge.sendAudioControl("audio.close", audioId);
            audioId = -1;
        }
        queuedBytes = 0;
        drainListeners.clear();
    }

    @SneakyThrows
    private static String loadWorkletScript() {
        try (InputStream in = TWorkerAudioSourceDataLine.class.getResourceAsStream("/js/pcm-processor.js")) {
            if (in == null) return "";
            return new String(in.readAllBytes());
        }
    }

    // Use bare setTimeout/clearTimeout so they resolve against the worker's
    // global scope, not window (which doesn't exist in a DedicatedWorker).
    @JSBody(params = {"handler", "ms"}, script = "return setTimeout(handler, ms);")
    private static native int workerSetTimeout(Runnable handler, int ms);

    @JSBody(params = {"id"}, script = "clearTimeout(id);")
    private static native void workerClearTimeout(int id);
}
