package me.mdbell.awtea.sound.worklet;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.workers.MessagePort;

@JSFunctor
public interface InitCallback extends JSObject {
    void init(MessagePort port);
}
