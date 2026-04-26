package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.event.*;
import me.mdbell.awtea.util.ThreadUtils;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A single-line text input component that allows the user to edit text.
 * This is a lightweight component that renders itself using graphics
 * primitives.
 * 
 * <p>
 * The text field displays an editable text string with a blinking cursor
 * (caret).
 * Users can type, delete, select, copy, paste, and navigate through the text.
 * When the Enter key is pressed, an {@link TActionEvent} is fired to registered
 * listeners.
 * 
 * <p>
 * <strong>Features implemented:</strong>
 * <ul>
 * <li>Text input via keyboard (typing, backspace, delete)</li>
 * <li>Caret positioning with mouse clicks and arrow keys</li>
 * <li>Text selection with mouse drag and Shift+arrows</li>
 * <li>Keyboard shortcuts: Ctrl+A (select all), Ctrl+C/V/X (copy/paste/cut)</li>
 * <li>Visual feedback: caret blinking, selection highlighting</li>
 * <li>Text scrolling when content exceeds visible width</li>
 * <li>ActionListener support (fires on Enter key)</li>
 * </ul>
 * 
 * <p>
 * <strong>TODO:</strong> Advanced features not yet implemented:
 * <ul>
 * <li>Password masking (setEchoChar)</li>
 * <li>Max length constraint</li>
 * <li>Input validation</li>
 * <li>IME support for international text input</li>
 * <li>Native clipboard integration (currently simulated)</li>
 * <li>Home/End keys</li>
 * </ul>
 *
 * @see java.awt.TextField
 * @see TActionEvent
 * @see TActionListener
 */
public class TTextField extends TComponent {

	private static final Logger log = LoggerFactory.getLogger(TTextField.class);

	/**
	 * The text content of the field.
	 */
	@Getter
	private String text;

	/**
	 * The number of columns (affects preferred width).
	 */
	@Getter
	@Setter
	private int columns;

	/**
	 * The current caret position (cursor index in the text).
	 * Valid range: 0 to text.length() inclusive.
	 */
	@Getter
	private int caretPosition;

	/**
	 * The selection start index, or -1 if no selection.
	 */
	private int selectionStart = -1;

	/**
	 * The selection end index, or -1 if no selection.
	 */
	private int selectionEnd = -1;

	/**
	 * The action command string that is sent with ActionEvent.
	 * If null, the text content is used as the action command.
	 */
	@Getter
	@Setter
	private String actionCommand;

	/**
	 * Whether the field is editable.
	 */
	@Getter
	@Setter
	private boolean editable = true;

	/**
	 * List of action listeners registered with this text field.
	 */
	private final List<TActionListener> actionListeners = new LinkedList<>();

	/**
	 * Tracks if the caret should be visible (for blinking animation).
	 */
	private boolean caretVisible = true;

	/**
	 * Timestamp of last caret blink toggle.
	 */
	private long lastCaretBlink = System.currentTimeMillis();

	/**
	 * Horizontal scroll offset for text that overflows the visible area.
	 */
	private int scrollOffset = 0;

	/**
	 * Simulated clipboard for copy/paste operations.
	 * TODO: Replace with native browser clipboard API when available.
	 */
	private static String clipboard = "";

	/**
	 * Padding inside the text field (left and right).
	 */
	private static final int PADDING_X = 5;

	/**
	 * Padding inside the text field (top and bottom).
	 */
	private static final int PADDING_Y = 7;

	private static final int SELECTION_PADDING_Y = 3;

	/**
	 * Caret blink interval in milliseconds.
	 */
	private static final long CARET_BLINK_INTERVAL = 500;

	/**
	 * Creates a text field with empty text.
	 */
	public TTextField() {
		this("", 0);
	}

	/**
	 * Creates a text field with the specified initial text.
	 *
	 * @param text the initial text
	 */
	public TTextField(String text) {
		this(text, 0);
	}

