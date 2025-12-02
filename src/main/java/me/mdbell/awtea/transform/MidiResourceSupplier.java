package me.mdbell.awtea.transform;

import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

public class MidiResourceSupplier implements ResourceSupplier {
    @Override
    public String[] supplyResources(ResourceSupplierContext context) {
        return new String[]{
                "sound/pcm-processor.js"
        };
    }
}
