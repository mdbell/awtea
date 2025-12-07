package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.EventObject;

@Getter
@ToString
public class TLineEvent extends EventObject {

	private final Type type;

	private final long framePosition;

	public TLineEvent(TLine line, Type type, long framePosition) {
		super(line);
		this.type = type;
		this.framePosition = framePosition;
	}

	public final TLine getLine() {
		return (TLine) getSource();
	}


	@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Type {
		private final String name;

		public static final Type OPEN = new Type("Open");
		public static final Type CLOSE = new Type("Close");
		public static final Type START = new Type("Start");
		public static final Type STOP = new Type("Stop");
	}
}