	/**
	 * Creates a text field with the specified initial text and number of columns.
	 *
	 * @param text    the initial text
	 * @param columns the number of columns (affects preferred width, 0 = auto)
	 */
	public TTextField(String text, int columns) {
		this.text = text != null ? text : "";
		this.columns = columns;
		this.caretPosition = this.text.length();
		this.actionCommand = null;

		// Set a lighter background color (matching TButton's light gray)
		setBackground(new Color(250, 250, 250));

		// Set focusable by default
		setFocusable(true);

		// Add key listener for text editing
		addKeyListener(new TKeyListener() {
			@Override
			public void keyTyped(TKeyEvent e) {
				handleKeyTyped(e);
			}

			@Override
			public void keyPressed(TKeyEvent e) {
				handleKeyPressed(e);
			}

			@Override
			public void keyReleased(TKeyEvent e) {
				// Not used for now
			}
		});

		// Add mouse listener for caret positioning and text selection
		addMouseListener(new TMouseListener() {
			@Override
			public void mouseClicked(TMouseEvent e) {
				// Single click positions caret
				int clickPos = getCaretPositionFromX(e.getX());
				setCaretPosition(clickPos);
				clearSelection();
				repaint();
			}

			@Override
			public void mousePressed(TMouseEvent e) {
				// Start of potential drag selection
				requestFocus();
				int clickPos = getCaretPositionFromX(e.getX());
				setCaretPosition(clickPos);
				selectionStart = clickPos;
				selectionEnd = clickPos;
				repaint();
			}

			@Override
			public void mouseReleased(TMouseEvent e) {
				// End of drag selection
				if (selectionStart == selectionEnd) {
					clearSelection();
				}
				repaint();
			}

			@Override
			public void mouseEntered(TMouseEvent e) {
				// Could change cursor style here
			}

			@Override
			public void mouseExited(TMouseEvent e) {
				// Could restore cursor style here
			}
		});

		// Add mouse motion listener for drag selection
		addMouseMotionListener(new TMouseMotionListener() {
			@Override
			public void mouseDragged(TMouseEvent e) {
				// Extend selection while dragging
				int dragPos = getCaretPositionFromX(e.getX());
				selectionEnd = dragPos;
				setCaretPosition(dragPos);
				repaint();
			}

			@Override
			public void mouseMoved(TMouseEvent e) {
				// Not used for now
			}
		});

		// Add focus listener for caret visibility
		addFocusListener(new TFocusListener() {
			@Override
			public void focusGained(TFocusEvent e) {
				caretVisible = true; // Start with caret visible
				lastCaretBlink = System.currentTimeMillis();
				toggleCaret();
				repaint();
			}

			@Override
			public void focusLost(TFocusEvent e) {
				caretVisible = false;
				repaint();
			}
		});
	}

	/**
	 * Sets the text content of this text field.
	 *
	 * @param text the new text
	 */
	public void setText(String text) {
		this.text = text != null ? text : "";
		// Adjust caret position if it's beyond the new text length
		if (caretPosition > this.text.length()) {
			caretPosition = this.text.length();
		}
		clearSelection();
		adjustScrollOffset();
		repaint();
	}

	/**
	 * Sets the caret position.
	 *
	 * @param position the new caret position (clamped to valid range)
	 */
	public void setCaretPosition(int position) {
		this.caretPosition = Math.max(0, Math.min(position, text.length()));
		adjustScrollOffset();
	}

	/**
	 * Gets the start of the text selection.
	 *
	 * @return the selection start index, or the caret position if no selection
	 */
	public int getSelectionStart() {
		if (selectionStart < 0 || selectionEnd < 0) {
			return caretPosition;
		}
		return Math.min(selectionStart, selectionEnd);
	}

	/**
	 * Gets the end of the text selection.
	 *
	 * @return the selection end index, or the caret position if no selection
	 */
	public int getSelectionEnd() {
		if (selectionStart < 0 || selectionEnd < 0) {
			return caretPosition;
		}
		return Math.max(selectionStart, selectionEnd);
	}

	/**
	 * Selects text between the specified start and end positions.
	 *
	 * @param start the start position
	 * @param end   the end position
	 */
	public void select(int start, int end) {
		selectionStart = Math.max(0, Math.min(start, text.length()));
		selectionEnd = Math.max(0, Math.min(end, text.length()));
		caretPosition = selectionEnd;
		repaint();
	}

	/**
	 * Selects all text in the field.
	 */
	public void selectAll() {
		select(0, text.length());
	}

	/**
	 * Gets the selected text.
	 *
	 * @return the selected text, or empty string if no selection
	 */
	public String getSelectedText() {
		int start = getSelectionStart();
		int end = getSelectionEnd();
		if (start == end) {
			return "";
		}
		return text.substring(start, end);
	}

	/**
	 * Clears the current selection.
	 */
	private void clearSelection() {
		selectionStart = -1;
		selectionEnd = -1;
	}

