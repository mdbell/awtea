package me.mdbell.awtea.sound.pcm;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.*;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.workers.MessagePort;
import me.mdbell.awtea.util.JSObjectsExtensions;

import java.io.InputStream;

@ExtensionMethod({JSObjectsExtensions.class})
public class PcmProcessorClient {

	// worklet needs to be a JS file, so we embed it in the JS source as a resource
	public static final String MODULE_PATH = "/sound/pcm-processor.js";

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

	public PcmProcessorClient() {
		this(44100); // 44Khz
	}

	public PcmProcessorClient(int sampleRate) {
		this(sampleRate, 2); // stereo audio
	}

	public PcmProcessorClient(int sampleRate, int channels){
		this(sampleRate, channels, sampleRate / 10); //~ 100ms
	}

	public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames){
		this(sampleRate, channels, maxQueuedFrames, createContext(sampleRate));
	}

	public PcmProcessorClient(int sampleRate, int channels, int maxQueuedFrames, AudioContext context) {
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.maxQueuedFrames = maxQueuedFrames;
		this.context = context;

		this.queuedFrames = 0;
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
			LineMessage msg = (LineMessage) evt.getData();
			if(msg.nullish() || msg.getType() == null){
				return;
			}
			if (msg.getType().equals("consumed")) {
				queuedFrames -= msg.getFrames();
				if(queuedFrames < 0) {
					queuedFrames = 0;
				}
			}
		});

		LineMessage message = JSObjects.create();

		// tell the processor to initialize itself
		message.setType("init");
		message.setChannels(this.channels);

		this.node.getPort().postMessage(message);

		this.node.connect(this.context.getDestination());
	}

	public int enqueue(float[] data, int frames){
		if(this.node.nullish()){
			return 0;
		}

		int free = this.maxQueuedFrames - this.queuedFrames;
		if(free <= 0){
			return 0;
		}

		int framesToSend = Math.min(frames, free);

		if(framesToSend <= 0){
			return 0;
		}

		// unwrap into Float32Array _without_ copying the data.
		Float32Array arr = unwrap(data);

		if(framesToSend != frames){
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

	public void close(){
		if(!node.nullish()){
			this.node.disconnect();
			this.node = null;
		}
		if(!this.context.nullish()){
			this.context.close();
		}
	}

	@SneakyThrows
	private static String getModuleUrl() {
		String script = "";
		try(InputStream in = PcmProcessorClient.class.getResourceAsStream(MODULE_PATH)){
			if(in != null) {
				byte[] data = in.readAllBytes();
				script = new String(data);
			}
		}
		return "data:text/javascript;charset=utf-8," + Window.encodeURIComponent(script);
	}

	private static AudioContext createContext(int sr){
		AudioContextOptions opts = JSObjects.create();
		opts.setSampleRate(sr);
		return createContext(opts);
	}

	@JSBody(params= {"port", "handler"}, script = "port.onmessage = handler")
	private static native void setOnMessage(MessagePort port, EventListener<MessageEvent> handler);

	@JSBody(params = "arr", script = "return arr;")
	private static native Float32Array unwrap(@JSByRef float[] arr);

	@JSBody(params = {"options"}, script = "return new AudioContext(options)")
	private static native AudioContext createContext(AudioContextOptions options);


	@JSBody(params = {"context", "module"}, script = "return context.audioWorklet.addModule(module);")
	private static native JSPromise<JSUndefined> addAudioModule(AudioContext context, String module);

	public interface AudioContextOptions extends JSObject{
		@JSProperty("sampleRate")
		void setSampleRate(int sr);
	}

	private interface LineMessage extends JSObject {

		@JSProperty
		String getType();

		@JSProperty
		void setType(String type);

		// only used when type == consumed || type == pcm

		@JSProperty
		int getFrames();

		@JSProperty
		void setFrames(int frames);

		// only used when type == init || type == pcm
		@JSProperty
		int getChannels();

		@JSProperty
		void setChannels(int channels);

		// only used when type == pcm

		@JSProperty
		void setData(ArrayBuffer data);
	}
}

