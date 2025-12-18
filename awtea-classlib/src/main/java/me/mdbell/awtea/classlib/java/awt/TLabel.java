package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;

import java.awt.Color;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A label component for displaying non-editable text.
 * This is a lightweight component that renders text using graphics primitives.
 * 
 * <p>
 * Labels can display a single line of text with configurable alignment.
 * 
 * <p>
 * <strong>TODO:</strong> Advanced features not yet implemented:
 * <ul>
 * <li>Icon support (text + icon or icon only)</li>
 * <li>Multi-line text wrapping</li>
 * <li>HTML text rendering</li>
 * <li>Vertical alignment options</li>
 * <li>Text with mnemonic underline</li>
 * <li>Display disabled state</li>
 * </ul>
 *
 * @see java.awt.Label
 */
public class TLabel extends TComponent {

	/**
	 * Indicates that the label should be left justified.
	 */
	public static final int LEFT = 0;

	/**
	 * Indicates that the label should be centered.
	 */
	public static final int CENTER = 1;

	/**
	 * Indicates that the label should be right justified.
	 */
	public static final int RIGHT = 2;

	/**
	 * The text displayed by the label.
	 */
	@Getter
	@Setter
	private String text;

	/**
	 * The alignment of the label's text.
	 * One of LEFT, CENTER, or RIGHT.
	 */
	@Getter
	private int alignment;

	/**
	 * Creates a label with empty text and left alignment.
	 */
	public TLabel() {
		this("", LEFT);
	}

	/**
	 * Creates a label with the specified text and left alignment.
	 *
	 * @param text the text to display
	 */
	public TLabel(String text) {
		this(text, LEFT);
	}

	/**
	 * Creates a label with the specified text and alignment.
	 *
	 * @param text      the text to display
	 * @param alignment the alignment (LEFT, CENTER, or RIGHT)
	 */
	public TLabel(String text, int alignment) {
		this.text = text != null ? text : "";
		setAlignment(alignment);
	}

	/**
	 * Sets the alignment of the label's text.
	 *
	 * @param alignment the alignment (LEFT, CENTER, or RIGHT)
	 * @throws IllegalArgumentException if alignment is not LEFT, CENTER, or RIGHT
	 */
	public void setAlignment(int alignment) {
		if (alignment != LEFT && alignment != CENTER && alignment != RIGHT) {
			throw new IllegalArgumentException("Invalid alignment: " + alignment);
		}
		this.alignment = alignment;
		repaint();
	}

	/**
	 * Paints the label by drawing its text according to the current alignment.
	 *
	 * @param g the graphics context to paint on
	 */
	@Override
	public void paint(TGraphics g) {
		int w = getWidth();
		int h = getHeight();

		// Clear background to prevent text overlap on repaint
		Color bg = getBackground();
		if (bg != null) {
			g.setColor(bg);
			g.fillRect(0, 0, w, h);
		}

		// Draw text if present
		if (text != null && !text.isEmpty()) {
			// Use foreground color if set, otherwise black
			Color textColor = getForeground();
			if (textColor == null) {
				textColor = Color.BLACK;
			}
			g.setColor(textColor);

			TFont font = g.getFont();
			if (font == null) {
				// Use a default font if none is set
				font = new TFont("SansSerif", TFont.PLAIN, 12);
				g.setFont(font);
			}

			TFontMetrics fm = g.getFontMetrics(font);
			int textWidth = fm.stringWidth(text);
			int textHeight = fm.getHeight();

			// Calculate x position based on alignment
			int x;
			switch (alignment) {
				case CENTER:
					x = (w - textWidth) / 2;
					break;
				case RIGHT:
					x = w - textWidth;
					break;
				case LEFT:
				default:
					x = 0;
					break;
			}

			// Vertically center the text
			int y = (h - textHeight) / 2 + fm.getAscent();

			g.drawString(text, x, y);
		}
	}

	/**
	 * Returns the preferred size of this label based on its text.
	 * 
	 * TODO: This calculation should account for padding/margins around the text.
	 *
	 * @return the preferred dimensions of this label
	 */
	public TDimension getPreferredSize() {
		// Check if explicitly set
		TDimension explicit = super.getPreferredSize();
		if (explicit != null) {
			return explicit;
		}

		// Calculate based on text
		if (text == null || text.isEmpty()) {
			return new TDimension(0, 20); // Default height for empty label
		}

		// Try to get graphics for measurements, but handle null gracefully
		TGraphics g = null;
		try {
			g = getGraphics();
		} catch (Exception e) {
			// Graphics not available yet, use fallback
		}

		if (g == null) {
			// Fallback dimensions based on character count
			return new TDimension(text.length() * 7 + 4, 20);
		}

		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
		}

		TFontMetrics fm = g.getFontMetrics(font);
		int textWidth = fm.stringWidth(text);
		int textHeight = fm.getHeight();

		// Add small padding
		return new TDimension(textWidth + 4, textHeight + 4);
	}
}
