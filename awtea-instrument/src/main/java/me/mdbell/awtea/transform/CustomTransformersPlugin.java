package me.mdbell.awtea.transform;

import me.mdbell.awtea.instrument.ArrayHacks;
import me.mdbell.awtea.instrument.DetourHacks;
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
		host.add(new MonitorHacks());
		host.add(new ArrayHacks());
	}

}
