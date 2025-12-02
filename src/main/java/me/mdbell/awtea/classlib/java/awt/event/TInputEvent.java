package me.mdbell.awtea.classlib.java.awt.event;

import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;

@ToString(callSuper = true)
public class TInputEvent extends TComponentEvent {
    public TInputEvent(TComponent component, int id) {
        super(component, id);
    }
}
