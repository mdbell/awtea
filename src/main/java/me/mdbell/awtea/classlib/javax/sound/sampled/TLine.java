package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;

import javax.sound.sampled.*;

/**
 * @see Line
 */
public interface TLine extends AutoCloseable {

    /**
     * Obtains the {@code Line.Info} object describing this line.
     *
     * @return description of the line
     */
    Line.Info getLineInfo();

    /**
     * Opens the line, indicating that it should acquire any required system
     * resources and become operational. If this operation succeeds, the line is
     * marked as open, and an {@code OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in an {@code LineUnavailableException}.
     * <p>
     * Some types of lines have configurable properties that may affect resource
     * allocation. For example, a {@code DataLine} must be opened with a
     * particular format and buffer size. Such lines should provide a mechanism
     * for configuring these properties, such as an additional {@code open}
     * method or methods which allow an application to specify the desired
     * settings.
     * <p>
     * This method takes no arguments, and opens the line with the current
     * settings. For {@link SourceDataLine} and {@link TargetDataLine} objects,
     * this means that the line is opened with default settings. For a
     * {@link Clip}, however, the buffer size is determined when data is loaded.
     * Since this method does not allow the application to specify any data to
     * load, an {@code IllegalArgumentException} is thrown. Therefore, you
     * should instead use one of the {@code open} methods provided in the
     * {@code Clip} interface to load data into the {@code Clip}.
     * <p>
     * For {@code DataLine}'s, if the {@code DataLine.Info} object which was
     * used to retrieve the line, specifies at least one fully qualified audio
     * format, the last one will be used as the default format.
     *
     * @throws IllegalArgumentException if this method is called on a Clip
     *                                  instance
     * @throws LineUnavailableException if the line cannot be opened due to
     *                                  resource restrictions
     * @throws SecurityException        if the line cannot be opened due to security
     *                                  restrictions
     * @see #close
     * @see #isOpen
     * @see LineEvent
     * @see DataLine
     * @see Clip#open(AudioFormat, byte[], int, int)
     * @see Clip#open(AudioInputStream)
     */
    void open() throws LineUnavailableException;

    /**
     * Indicates whether the line is open, meaning that it has reserved system
     * resources and is operational, although it might not currently be playing
     * or capturing sound.
     *
     * @return {@code true} if the line is open, otherwise {@code false}
     * @see #open()
     * @see #close()
     */
    boolean isOpen();

    /**
     * Obtains the set of controls associated with this line. Some controls may
     * only be available when the line is open. If there are no controls, this
     * method returns an array of length 0.
     *
     * @return the array of controls
     * @see #getControl
     */
    Control[] getControls();

    /**
     * Indicates whether the line supports a control of the specified type. Some
     * controls may only be available when the line is open.
     *
     * @param control the type of the control for which support is queried
     * @return {@code true} if at least one control of the specified type is
     * supported, otherwise {@code false}
     */
    boolean isControlSupported(Control.Type control);

    /**
     * Obtains a control of the specified type, if there is any. Some controls
     * may only be available when the line is open.
     *
     * @param control the type of the requested control
     * @return a control of the specified type
     * @throws IllegalArgumentException if a control of the specified type is
     *                                  not supported
     * @see #getControls
     * @see #isControlSupported(Control.Type control)
     */
    Control getControl(Control.Type control);

    /**
     * Adds a listener to this line. Whenever the line's status changes, the
     * listener's {@code update()} method is called with a {@code LineEvent}
     * object that describes the change.
     *
     * @param listener the object to add as a listener to this line
     * @see #removeLineListener
     * @see LineListener#update
     * @see LineEvent
     */
    void addLineListener(LineListener listener);

    /**
     * Removes the specified listener from this line's list of listeners.
     *
     * @param listener listener to remove
     * @see #addLineListener
     */
    void removeLineListener(LineListener listener);

	@Getter
	class Info {

		protected Class<? extends TDataLine> lineClass;

		public Info(Class<? extends TDataLine> lineClass) {
			this.lineClass = lineClass;
		}
	}
}
