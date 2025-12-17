package me.mdbell.awtea.classlib.javax.swing;

import me.mdbell.awtea.classlib.java.awt.TButton;

/**
 * Swing version of Button component.
 * For now, this is just a stub that extends TButton (AWT Button).
 * 
 * <p><strong>TODO:</strong> Swing-specific features not yet implemented:
 * <ul>
 *   <li>Look and Feel support</li>
 *   <li>Icon support (setIcon, setPressedIcon, etc.)</li>
 *   <li>HTML text rendering</li>
 *   <li>Border management</li>
 *   <li>Swing-specific properties (contentAreaFilled, borderPainted, etc.)</li>
 *   <li>Default button behavior (Enter key activation)</li>
 *   <li>AbstractButton hierarchy</li>
 * </ul>
 *
 * @see javax.swing.JButton
 * @see TButton
 */
public class TJButton extends TButton {

	/**
	 * Creates a button with no text.
	 */
	public TJButton() {
		super();
	}

	/**
	 * Creates a button with the specified text.
	 *
	 * @param text the text of the button
	 */
	public TJButton(String text) {
		super(text);
	}

	// TODO: Add Swing-specific methods as needed
	// - setIcon(Icon)
	// - setPressedIcon(Icon)
	// - setRolloverIcon(Icon)
	// - setDisabledIcon(Icon)
	// - setHorizontalAlignment(int)
	// - setVerticalAlignment(int)
	// - setHorizontalTextPosition(int)
	// - setVerticalTextPosition(int)
	// - setBorderPainted(boolean)
	// - setContentAreaFilled(boolean)
	// - setFocusPainted(boolean)
}
