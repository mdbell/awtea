package me.mdbell.awtea.sound.worklet;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;

public class PcmProcessor implements InitCallback, ProcessCallback {

    public static void main(String[] args) {
        PcmProcessor instance = new PcmProcessor();

        instance.register();
        System.out.println("Testing");
    }

    private JSObject createConstructor() {
        return createProcessorConstructor(
                this,
                this
        );
    }

    private void register() {
        JSObject ctor = createConstructor();
        registerProcessor("pcm-processor", ctor);
    }

    @JSBody(params = {"initCallback", "processCallback"}, script =
            "return (class extends AudioWorkletProcessor {" +
                    "   constructor() { " +
                    "       initCallback();" +
                    "   }" +
                    "   process(inputs, outputs, parameters) {" +
                    "       return processCallback(inputs, outputs, parameters);" +
                    "   }" +
                    "})")
    private static native JSObject createProcessorConstructor(
            InitCallback initCallback,
            ProcessCallback processCallback
    );


    @JSBody(params = {"name", "ctor"}, script = "registerProcessor(name, ctor);")
    private static native void registerProcessor(String name, JSObject ctor);

    @Override
    public void init() {

    }

    @Override
    public boolean process(@JSByRef float[][] inputs, @JSByRef float[][] outputs, JSObject parameters) {
        return true;
    }

}
