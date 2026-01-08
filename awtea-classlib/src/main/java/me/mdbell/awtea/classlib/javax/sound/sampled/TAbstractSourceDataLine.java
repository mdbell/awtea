package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;
import me.mdbell.awtea.monitor.LineMonitor;
import me.mdbell.awtea.monitor.PcmMonitor;
import me.mdbell.awtea.sound.AudioConstants;
import me.mdbell.awtea.sound.AudioUtils;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public abstract class TAbstractSourceDataLine implements TSourceDataLine, AudioConstants {

    private static final Logger log = LoggerFactory.getLogger(TAbstractSourceDataLine.class);

    protected int channels;
    protected int sampleRate;
    protected int sampleSizeBits;
    protected int sampleSizeBytes;
    protected int frameSizeBytes;
    protected boolean bigEndian;

    @Getter
    private final TLine.Info lineInfo;

    @Getter
    private TAudioFormat format;

    @Getter
    private boolean running = false;

    @Getter
    private boolean open = true;

    private final Set<TLineListener> lineListeners = new HashSet<>();

    private long currentFramePosition = 0;

    protected TAbstractSourceDataLine(TDataLine.Info info) throws TLineUnavailableException {
        this.lineInfo = info;
        TAudioFormat format = info.getFormats()[0];
        int bufferSize = info.getMinBufferSize();
        open(format, bufferSize);
    }

    protected void dispatchLineEvent(TLineEvent.Type type) {
        TLineEvent event = new TLineEvent(this, type, getFramePosition());
        lineListeners.forEach(l -> l.update(event));
    }

    /**
     * Called from open() after basic format fields are initialized. bufferFrames is a hint/capacity.
     */
    protected abstract void openBackend(int bufferFrames) throws TLineUnavailableException;

    /**
     * How many frames can be queued *now* without blocking?
     */
    protected abstract int getFreeFrames();

    /**
     * Maximum queue capacity in frames (for getBufferSize/available).
     */
    protected abstract int getMaxFrames();

    /**
     * Enqueue up to `frames` frames from the `bytes` array starting at `offset`.
     * Bytes are raw PCM data.
     *
     * @param bytes the byte array containing PCM data
     * @param offset the offset in the byte array to start reading from
     * @param frames the number of frames to enqueue
     * @return The number of frames accepted or 0 if full
     */
    protected abstract int enqueue(byte[] bytes, int offset, int frames);

    /**
     * Drain up to `framesToDrain` frames from the backend.
     *
     * @param framesToDrain number of frames to drain, or -1 to wait until all drained
     * @return number of frames actually drained - may be less than requested
     */
    protected abstract int drainInternal(int framesToDrain);

    @Override
    public void open() throws TLineUnavailableException {
        TDataLine.Info lineInfo = (TDataLine.Info) getLineInfo();
        TAudioFormat format = lineInfo.getFormats()[0];
        open(format, lineInfo.getMaxBufferSize());
    }

    @Override
    public final void open(TAudioFormat format) throws TLineUnavailableException {
        TDataLine.Info lineInfo = (TDataLine.Info) getLineInfo();
        open(format, lineInfo.getMaxBufferSize());
    }

    @Override
    public void open(TAudioFormat format, int bufferSize) throws TLineUnavailableException {
        this.format = format;
        this.channels = format.getChannels();
        this.sampleRate = (int) format.getSampleRate();
        this.sampleSizeBits = format.getSampleSizeInBits();
        this.sampleSizeBytes = this.sampleSizeBits / 8;
        this.frameSizeBytes = this.sampleSizeBytes * this.channels;
        this.bigEndian = format.isBigEndian();

        int framesHint = bufferSize / this.channels;
        if (framesHint <= 0) {
            log.info("Warning: invalid buffer size hint {}", bufferSize + " bytes; using default");
            framesHint = (int) (sampleRate * 0.1); // ~100ms as a fallback
        }

        currentFramePosition = 0;

        log.info("Opening {} with buffer hint size of {} frames", this.getClass().getSimpleName(), framesHint);

        try {
            openBackend(framesHint);
        } catch (Throwable t) {
            throw new TLineUnavailableException("Failed to open backend: " + t.getMessage());
        }

        open = true;
        running = false;

        LineMonitor.get().onOpen(this);
        dispatchLineEvent(TLineEvent.Type.OPEN);
    }

    @Override
    public void start() {
        running = true;
        LineMonitor.get().onStart(this);
        dispatchLineEvent(TLineEvent.Type.START);
    }

    @Override
    public final void stop() {
        running = false;
        LineMonitor.get().onStop(this);
        dispatchLineEvent(TLineEvent.Type.STOP);
    }

    @Override
    public void flush() {
        LineMonitor.get().onFlush(this);
    }

    @Override
    public void close() {
        stop();
        open = false;
        this.format = null;
        this.channels = 0;
        this.sampleRate = 0;
        this.sampleSizeBits = 0;
        this.sampleSizeBytes = 0;
        this.frameSizeBytes = 0;
        this.bigEndian = false;
        LineMonitor.get().onClose(this);
        PcmMonitor.get().onClose(this);
        dispatchLineEvent(TLineEvent.Type.CLOSE);
    }

    @Override
    public void drain() {
        if (!open) {
            return;
        }
        drainInternal(-1);
    }

    @Override
    public final boolean isActive() {
        return running && open;
    }

    @Override
    public final int getBufferSize() {
        return getMaxFrames() * frameSizeBytes;
    }

    @Override
    public final int available() {
        if (!open || !running) {
            LineMonitor.get().onAvailable(this, 0);
            return 0;
        }
        int freeFrames = getFreeFrames();
        if (freeFrames < 0) freeFrames = 0;

        int result = freeFrames * frameSizeBytes;

        LineMonitor.get().onAvailable(this, result);

        return result;
    }

    @Override
    public long getMicrosecondPosition() {
        // implementations can override if needed
        return 0;
    }

    @Override
    public int getFramePosition() {
        return (int) currentFramePosition;
    }

    @Override
    public long getLongFramePosition() {
        return currentFramePosition;
    }

    @Override
    public float getLevel() {
        return 0f;
    }

    @Override
    public TControl[] getControls() {
        return new TControl[0];
    }

    @Override
    public boolean isControlSupported(TControl.Type control) {
        return false;
    }

    @Override
    public TControl getControl(TControl.Type control) {
        return null;
    }

    @Override
    public void addLineListener(TLineListener listener) {
        lineListeners.add(listener);
    }

    @Override
    public void removeLineListener(TLineListener listener) {
        lineListeners.remove(listener);
    }

    @Override
    public final int write(byte[] b, int off, int len) {

        if (!open || getMaxFrames() <= 0) {
            return 0;
        }

        final int frameSizeBytes = this.frameSizeBytes;

        int framesRequested = len / frameSizeBytes;
        if (framesRequested == 0) {
            return 0;
        }
        final int bytesRequested = framesRequested * frameSizeBytes;

        int writtenBytes = 0;

        while (writtenBytes < bytesRequested) {
            if (!open || !running) {
                break;
            }

            int remainingBytes = bytesRequested - writtenBytes;
            int remainingFrames = remainingBytes / frameSizeBytes;
            if (remainingFrames <= 0) {
                break;
            }

            int freeFrames = getFreeFrames();
            if (freeFrames <= 0) {
                // Let backend drain; this will async-wait and then return
                int drained = drainInternal(remainingFrames);
                // If nothing drained and still no free frames, bail out to avoid spinning
                if (drained <= 0 && getFreeFrames() <= 0) {
                    break;
                }
                continue;
            }

            int framesToEnqueue = Math.min(remainingFrames, freeFrames);
            if (framesToEnqueue <= 0) {
                // Shouldn't happen, but be defensive
                int drained = drainInternal(remainingFrames);
                if (drained <= 0) {
                    break;
                }
                continue;
            }

            int offsetInArray = off + writtenBytes;

            int framesEnqueued = enqueue(b, offsetInArray, framesToEnqueue);
            if (framesEnqueued <= 0) {
                // Backend lied about free frames or is temporarily stuck;
                // try draining some and retry.
                int drained = drainInternal(remainingFrames);
                if (drained <= 0) {
                    break;
                }
                continue;
            }
            currentFramePosition += framesEnqueued;

            int bytesPushed = framesEnqueued * frameSizeBytes;
            writtenBytes += bytesPushed;

            LineMonitor.get().onWrite(this, bytesPushed);
            onSamplesChunk(b, offsetInArray, framesEnqueued);
        }

        return writtenBytes;
    }

    protected void onSamplesChunk(byte[] pcmBytes, int offset, int frames) {
        // compute per-channel peak from this chunk and forward to AudioMonitor
        if (pcmBytes == null || frames <= 0) {
            return;
        }

        // Convert a portion of PCM bytes to floats for peak detection
        float[] peaks = new float[channels];
        float scale = (float) (1.0 / Math.pow(2, sampleSizeBits - 1));

        for (int f = 0; f < frames; f++) {
            int frameOffset = offset + f * frameSizeBytes;
            for (int ch = 0; ch < channels; ch++) {
                int sampleOffset = frameOffset + ch * sampleSizeBytes;
                int sample = AudioUtils.getSample(pcmBytes, sampleOffset, sampleSizeBits, bigEndian);
                float v = Math.abs(sample * scale);
                if (v > peaks[ch]) {
                    peaks[ch] = v;
                }
            }
        }
        PcmMonitor.get().onPcmEnvelope(this, peaks);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (open) {
            log.warn("Finalizing an open SourceDataLine; closing.");
            close();
        }
    }
}
