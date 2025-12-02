package me.mdbell.awtea.sound.midi;

import org.teavm.jso.*;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.Uint8Array;

@JSClass(name = "default")
@JSModule(value = "../../../tinymidipcm/index.js")
public class TinyMidiPCM implements JSObject {

    public TinyMidiPCM(Options options) {

    }

    public native JSPromise<JSUndefined> init();

    public native void render(byte[] buffer);

    public native void setBufferDuration(int seconds);

    public native void ensureInitialized();

    @JSByRef
    public native void setSoundfont(byte[] soundfont);

    public native Uint8Array getPCMBuffer();


    public interface Options extends JSObject {
        @JSProperty("renderInterval")
        void setRenderInterval(int renderInterval);

        @JSProperty("gain")
        void setGain(float gain);

        @JSProperty("onPCMData")
        void setOnPcmData(OnPcmDataCallback onPcmData);

        @JSProperty("onRenderEnd")
        void setOnRenderEnd(OnRenderEndCallback onRenderEnd);

        @JSProperty("bufferSize")
        void setBufferSize(int bufferSize);

        @JSFunctor
        public interface OnPcmDataCallback extends JSObject {
            void onPcmData(Uint8Array data);
        }

        @JSFunctor
        public interface OnRenderEndCallback extends JSObject {
            void onRenderEnd(int ms);
        }
    }
}
