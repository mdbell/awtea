package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;

/**
 * @see java.awt.event.InputEvent
 */
@Getter
@ToString(callSuper = true)
public abstract class TInputEvent extends TComponentEvent {

	/**
	 * The Shift key modifier constant.
	 */
	public static final int SHIFT_MASK = 1 << 0;

	/**
	 * The Control key modifier constant.
	 */
	public static final int CTRL_MASK = 1 << 1;

	/**
	 * The Meta key modifier constant.
	 */
	public static final int META_MASK = 1 << 2;

	/**
	 * The Alt key modifier constant.
	 */
	public static final int ALT_MASK = 1 << 3;

	/**
	 * The AltGraph key modifier constant.
	 */
	public static final int ALT_GRAPH_MASK = 1 << 5;

	/**
	 * The Mouse Button1 modifier constant.
	 */
	public static final int BUTTON1_MASK = 1 << 4;

	/**
	 * The Mouse Button2 modifier constant.
	 */
	public static final int BUTTON2_MASK = 1 << 3;

	/**
	 * The Mouse Button3 modifier constant.
	 */
	public static final int BUTTON3_MASK = 1 << 2;

	/**
	 * The Shift key extended modifier constant.
	 */
	public static final int SHIFT_DOWN_MASK = 1 << 6;

	/**
	 * The Control key extended modifier constant.
	 */
	public static final int CTRL_DOWN_MASK = 1 << 7;

	/**
	 * The Meta key extended modifier constant.
	 */
	public static final int META_DOWN_MASK = 1 << 8;

	/**
	 * The Alt key extended modifier constant.
	 */
	public static final int ALT_DOWN_MASK = 1 << 9;

	/**
	 * The Mouse Button1 extended modifier constant.
	 */
	public static final int BUTTON1_DOWN_MASK = 1 << 10;

	/**
	 * The Mouse Button2 extended modifier constant.
	 */
	public static final int BUTTON2_DOWN_MASK = 1 << 11;

	/**
	 * The Mouse Button3 extended modifier constant.
	 */
	public static final int BUTTON3_DOWN_MASK = 1 << 12;

	/**
	 * The AltGraph key extended modifier constant.
	 */
	public static final int ALT_GRAPH_DOWN_MASK = 1 << 13;

	long when;
	int modifiers;

	TInputEvent(TComponent source, int id, long when, int modifiers) {
		super(source, id);
		this.when = when;
		this.modifiers = modifiers;
	}

}
