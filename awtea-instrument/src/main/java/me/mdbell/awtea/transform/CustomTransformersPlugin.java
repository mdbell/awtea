package me.mdbell.awtea.transform;

import me.mdbell.awtea.instrument.ArrayHacks;
import me.mdbell.awtea.instrument.DetourHacks;
import me.mdbell.awtea.instrument.EmbedResourceTransformer;
import me.mdbell.awtea.instrument.MonitorHacks;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class CustomTransformersPlugin implements TeaVMPlugin {

	@Override
	public void install(TeaVMHost host) {
		DetourHacks detours = DetourHacks.fromResource("META-INF/awtea.detours");
		host.add(detours);
		// warnings only: the classlib detours are opportunistic platform
		// fixes, and an app need not use every API they cover
		host.add(detours.zeroMatchVerifier(false));
		host.add(new EmbedResourceTransformer(host.getClassLoader()));
		// JS backend only: the monitor wrappers call synchronized code
		// (OperationsMonitor.ensureEntry), which wasm-gc classifies as
		// suspendable — that poisons every @Monitored method, and JS-driven
		// callbacks (requestAnimationFrame, DOM events) into suspendable code
		// trap in Fiber.isResuming with no current fiber. The monitors feed
		// the awtea-ui debug frames, which are a JS-side concern anyway.
		if (host.getExtension(org.teavm.backend.javascript.TeaVMJavaScriptHost.class) != null) {
			host.add(new MonitorHacks());
		}
		host.add(new ArrayHacks());
		// Debug method-entry tracing, off unless the property is set (see
		// TraceHacks). Comma-separated class-name prefixes.
		String tracePrefixes = host.getProperties().getProperty("awtea.trace");
		if (tracePrefixes != null && !tracePrefixes.isEmpty()) {
			host.add(new me.mdbell.awtea.instrument.TraceHacks(tracePrefixes));
		}
	}

}