	/**
	 * Adds the specified action listener to receive action events from this text
	 * field.
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
	 * This is called when the user presses Enter.
	 */
	protected void fireActionPerformed() {
		String command = actionCommand != null ? actionCommand : text;
		TActionEvent event = new TActionEvent(
				this,
				TActionEvent.ACTION_PERFORMED,
				command,
				System.currentTimeMillis(),
				0);

		for (TActionListener listener : actionListeners) {
			listener.actionPerformed(event);
		}
	}

	/**
	 * Handles key typed events (character input).
	 *
	 * @param e the key event
	 */
	private void handleKeyTyped(TKeyEvent e) {
		if (!editable) {
			return;
		}

		char keyChar = e.getKeyChar();

		// Ignore control characters except for specific ones we handle in keyPressed
		if (Character.isISOControl(keyChar)) {
			return;
		}

		// Insert the character at the caret position
		deleteSelection();
		text = text.substring(0, caretPosition) + keyChar + text.substring(caretPosition);
		caretPosition++;
		adjustScrollOffset();
		repaint();
	}

	/**
	 * Handles key pressed events (special keys and shortcuts).
	 *
	 * @param e the key event
	 */
	private void handleKeyPressed(TKeyEvent e) {
		int keyCode = e.getKeyCode();
		boolean ctrl = (e.getModifiers() & TKeyEvent.FLAG_CTRL) != 0;
		boolean shift = (e.getModifiers() & TKeyEvent.FLAG_SHIFT) != 0;

		// Handle Enter key (fire action event)
		if (keyCode == TKeyEvent.VK_ENTER) {
			fireActionPerformed();
			e.consume();
			return;
		}

		// Handle Tab key (focus traversal - let the focus manager handle it)
		if (keyCode == TKeyEvent.VK_TAB) {
			// Don't consume - allow focus traversal
			return;
		}

		if (!editable) {
			// Allow navigation even when not editable
			if (keyCode == TKeyEvent.VK_LEFT || keyCode == TKeyEvent.VK_RIGHT ||
					keyCode == TKeyEvent.VK_HOME || keyCode == TKeyEvent.VK_END) {
				handleNavigation(keyCode, shift);
			}
			return;
		}

		// Handle Ctrl+A (select all)
		if (ctrl && keyCode == TKeyEvent.VK_A) {
			selectAll();
			e.consume();
			return;
		}

		// Handle Ctrl+C (copy)
		if (ctrl && keyCode == TKeyEvent.VK_C) {
			clipboard = getSelectedText();
			log.debug("Copied to clipboard: {}", clipboard);
			e.consume();
			return;
		}

		// Handle Ctrl+X (cut)
		if (ctrl && keyCode == TKeyEvent.VK_X) {
			clipboard = getSelectedText();
			deleteSelection();
			log.debug("Cut to clipboard: {}", clipboard);
			e.consume();
			repaint();
			return;
		}

		// Handle Ctrl+V (paste)
		if (ctrl && keyCode == TKeyEvent.VK_V) {
			if (!clipboard.isEmpty()) {
				deleteSelection();
				text = text.substring(0, caretPosition) + clipboard + text.substring(caretPosition);
				caretPosition += clipboard.length();
				adjustScrollOffset();
				log.debug("Pasted from clipboard: {}", clipboard);
				repaint();
			}
			e.consume();
			return;
		}

		// Handle backspace
		if (keyCode == TKeyEvent.VK_BACK_SPACE) {
			if (hasSelection()) {
				deleteSelection();
			} else if (caretPosition > 0) {
				text = text.substring(0, caretPosition - 1) + text.substring(caretPosition);
				caretPosition--;
				adjustScrollOffset();
			}
			e.consume();
			repaint();
			return;
		}

		// Handle delete
		if (keyCode == TKeyEvent.VK_DELETE) {
			if (hasSelection()) {
				deleteSelection();
			} else if (caretPosition < text.length()) {
				text = text.substring(0, caretPosition) + text.substring(caretPosition + 1);
			}
			e.consume();
			repaint();
			return;
		}

		// Handle arrow keys and Home/End
		if (keyCode == TKeyEvent.VK_LEFT || keyCode == TKeyEvent.VK_RIGHT ||
				keyCode == TKeyEvent.VK_HOME || keyCode == TKeyEvent.VK_END) {
			handleNavigation(keyCode, shift);
			e.consume();
			return;
		}
	}

