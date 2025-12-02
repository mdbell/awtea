package me.mdbell.awtea.transform;

import lombok.SneakyThrows;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class FontResourceSupplier implements ResourceSupplier {
    @SneakyThrows
    @Override
    public String[] supplyResources(ResourceSupplierContext context) {
//        return new String[]{
//			"fonts/NotoSans.ttf"
//        };
        Path root = Path.of(
                Objects.requireNonNull(context.getClassLoader().getResource("fonts")).toURI()
        );
		File dir = root.toFile();
		String[] fonts = dir.list();
		for(int i = 0; i < fonts.length; i++){
			fonts[i] = "fonts/" + fonts[i];
		}
        return fonts;
    }
}
