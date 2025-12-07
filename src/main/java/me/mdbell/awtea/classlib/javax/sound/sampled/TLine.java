package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.Getter;
import lombok.ToString;

/**
 * @see javax.sound.sampled.Line
 */
public interface TLine extends AutoCloseable {

	TLine.Info getLineInfo();

	void open() throws TLineUnavailableException;

	@Override
	void close();

	boolean isOpen();

	TControl[] getControls();

	boolean isControlSupported(TControl.Type control);

	TControl getControl(TControl.Type control);

	void addLineListener(TLineListener listener);

	void removeLineListener(TLineListener listener);

	@Getter
	@ToString
	class Info {

		private final Class<?> lineClass;

		public Info(Class<?> lineClass) {
			if (lineClass == null) {
				this.lineClass = TLine.class;
			} else {
				this.lineClass = lineClass;
			}
		}

		public boolean matches(TLine.Info info) {
			if (!this.getClass().isInstance(info)) {
				return false;
			}
			return lineClass.isAssignableFrom(info.getLineClass());
		}
	}
}
