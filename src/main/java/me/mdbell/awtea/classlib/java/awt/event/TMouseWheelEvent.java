package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.input.MouseButtonType;
import me.mdbell.awtea.input.MouseEventType;
import me.mdbell.awtea.util.Point;
import org.teavm.jso.dom.html.HTMLCanvasElement;

@ToString(callSuper = true)
@Getter
/**
 * @see java.awt.event.MouseWheelEvent
 */
public class TMouseWheelEvent extends TMouseEvent {

	public static final int SCROLL_AMOUNT = 3;

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

	public static TMouseWheelEvent adapt(TContainer container, HTMLCanvasElement src,
										 org.teavm.jso.dom.events.WheelEvent event, String type) {
		MouseEventType source = MouseEventType.fromHtml(type);

		Point point = new Point(event.getClientX(), event.getClientY());

		translatePoint(point, src);

		double deltaY = event.getDeltaY();
		int scollType = event.getDeltaMode(); // TODO: verify the values map 1-1
		int wheelRotation = (int) Math.signum(deltaY);
		int unitsToScroll = wheelRotation * SCROLL_AMOUNT;

		MouseButtonType button = MouseButtonType.fromHtml(event.getButton());

		TComponent component = container.getComponentAt(point.getX(), point.getY());
		if (component == null) {
			component = container;
		}

		return new TMouseWheelEvent(component, source.getId(), point.getX(), point.getY(),
			button, event.getMetaKey(), deltaY, SCROLL_AMOUNT, scollType, unitsToScroll, wheelRotation);
	}
}
