package me.mdbell.awtea.transform;

import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

public class CustomTransformersPlugin implements TeaVMPlugin {

	@Override
	public void install(TeaVMHost host) {
		host.add(DetourHacks.fromResource("META-INF/awtea.detours"));
		host.add(new ArrayHacks());
	}

}