	/**
	 * Handles navigation keys (arrows, Home, End) with optional Shift for
	 * selection.
	 *
	 * @param keyCode the key code
	 * @param shift   whether Shift is pressed
	 */
	private void handleNavigation(int keyCode, boolean shift) {
		int oldCaretPos = caretPosition;

		if (shift && !hasSelection()) {
			// Start a new selection
			selectionStart = caretPosition;
		}

		switch (keyCode) {
			case TKeyEvent.VK_LEFT:
				if (caretPosition > 0) {
					caretPosition--;
				}
				break;
			case TKeyEvent.VK_RIGHT:
				if (caretPosition < text.length()) {
					caretPosition++;
				}
				break;
			case TKeyEvent.VK_HOME:
				caretPosition = 0;
				break;
			case TKeyEvent.VK_END:
				caretPosition = text.length();
				break;
		}

		if (shift) {
			// Extend selection
			selectionEnd = caretPosition;
		} else {
			// Clear selection when navigating without shift
			clearSelection();
		}

		adjustScrollOffset();
		repaint();
	}

	/**
	 * Checks if there is an active text selection.
	 *
	 * @return true if text is selected
	 */
	private boolean hasSelection() {
		return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
	}

	/**
	 * Deletes the currently selected text.
	 */
	private void deleteSelection() {
		if (!hasSelection()) {
			return;
		}
		int start = getSelectionStart();
		int end = getSelectionEnd();
		text = text.substring(0, start) + text.substring(end);
		caretPosition = start;
		clearSelection();
		adjustScrollOffset();
	}

	/**
	 * Converts a mouse X coordinate to a caret position in the text.
	 *
	 * @param x the X coordinate relative to the component
	 * @return the closest caret position
	 */
	private int getCaretPositionFromX(int x) {
		TGraphics g = getGraphics();
		if (g == null) {
			return 0;
		}

		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
			g.setFont(font);
		}

		TFontMetrics fm = g.getFontMetrics(font);

		// Adjust for padding and scroll offset
		int adjustedX = x - PADDING_X + scrollOffset;

		// Find the closest character position
		int currentX = 0;
		for (int i = 0; i <= text.length(); i++) {
			if (i > 0) {
				String substr = text.substring(0, i);
				currentX = fm.stringWidth(substr);
			}

			if (adjustedX < currentX) {
				// Check if we're closer to the previous position
				if (i > 0) {
					String prevSubstr = text.substring(0, i - 1);
					int prevX = fm.stringWidth(prevSubstr);
					int midpoint = (prevX + currentX) / 2;
					if (adjustedX < midpoint) {
						return i - 1;
					}
				}
				return i;
			}
		}

