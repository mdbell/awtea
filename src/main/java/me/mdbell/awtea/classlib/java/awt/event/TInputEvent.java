package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;

@Getter
@ToString(callSuper = true)
public abstract class TInputEvent extends TComponentEvent {

	long when;
	int modifiers;

	TInputEvent(TComponent source, int id, long when, int modifiers) {
		super(source, id);
		this.when = when;
		this.modifiers = modifiers;
	}

}
