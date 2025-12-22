package me.mdbell.awtea.sound;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webaudio.*;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class AudioContextLine implements SourceDataLine, AudioConstants {

	private static final Logger log = LoggerFactory.getLogger(AudioContextLine.class);

    /**
     * The time in milliseconds between each flush of the audio buffer, or how often we check to see if there is any
	 * audio for us to play. higher value = lower cpu usage, but greater audio delay.
     */
    public static final int FLUSH_TIME = 10;

	/**
	 * The max amount of audio we can queue ahead of time.
	 * Note: bigger values != better - it will introduce audio lag (on top of the existing lag that is part of WebAudio)
	 */
	private static final double MAX_QUEUE_SECONDS = 0.075;

	/**
	 * System property to configure default PCM audio line buffer size.
	 * Valid values: positive integers (default: sample rate * channels)
	 */
	private static final String BUFFER_SIZE_PROPERTY = "me.mdbell.awtea.sound.pcm.buffer_size";

	/**
	 * Cached PCM buffer size override from system property, or -1 if not set.
	 * When > 0, this value is used as the default buffer size instead of sample_rate * channels.
	 */
	private static final int BUFFER_SIZE_OVERRIDE = getBufferSizeOverride();

	/**
	 * Get the PCM buffer size override from system properties with validation.
	 * @return the buffer size if valid positive integer, -1 if not set or invalid
	 */
	private static int getBufferSizeOverride() {
		String bufferSizeStr = System.getProperty(BUFFER_SIZE_PROPERTY);
		if (bufferSizeStr != null) {
			try {
				int value = Integer.parseInt(bufferSizeStr);
				if (value > 0) {
					return value;
				}
			} catch (NumberFormatException e) {
				// Fall through to not set
			}
		}
		return -1; // Not set or invalid
	}

    /***
     * The global audio context.
     */
    private static final AudioContext context = AudioUtils.getGlobalContext();
    /**
     * The global gain node for all line audio.
     */
    private static final GainNode gainNode;

    /**
     * The buffer sources for this line.
     */
    private final List<AudioBufferSourceNode> bufferSources = new ArrayList<>();

    /**
     * The audio format of this line.
     */
    private AudioFormat format;

    /**
     * The audio buffer for this line.
     */
    private CircularAudioBuffer audioBuffer;

    private boolean running = false;

	private double nextStartTime = 0;

	private boolean closed = false;
	private int writeEpoch = 0;   // increments on flush/stop/close

	private float[] sampleScratch;

    static {
        gainNode = context.createGain();
        gainNode.getGain().setValueAtTime(SFX_GAIN, context.getCurrentTime());
        gainNode.connect(AudioUtils.getGlobalGain());
    }

    public AudioContextLine(AudioFormat format) throws LineUnavailableException {
        this.open(format);
    }

    @Override
    public void drain() {
        double delay;
        try {
            delay = drainImpl();
        } catch (Exception e) {
            delay = FLUSH_TIME;
        }
        scheduleNextDrain(delay);
    }

    private void scheduleNextDrain(double delay) {
        if (!running) {
            return;
        }
        Window.setTimeout(this::drain, delay < 0 ? FLUSH_TIME : delay);
    }

    public double drainImpl() {
        int available = audioBuffer.availableToRead();

        if (available == 0) {
            return FLUSH_TIME;
        }

		double now = context.getCurrentTime();
		double queuedAhead = nextStartTime - now;

		if (queuedAhead > MAX_QUEUE_SECONDS) {
			// Do NOT schedule more yet; let WebAudio catch up.
			return FLUSH_TIME;
		}

		int channels = format.getChannels();
		int channelLength = available / channels;
		double startTime = Math.max(now, nextStartTime);

        AudioBuffer buffer = context.createBuffer(channels, channelLength, (int) format.getSampleRate());

		float[] samples = sampleScratch;   // reuse
		int samplesToRead = channelLength * channels;

		audioBuffer.read(samples, samplesToRead);


		if (channels == 1) {
			// Mono: trivial copy
			Float32Array ch0 = buffer.getChannelData(0);
			for (int i = 0; i < channelLength; i++) {
				ch0.set(i, samples[i]);
			}
		} else if (channels == 2) {
			// Stereo: deinterleave in one pass
			Float32Array ch0 = buffer.getChannelData(0); // left
			Float32Array ch1 = buffer.getChannelData(1); // right

			int si = 0; // source index
			for (int frame = 0; frame < channelLength; frame++) {
				float left  = samples[si++];
				float right = samples[si++];
				ch0.set(frame, left);
				ch1.set(frame, right);
			}
		} else {
			// N-channel generic path
			Float32Array[] chans = new Float32Array[channels];
			for (int ch = 0; ch < channels; ch++) {
				chans[ch] = buffer.getChannelData(ch);
			}

			int si = 0;
			for (int frame = 0; frame < channelLength; frame++) {
				for (int ch = 0; ch < channels; ch++) {
					chans[ch].set(frame, samples[si++]);
				}
			}
		}

		double duration = buffer.getDuration();

        AudioBufferSourceNode source = context.createBufferSource();

        if (source == null) {
            log.error("Failed to create buffer source node - Stopping audio line.");
            running = false;
            return 0;
        }

        bufferSources.add(source);

        source.setBuffer(buffer);
        source.connect(gainNode);
        source.onEnded(evt -> bufferSources.remove(source));
        source.start(startTime);

		nextStartTime = startTime + duration;

        return FLUSH_TIME;
    }

    @Override
	public void flush() {
		audioBuffer.reset();
		bufferSources.forEach(n -> {
			n.stop(0);
			n.disconnect();
		});
		bufferSources.clear();
		nextStartTime = 0;
		writeEpoch++;     // invalidate blocking writes
	}

    @Override
    public void start() {
        running = true;
        scheduleNextDrain(0);
    }

    @Override
	public void stop() {
		running = false;
		writeEpoch++;
	}

	@Async
	@Override
	public native int write(byte[] b, int off, int len);

	@SuppressWarnings("unused")
	private void write(byte[] b, int off, int len, AsyncCallback<Integer> callback) {

		int sampleSizeBytes = format.getSampleSizeInBits() / 8;
		int channels        = format.getChannels();
		int frameSizeBytes  = sampleSizeBytes * channels;

		// Only whole frames allowed
		int framesRequested = len / frameSizeBytes;
		if (framesRequested == 0) {
			callback.complete(0);
			return;
		}
		final int bytesRequested = framesRequested * frameSizeBytes;

		// Conversion parameters
		final boolean isBigEndian     = format.isBigEndian();
		final int sampleSizeInBits    = format.getSampleSizeInBits();
		final float floatScale        = (float)(1.0 / Math.pow(2, sampleSizeInBits - 1));

		// Track progress
		final int startEpoch = writeEpoch;
		final int[] written = {0};

		class Writer {
			void attempt() {

				// If flush/stop/close occurred, unblock immediately
				if (closed || writeEpoch != startEpoch) {
					callback.complete(written[0]);
					return;
				}

				int remaining = bytesRequested - written[0];
				if (remaining <= 0) {
					callback.complete(written[0]);
					return;
				}

				int availBytes = available();
				if (availBytes <= 0) {
					// “block” by yielding to event loop
					Window.setTimeout(this::attempt, 0);
					return;
				}

				// How many frames can we write *now*
				int availFrames = availBytes / frameSizeBytes;
				if (availFrames <= 0) {
					Window.setTimeout(this::attempt, 0);
					return;
				}

				int framesToWriteNow = Math.min(availFrames, remaining / frameSizeBytes);
				int bytesToWriteNow  = framesToWriteNow * frameSizeBytes;

				int start = off + written[0];
				int end   = start + bytesToWriteNow;

				// Convert & write to circular buffer
				for (int i = start; i < end; i += frameSizeBytes) {
					for (int ch = 0; ch < channels; ch++) {
						int sampleOffset = i + ch * sampleSizeBytes;
						int sample = AudioUtils.getSample(b, sampleOffset,
							sampleSizeInBits, isBigEndian);
						audioBuffer.write(sample * floatScale);
					}
				}

				written[0] += bytesToWriteNow;

				// Finished?
				if (written[0] >= bytesRequested) {
					callback.complete(written[0]);
				} else {
					// Continue blocking
					Window.setTimeout(this::attempt, 0);
				}
			}
		}

		new Writer().attempt();
	}



    @Override
	public int available() {
		int channels = format.getChannels();
		int sampleSizeBytes = format.getSampleSizeInBits() / 8;
		int frameSizeBytes = sampleSizeBytes * channels;

		int availableSamples = audioBuffer.availableToWrite(); // floats, 1 per sample
		int availableFrames  = availableSamples / channels;

		return availableFrames * frameSizeBytes;
	}

    @Override
    public int getFramePosition() {
        return 0;
    }

    @Override
    public long getLongFramePosition() {
        return 0;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
	public int getBufferSize() {
		int channels = format.getChannels();
		int sampleSizeBytes = format.getSampleSizeInBits() / 8;
		int frameSizeBytes = channels * sampleSizeBytes;

		int totalSamples = audioBuffer.availableToWrite() + audioBuffer.availableToRead();
		int totalFrames  = totalSamples / channels;
		return totalFrames * frameSizeBytes;
	}

    @Override
    public long getMicrosecondPosition() {
        return (long) (context.getCurrentTime() * 1_000_000);
    }

    @Override
    public float getLevel() {
        return 0;
    }

    @Override
	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		this.format = format;
		this.audioBuffer = new CircularAudioBuffer(bufferSize);
		this.closed = false;
		this.running = false;
		this.nextStartTime = 0;
		this.writeEpoch++;

		if (sampleScratch == null || sampleScratch.length < bufferSize) {
			sampleScratch = new float[bufferSize];
		}
	}

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        int defaultBufferSize = BUFFER_SIZE_OVERRIDE > 0 ? BUFFER_SIZE_OVERRIDE : (int) (format.getSampleRate() * format.getChannels());
        this.open(format, defaultBufferSize);
    }

    @Override
    public Line.Info getLineInfo() {
        return null;
    }

    @Override
    public void open() throws LineUnavailableException {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

	@Override
	public void close() {
		closed = true;
		running = false;
		writeEpoch++;
	}

    @Override
    public Control[] getControls() {
        return new Control[0];
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return false;
    }

    @Override
    public Control getControl(Control.Type control) {
        return null;
    }

    @Override
    public void addLineListener(LineListener listener) {
    }

    @Override
    public void removeLineListener(LineListener listener) {
    }
}
