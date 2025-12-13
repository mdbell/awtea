package me.mdbell.awtea.sound.pcm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.AudioNode;
import org.teavm.jso.workers.MessagePort;

public abstract class AudioWorkletNode implements JSObject, AudioNode {

	@JSBody(params = {"context", "name", "options"}, script = "return new AudioWorkletNode(context, name, options);")
	public static native AudioWorkletNode create(AudioContext context, String name, Options options);

	@JSProperty
	public native MessagePort getPort();

	public interface Options extends JSObject {

		@JSProperty("numberOfInputs")
		void setNumberOfInputs(int inputs);

		@JSProperty("numberOfOutputs")
		void setNumberOfOutputs(int outputs);

		@JSProperty("outputChannelCount")
		void setOutputChannelCount(int[] count);
	}
}
