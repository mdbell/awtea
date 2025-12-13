package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.interop.PlatformMarker;

@DetourReceiver(target = System.class)
@NoDetours
public class SystemDetour {

	private static final Logger log = LoggerFactory.getLogger(SystemDetour.class);

	@DetourMethod("exit")
	public static void exit(int exitCode) {
		if (!isTeaVM()) {
			System.exit(exitCode);
		} else {
			log.warn("System.exit({}) called!", exitCode);
		}
	}

	@PlatformMarker
	private static boolean isTeaVM() {
		return false;
	}
}
