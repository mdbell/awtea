package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.input.MouseButtonType;

@ToString(callSuper = true)
@Getter
public class TMouseWheelEvent extends TMouseEvent {


	private final double preciseWheelRotation;
	private final int scrollAmount;
	private final int scrollType;
	private final int unitsToScroll;
	private final int wheelRotation;

	public TMouseWheelEvent(TComponent component, int id, int x, int y, MouseButtonType button, boolean meta,
							double preciseWheelRotation, int scrollAmount, int scrollType, int unitsToScroll, int wheelRotation) {
		super(component, id, x, y, button, meta);
		this.preciseWheelRotation = preciseWheelRotation;
		this.scrollAmount = scrollAmount;
		this.scrollType = scrollType;
		this.unitsToScroll = unitsToScroll;
		this.wheelRotation = wheelRotation;
	}

	public String paramString() {
		return null;
	}

}
