package me.mdbell.awtea.gl;

import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Registers {@link EmbedTransformer}. Discovered via
 * {@code META-INF/services/org.teavm.vm.spi.TeaVMPlugin}, so any build with
 * awtea-graphics on the classpath embeds {@code @ShaderSource}/
 * {@code @CSSSource} resources automatically.
 */
public class EmbedPlugin implements TeaVMPlugin {

    @Override
    public void install(TeaVMHost host) {
        host.add(new EmbedTransformer(host.getClassLoader()));
    }
}
