package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import org.teavm.interop.PlatformMarker;

import java.text.MessageFormat;

@DetourReceiver(target = System.class)
@NoDetours
public class SystemDetour {

	@DetourMethod("exit")
	public static void exit(int exitCode) {
		if (!isTeaVM()) {
			System.exit(exitCode);
		} else {
			System.err.println(MessageFormat.format("System.exit({0}) called!", exitCode));
		}
	}

	@PlatformMarker
	private static boolean isTeaVM() {
		return false;
	}
}
