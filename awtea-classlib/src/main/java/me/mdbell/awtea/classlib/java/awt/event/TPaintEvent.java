package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TRectangle;

/**
 * @see java.awt.event.PaintEvent
 */
@Getter
public class TPaintEvent extends TComponentEvent {

	public static final int PAINT_FIRST = 800;
	public static final int PAINT_LAST = 801;

	public static final int PAINT = PAINT_FIRST;
	public static final int UPDATE = PAINT_FIRST + 1;

	private TRectangle updateRect;

	public TPaintEvent(TComponent component, int id, TRectangle updateRect) {
		super(component, id);
		this.updateRect = updateRect;
	}

	public void setUpdateRect(TRectangle updateRect) {
		this.updateRect = updateRect;
	}
}
