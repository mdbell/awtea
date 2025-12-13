package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TAWTEvent;
import me.mdbell.awtea.classlib.java.awt.TComponent;

@Getter
@ToString(callSuper = true)
public class TComponentEvent extends TAWTEvent {

	private final TComponent component;

	public TComponentEvent(TComponent component, int id) {
		super(component, id);
		this.component = component;
	}
}