		return text.length();
	}

	/**
	 * Adjusts the horizontal scroll offset to keep the caret visible.
	 */
	private void adjustScrollOffset() {
		TGraphics g = getGraphics();
		if (g == null) {
			return;
		}

		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
		}

		TFontMetrics fm = g.getFontMetrics(font);
		String textBeforeCaret = text.substring(0, caretPosition);
		int caretX = fm.stringWidth(textBeforeCaret);

		int availableWidth = getWidth() - 2 * PADDING_X;

		// Scroll right if caret is beyond visible area
		if (caretX - scrollOffset > availableWidth) {
			scrollOffset = caretX - availableWidth;
		}

		// Scroll left if caret is before visible area
		if (caretX < scrollOffset) {
			scrollOffset = caretX;
		}

		// Don't scroll left past the beginning
		if (scrollOffset < 0) {
			scrollOffset = 0;
		}
	}

	/**
	 * Paints the text field with border, text, caret, and selection.
	 *
	 * @param g the graphics context to paint on
	 */
	@Override
	public void paint(TGraphics g) {
		int w = getWidth();
		int h = getHeight();

		// Draw background
		Color bgColor = getBackground();
		if (bgColor == null) {
			bgColor = Color.WHITE;
		}
		g.setColor(bgColor);
		g.fillRect(0, 0, w, h);

		// Draw border
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, w - 1, h - 1);

		// Set up font
		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
			g.setFont(font);
		}

		TFontMetrics fm = g.getFontMetrics(font);

		// Calculate text baseline
		int textY = PADDING_Y + fm.getAscent();

		// Draw selection highlight if present
		if (hasSelection()) {
			int start = getSelectionStart();
			int end = getSelectionEnd();

			String textBeforeStart = text.substring(0, start);
			String selectedText = text.substring(start, end);

			int startX = PADDING_X + fm.stringWidth(textBeforeStart) - scrollOffset;
			int selWidth = fm.stringWidth(selectedText);

			// Clip selection to visible area
			int clipLeft = Math.max(PADDING_X, startX);
			int clipRight = Math.min(w - PADDING_X, startX + selWidth);

			if (clipRight > clipLeft) {
				g.setColor(new Color(180, 200, 255)); // Light blue selection
				g.fillRect(clipLeft, SELECTION_PADDING_Y, clipRight - clipLeft, h - 2 * SELECTION_PADDING_Y);
			}
		}

		// Draw text (clipped to visible area)
		Color fgColor = getForeground();
		if (fgColor == null) {
			fgColor = Color.BLACK;
		}
		g.setColor(fgColor);

		// Draw the text directly without creating a clipped graphics
		// The text baseline should be positioned correctly
		g.drawString(text, PADDING_X - scrollOffset, textY);

		// Draw caret if focused and editable
		if (isFocusOwner() && editable && caretVisible) {
			String textBeforeCaret = text.substring(0, caretPosition);
			int caretX = PADDING_X + fm.stringWidth(textBeforeCaret) - scrollOffset;

			// Only draw caret if it's within visible bounds
			if (caretX >= PADDING_X && caretX <= w) {
				g.setColor(Color.BLACK);
				g.drawLine(caretX, PADDING_Y / 2, caretX, h - PADDING_Y / 2);
			}
		}
	}

	private void toggleCaret() {
		caretVisible = !caretVisible;

		if (isFocusOwner() && editable) {
			ThreadUtils.runOnce("Caret-blinker-" + hashCode(), this::toggleCaret, CARET_BLINK_INTERVAL);
		}
		repaint();
	}

	/**
	 * Returns the preferred size of this text field based on columns or current
	 * text.
	 *
	 * @return the preferred dimensions of this text field
	 */
	public TDimension getPreferredSize() {
		// Check if explicitly set
		TDimension explicit = super.getPreferredSize();
		if (explicit != null) {
			return explicit;
		}

		// Try to get graphics for measurements
		TGraphics g = null;
		try {
			g = getGraphics();
		} catch (Exception e) {
			// Graphics not available yet, use fallback
		}

		if (g == null) {
			// Fallback based on columns or text length
			int charWidth = 8; // Approximate character width
			int width = columns > 0 ? columns * charWidth : Math.max(100, text.length() * charWidth);
			return new TDimension(width + 2 * PADDING_X, 25);
		}

		TFont font = getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
		}

		TFontMetrics fm = g.getFontMetrics(font);

		int width;
		if (columns > 0) {
			// Use columns to determine width (measure 'n' character as average)
			width = columns * fm.stringWidth("n");
		} else {
			// Use text length, with minimum width
			width = Math.max(100, fm.stringWidth(text));
		}

		int height = fm.getHeight();

		return new TDimension(width + 2 * PADDING_X, height + 2 * PADDING_Y);
	}

	/**
	 * Returns the minimum size of this text field.
	 *
	 * @return the minimum dimensions of this text field
	 */
	public TDimension getMinimumSize() {
		// Check if explicitly set
		TDimension explicit = super.getMinimumSize();
		if (explicit != null) {
			return explicit;
		}

		TGraphics g = null;
		try {
			g = getGraphics();
		} catch (Exception e) {
			// Graphics not available yet
		}

		if (g == null) {
			return new TDimension(50 + 2 * PADDING_X, 20);
		}

		TFont font = g.getFont();
		if (font == null) {
			font = new TFont("SansSerif", TFont.PLAIN, 12);
		}

		TFontMetrics fm = g.getFontMetrics(font);
		int height = fm.getHeight();

		return new TDimension(50 + 2 * PADDING_X, height + 2 * PADDING_Y);
	}

	/**
	 * Checks if this component currently owns the focus.
	 *
	 * @return true if this component has focus
	 */
	private boolean isFocusOwner() {
		// Check with focus manager if available
		me.mdbell.awtea.classlib.java.awt.awtea.TFocusManager focusManager = me.mdbell.awtea.classlib.java.awt.awtea.TFocusManager
				.get();
		return focusManager != null && focusManager.getGlobalFocusOwner() == this;
	}
}
