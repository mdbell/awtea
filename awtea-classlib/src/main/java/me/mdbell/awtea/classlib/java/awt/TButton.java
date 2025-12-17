package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.event.TActionEvent;
import me.mdbell.awtea.classlib.java.awt.event.TActionListener;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A basic button component that displays a label and responds to clicks.
 * This is a lightweight component that renders itself using graphics
 * primitives.
 * 
 * <p>
 * The button is rendered as a bordered rectangle with centered text.
 * When clicked, it fires an {@link TActionEvent} to registered listeners.
 * 
 * <p>
 * <strong>TODO:</strong> Advanced features not yet implemented:
 * <ul>
 * <li>Keyboard activation (Enter/Space key)</li>
 * <li>Disabled state rendering</li>
 * <li>Focus visual indicator</li>
 * <li>Pressed/hover state animation</li>
 * <li>Icon support</li>
 * <li>Mnemonics and keyboard shortcuts</li>
 * </ul>
 *
 * @see java.awt.Button
 * @see TActionEvent
 * @see TActionListener
 */
public class TButton extends TComponent {

	/**
	 * The label displayed on the button.
	 */
	@Getter
	@Setter
	private String label;

	/**
	 * The action command string that is sent with ActionEvent.
	 * If null, the label is used as the action command.
	 */
	@Getter
	@Setter
	private String actionCommand;

	/**
	 * Whether the button is enabled (can be clicked).
	 * TODO: Currently not rendered differently, should gray out when disabled.
	 */
	@Getter
	@Setter
	private boolean enabled = true;

	/**
	 * List of action listeners registered with this button.
	 */
	private final List<TActionListener> actionListeners = new LinkedList<>();

	/**
	 * Tracks if the mouse is currently pressed on the button.
	 * Used for visual feedback during click.
	 */
	private boolean pressed = false;

	/**
	 * Creates a button with no label.
	 */
	public TButton() {
		this("");
	}

	/**
	 * Creates a button with the specified label.
	 *
	 * @param label the text displayed on the button
	 */
	public TButton(String label) {
		this.label = label;
		this.actionCommand = label;

		// Add mouse listener to handle button clicks
		addMouseListener(new me.mdbell.awtea.classlib.java.awt.event.TMouseListener() {
			@Override
			public void mouseClicked(TMouseEvent e) {
				if (enabled) {
					fireActionPerformed(e);
				}
			}

			@Override
			public void mousePressed(TMouseEvent e) {
				if (enabled) {
					pressed = true;
					repaint();
				}
			}

			@Override
			public void mouseReleased(TMouseEvent e) {
				if (pressed) {
					pressed = false;
					repaint();
				}
			}

			@Override
			public void mouseEntered(TMouseEvent e) {
				// TODO: Implement hover state
			}

			@Override
			public void mouseExited(TMouseEvent e) {
				if (pressed) {
					pressed = false;
					repaint();
				}
			}
		});
	}

	/**
	 * Adds the specified action listener to receive action events from this button.
	 *
	 * @param l the action listener
	 */
	public void addActionListener(TActionListener l) {
		if (l == null) {
			return; // Match AWT behavior
		}
		actionListeners.add(l);
	}

	/**
	 * Removes the specified action listener so it no longer receives action events.
	 *
	 * @param l the action listener to remove
	 */
	public void removeActionListener(TActionListener l) {
		if (l == null) {
			return; // Match AWT behavior
		}
		actionListeners.remove(l);
	}

	/**
	 * Fires an action event to all registered listeners.
	 *
	 * @param trigger the mouse event that triggered this action
	 */
	protected void fireActionPerformed(TMouseEvent trigger) {
		String command = actionCommand != null ? actionCommand : label;
		TActionEvent event = new TActionEvent(
				this,
				TActionEvent.ACTION_PERFORMED,
				command,
				trigger.getWhen(),
				trigger.getModifiers());

		for (TActionListener listener : actionListeners) {
			listener.actionPerformed(event);
		}
	}

	/**
	 * Paints the button as a bordered rectangle with centered text.
	 * 
	 * <p>
	 * Visual design (simplified):
	 * <ul>
	 * <li>Light gray background</li>
	 * <li>Dark gray border</li>
	 * <li>Slightly darker background when pressed</li>
	 * <li>Black text, centered horizontally and vertically</li>
	 * </ul>
	 *
	 * @param g the graphics context to paint on
	 */
	@Override
	public void paint(TGraphics g) {
		int w = getWidth();
		int h = getHeight();

		// Draw background
		Color bgColor;
		if (!enabled) {
			// TODO: Disabled state should be more visually distinct
			bgColor = new Color(220, 220, 220);
		} else if (pressed) {
			bgColor = new Color(180, 180, 180);
		} else {
			bgColor = new Color(240, 240, 240);
		}

		g.setColor(bgColor);
		g.fillRect(0, 0, w, h);

		// Draw border
		g.setColor(new Color(100, 100, 100));
		g.drawRect(0, 0, w - 1, h - 1);

		// Draw label text (centered)
		if (label != null && !label.isEmpty()) {
			g.setColor(enabled ? Color.BLACK : Color.GRAY);

			TFont font = g.getFont();
			if (font == null) {
				// Use a default font if none is set
				font = new TFont("SansSerif", TFont.PLAIN, 12);
				g.setFont(font);
			}

			TFontMetrics fm = g.getFontMetrics(font);
			int textWidth = fm.stringWidth(label);
			int textHeight = fm.getHeight();

			// Center the text
			int x = (w - textWidth) / 2;
			int y = (h - textHeight) / 2 + fm.getAscent();

			g.drawString(label, x, y);
		}
	}

	/**
	 * Returns the preferred size of this button based on its label.
	 * 
	 * TODO: This calculation should account for padding/margins around the text.
	 *
	 * @return the preferred dimensions of this button
	 */
	public TDimension getPreferredSize() {
		// Check if explicitly set
		TDimension explicit = super.getPreferredSize();
		if (explicit != null) {
			return explicit;
		}

		// Calculate based on label
		if (label == null || label.isEmpty()) {
			return new TDimension(75, 25); // Default button size
		}

		// Try to get graphics for measurements, but handle null gracefully
		TGraphics g = null;
		try {
			g = getGraphics();
		} catch (Exception e) {
			// Graphics not available yet, use fallback
		}

		if (g == null) {
			return new TDimension(Math.max(75, label.length() * 8 + 20), 25);
		}

		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
		}

		TFontMetrics fm = g.getFontMetrics(font);
		int textWidth = fm.stringWidth(label);
		int textHeight = fm.getHeight();

		// Add padding: 20px horizontal, 10px vertical
		return new TDimension(textWidth + 20, textHeight + 10);
	}
}
