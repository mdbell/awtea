package me.mdbell.awtea.transform;

import lombok.SneakyThrows;
import org.teavm.classlib.ResourceSupplier;
import org.teavm.classlib.ResourceSupplierContext;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FontResourceSupplier implements ResourceSupplier {
	@SneakyThrows
	@Override
	public String[] supplyResources(ResourceSupplierContext context) {
		List<String> result = new ArrayList<>();

		Enumeration<URL> urls = context.getClassLoader().getResources("fonts");

		while(urls.hasMoreElements()) {
			URL url = urls.nextElement();
			List<String> fonts = listFontsInUrl(url);
			result.addAll(fonts);
		}
		return result.toArray(new String[0]);
	}

	private List<String> listFontsInUrl(URL url) throws IOException, URISyntaxException {
		List<String> result = new ArrayList<>();
		String protocol = url.getProtocol();

		if ("file".equals(protocol)) {
			// resources are exploded on disk: e.g. build/resources/main/fonts
			Path dir = Path.of(url.toURI());
			if (Files.isDirectory(dir)) {
				try (var stream = Files.list(dir)) {
					stream
						.filter(Files::isRegularFile)
						.forEach(p -> result.add("fonts/" + p.getFileName().toString()));
				}
			}
		} else if ("jar".equals(protocol)) {
			// resources are inside a jar: jar:file:/.../awtea.jar!/fonts
			JarURLConnection conn = (JarURLConnection) url.openConnection();
			try (JarFile jar = conn.getJarFile()) {
				String prefix = conn.getEntryName();
				if (prefix == null || prefix.isEmpty()) {
					prefix = "fonts";
				}
				if (!prefix.endsWith("/")) {
					prefix += "/";
				}

				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					// pick only files directly or recursively under fonts/
					if (!entry.isDirectory() && name.startsWith(prefix)) {
						result.add(name);
					}
				}
			}
		}
		return result;
	}
}
