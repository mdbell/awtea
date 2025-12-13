package me.mdbell.awtea.classlib.javax.sound.sampled;

import lombok.*;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TControl {

	private final Type type;

	@ToString
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public static class Type {

		private final String name;

	}
}
