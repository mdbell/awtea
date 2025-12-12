package me.mdbell.awtea.util;

import me.mdbell.awtea.util.coverage.ClassCoverage;
import me.mdbell.awtea.util.coverage.CoverageData;
import me.mdbell.awtea.util.coverage.HtmlReportGenerator;
import me.mdbell.awtea.util.coverage.MarkdownReportGenerator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ApiDiff {

	private static final String AWTEA_ROOT_PACKAGE = "me.mdbell.awtea.classlib";
	private static final String AWTEA_PREFIX = AWTEA_ROOT_PACKAGE + ".";
	private static final String CLASS_PREFIX = "T";

	private static int globalRuntimeTotal = 0;
	private static int globalImplementedTotal = 0;
	private static CoverageData coverageData = null;


	public static void main(String[] args) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		String outputFormat = null;
		String outputPath = null;
		boolean checkMissingClasses = false;
		List<String> packagesToScan = new ArrayList<>();
		List<String> classesToCheckList = new ArrayList<>();
		
		// Parse command-line arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--format") && i + 1 < args.length) {
				outputFormat = args[++i];
			} else if (args[i].equals("--output") && i + 1 < args.length) {
				outputPath = args[++i];
			} else if (args[i].equals("--missing-classes")) {
				checkMissingClasses = true;
			} else if (args[i].equals("--packages") && i + 1 < args.length) {
				String pkgs = args[++i];
				packagesToScan.addAll(Arrays.asList(pkgs.split(",")));
			} else if (args[i].equals("--help") || args[i].equals("-h")) {
				printUsage();
				return;
			} else {
				classesToCheckList.add(args[i]);
			}
		}
		
		// If checking missing classes and no packages specified, default to java.awt.*
		if (checkMissingClasses && packagesToScan.isEmpty()) {
			packagesToScan.add("java.awt");
			packagesToScan.add("java.awt.event");
			packagesToScan.add("java.awt.geom");
			packagesToScan.add("java.awt.image");
		}
		
		if (checkMissingClasses) {
			findMissingClasses(packagesToScan, loader);
			return;
		}
		
		String[] classesToCheck = classesToCheckList.isEmpty() ? null : 
			classesToCheckList.toArray(new String[0]);

		// Initialize coverage data if generating a report
		if (outputFormat != null) {
			coverageData = new CoverageData();
			coverageData.setTimestamp(LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}

		String[] teavmClasses = findClassesInPackage(AWTEA_ROOT_PACKAGE, loader);

		for (String teavmName : teavmClasses) {
			String runtimeName = mapTeaVmToRuntimeClassName(teavmName);
			if (runtimeName == null) {
				continue;
			};

			if(classesToCheck != null) {
				boolean matched = false;
				for (String cls : classesToCheck) {
					if (teavmName.equals(cls) || runtimeName.equals(cls)) {
						matched = true;
						break;
					}
				}
				if (!matched) {
					continue;
				}
			}

			try {
				Class<?> teavmClass = Class.forName(teavmName, false, loader);
				Class<?> runtimeClass = Class.forName(runtimeName, false, loader);

				compareClasses(teavmClass, runtimeClass);
			} catch (ClassNotFoundException e) {
				// ignored
//				System.out.printf("No runtime class for %s -> %s%n", teavmName, runtimeName);
			} catch (Throwable t) {
				System.out.printf("Error loading %s or %s: %s%n", teavmName, runtimeName, t);
			}
		}

		// Print console summary if not generating a report
		if (outputFormat == null) {
			System.out.println("======================================================");
			System.out.println("Global API Coverage Summary:");
			System.out.printf("Total covered: %d / %d (%.1f%%)%n",
				globalImplementedTotal,
				globalRuntimeTotal,
				globalRuntimeTotal == 0 ? 100.0 : (100.0 * globalImplementedTotal / globalRuntimeTotal));
			System.out.println("======================================================");
		} else {
			// Generate report
			generateReport(outputFormat, outputPath);
		}

	}

	private static void printUsage() {
		System.out.println("Usage: ApiDiff [options] [classNames...]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  --format <html|markdown>  Generate report in specified format");
		System.out.println("  --output <path>           Output file path (default: docs/coverage/report.<ext>)");
		System.out.println("  --missing-classes         Check for missing public classes in packages");
		System.out.println("  --packages <pkg1,pkg2>    Comma-separated list of packages to scan (default: java.awt.*)");
		System.out.println("  --help, -h                Show this help message");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  ApiDiff                                    # Console output");
		System.out.println("  ApiDiff --format html                      # Generate HTML report");
		System.out.println("  ApiDiff --format markdown --output out.md  # Generate Markdown report");
		System.out.println("  ApiDiff --missing-classes                  # Find missing classes in java.awt.*");
		System.out.println("  ApiDiff --missing-classes --packages javax.swing,javax.sound.sampled");
	}

	private static void findMissingClasses(List<String> packagesToScan, ClassLoader loader) {
		System.out.println("Searching for missing public classes in packages:");
		for (String pkg : packagesToScan) {
			System.out.println("  - " + pkg);
		}
		System.out.println();
		
		Set<String> implementedClasses = new HashSet<>();
		
		// First, collect all classes that awtea implements
		String[] teavmClasses = findClassesInPackage(AWTEA_ROOT_PACKAGE, loader);
		for (String teavmName : teavmClasses) {
			String runtimeName = mapTeaVmToRuntimeClassName(teavmName);
			if (runtimeName != null) {
				implementedClasses.add(runtimeName);
			}
		}
		
		// Now scan for public classes in runtime JDK
		Map<String, List<String>> missingByPackage = new TreeMap<>();
		int totalMissing = 0;
		int totalFound = 0;
		
		for (String pkgToScan : packagesToScan) {
			List<String> missingInPackage = new ArrayList<>();
			
			// For system packages, we need to use rt.jar or system module
			// Try to find classes using Package.getPackage and scanning available classes
			try {
				// Use ClassGraph or manual scanning of rt.jar
				// For simplicity, we'll list known AWT classes
				Set<String> knownClasses = getKnownClassesInPackage(pkgToScan);
				
				for (String className : knownClasses) {
					try {
						Class<?> runtimeClass = Class.forName(className, false, loader);
						
						// Check if it's a public class
						if (Modifier.isPublic(runtimeClass.getModifiers())) {
							totalFound++;
							if (!implementedClasses.contains(className)) {
								missingInPackage.add(className);
								totalMissing++;
							}
						}
					} catch (ClassNotFoundException e) {
						// Skip classes that can't be loaded
					}
				}
			} catch (Exception e) {
				System.err.println("Error scanning package " + pkgToScan + ": " + e.getMessage());
			}
			
			if (!missingInPackage.isEmpty()) {
				Collections.sort(missingInPackage);
				missingByPackage.put(pkgToScan, missingInPackage);
			}
		}
		
		// Print results
		System.out.println("======================================================");
		System.out.println("Missing Classes Report");
		System.out.println("======================================================");
		System.out.printf("Total public classes found: %d%n", totalFound);
		System.out.printf("Total missing classes: %d%n", totalMissing);
		System.out.printf("Coverage: %d / %d (%.1f%%)%n", 
			totalFound - totalMissing, totalFound, 
			totalFound == 0 ? 100.0 : (100.0 * (totalFound - totalMissing) / totalFound));
		System.out.println("======================================================");
		System.out.println();
		
		if (totalMissing > 0) {
			for (Map.Entry<String, List<String>> entry : missingByPackage.entrySet()) {
				String pkg = entry.getKey();
				List<String> missing = entry.getValue();
				
				System.out.printf("Package: %s (%d missing)%n", pkg, missing.size());
				for (String className : missing) {
					System.out.println("  - " + className);
				}
				System.out.println();
			}
		} else {
			System.out.println("✓ All public classes are implemented!");
		}
	}
	
	private static Set<String> getKnownClassesInPackage(String pkgName) {
		Set<String> classes = new HashSet<>();
		
		// This is a curated list of known public AWT classes
		// In a real implementation, you'd scan rt.jar or use ClassGraph library
		if (pkgName.equals("java.awt")) {
			classes.addAll(Arrays.asList(
				"java.awt.AWTError", "java.awt.AWTEvent", "java.awt.AWTEventMulticaster",
				"java.awt.AWTException", "java.awt.AWTKeyStroke", "java.awt.AWTPermission",
				"java.awt.ActiveEvent", "java.awt.Adjustable", "java.awt.AlphaComposite",
				"java.awt.BasicStroke", "java.awt.BorderLayout", "java.awt.BufferCapabilities",
				"java.awt.Button", "java.awt.Canvas", "java.awt.CardLayout",
				"java.awt.Checkbox", "java.awt.CheckboxGroup", "java.awt.CheckboxMenuItem",
				"java.awt.Choice", "java.awt.Color", "java.awt.Component",
				"java.awt.ComponentOrientation", "java.awt.Composite", "java.awt.Container",
				"java.awt.ContainerOrderFocusTraversalPolicy", "java.awt.Cursor", "java.awt.DefaultFocusTraversalPolicy",
				"java.awt.DefaultKeyboardFocusManager", "java.awt.Desktop", "java.awt.Dialog",
				"java.awt.Dimension", "java.awt.DisplayMode", "java.awt.EventQueue",
				"java.awt.FileDialog", "java.awt.FlowLayout", "java.awt.FocusTraversalPolicy",
				"java.awt.Font", "java.awt.FontFormatException", "java.awt.FontMetrics",
				"java.awt.Frame", "java.awt.GradientPaint", "java.awt.Graphics",
				"java.awt.Graphics2D", "java.awt.GraphicsConfiguration", "java.awt.GraphicsConfigTemplate",
				"java.awt.GraphicsDevice", "java.awt.GraphicsEnvironment", "java.awt.GridBagConstraints",
				"java.awt.GridBagLayout", "java.awt.GridBagLayoutInfo", "java.awt.GridLayout",
				"java.awt.HeadlessException", "java.awt.IllegalComponentStateException", "java.awt.Image",
				"java.awt.ImageCapabilities", "java.awt.Insets", "java.awt.ItemSelectable",
				"java.awt.JobAttributes", "java.awt.KeyEventDispatcher", "java.awt.KeyEventPostProcessor",
				"java.awt.KeyboardFocusManager", "java.awt.Label", "java.awt.LayoutManager",
				"java.awt.LayoutManager2", "java.awt.LinearGradientPaint", "java.awt.List",
				"java.awt.MediaTracker", "java.awt.Menu", "java.awt.MenuBar",
				"java.awt.MenuComponent", "java.awt.MenuContainer", "java.awt.MenuItem",
				"java.awt.MenuShortcut", "java.awt.ModalEventFilter", "java.awt.ModalExclusionType",
				"java.awt.MouseInfo", "java.awt.MultipleGradientPaint", "java.awt.PageAttributes",
				"java.awt.Paint", "java.awt.PaintContext", "java.awt.Panel",
				"java.awt.Point", "java.awt.PointerInfo", "java.awt.Polygon",
				"java.awt.PopupMenu", "java.awt.PrintGraphics", "java.awt.PrintJob",
				"java.awt.RadialGradientPaint", "java.awt.Rectangle", "java.awt.RenderingHints",
				"java.awt.Robot", "java.awt.ScrollPane", "java.awt.ScrollPaneAdjustable",
				"java.awt.Scrollbar", "java.awt.SecondaryLoop", "java.awt.SequencedEvent",
				"java.awt.Shape", "java.awt.SplashScreen", "java.awt.Stroke",
				"java.awt.SystemColor", "java.awt.SystemTray", "java.awt.TextArea",
				"java.awt.TextComponent", "java.awt.TextField", "java.awt.TexturePaint",
				"java.awt.Toolkit", "java.awt.Transparency", "java.awt.TrayIcon",
				"java.awt.Window"
			));
		} else if (pkgName.equals("java.awt.event")) {
			classes.addAll(Arrays.asList(
				"java.awt.event.ActionEvent", "java.awt.event.ActionListener", "java.awt.event.AdjustmentEvent",
				"java.awt.event.AdjustmentListener", "java.awt.event.ComponentAdapter", "java.awt.event.ComponentEvent",
				"java.awt.event.ComponentListener", "java.awt.event.ContainerAdapter", "java.awt.event.ContainerEvent",
				"java.awt.event.ContainerListener", "java.awt.event.FocusAdapter", "java.awt.event.FocusEvent",
				"java.awt.event.FocusListener", "java.awt.event.HierarchyBoundsAdapter", "java.awt.event.HierarchyBoundsListener",
				"java.awt.event.HierarchyEvent", "java.awt.event.HierarchyListener", "java.awt.event.InputEvent",
				"java.awt.event.InputMethodEvent", "java.awt.event.InputMethodListener", "java.awt.event.InvocationEvent",
				"java.awt.event.ItemEvent", "java.awt.event.ItemListener", "java.awt.event.KeyAdapter",
				"java.awt.event.KeyEvent", "java.awt.event.KeyListener", "java.awt.event.MouseAdapter",
				"java.awt.event.MouseEvent", "java.awt.event.MouseListener", "java.awt.event.MouseMotionAdapter",
				"java.awt.event.MouseMotionListener", "java.awt.event.MouseWheelEvent", "java.awt.event.MouseWheelListener",
				"java.awt.event.PaintEvent", "java.awt.event.TextEvent", "java.awt.event.TextListener",
				"java.awt.event.WindowAdapter", "java.awt.event.WindowEvent", "java.awt.event.WindowFocusListener",
				"java.awt.event.WindowListener", "java.awt.event.WindowStateListener"
			));
		} else if (pkgName.equals("java.awt.geom")) {
			classes.addAll(Arrays.asList(
				"java.awt.geom.AffineTransform", "java.awt.geom.Arc2D", "java.awt.geom.Area",
				"java.awt.geom.CubicCurve2D", "java.awt.geom.Dimension2D", "java.awt.geom.Ellipse2D",
				"java.awt.geom.FlatteningPathIterator", "java.awt.geom.GeneralPath", "java.awt.geom.IllegalPathStateException",
				"java.awt.geom.Line2D", "java.awt.geom.NoninvertibleTransformException", "java.awt.geom.Path2D",
				"java.awt.geom.PathIterator", "java.awt.geom.Point2D", "java.awt.geom.QuadCurve2D",
				"java.awt.geom.Rectangle2D", "java.awt.geom.RectangularShape", "java.awt.geom.RoundRectangle2D"
			));
		} else if (pkgName.equals("java.awt.image")) {
			classes.addAll(Arrays.asList(
				"java.awt.image.AffineTransformOp", "java.awt.image.AreaAveragingScaleFilter", "java.awt.image.BandCombineOp",
				"java.awt.image.BandedSampleModel", "java.awt.image.BufferStrategy", "java.awt.image.BufferedImage",
				"java.awt.image.BufferedImageFilter", "java.awt.image.BufferedImageOp", "java.awt.image.ByteLookupTable",
				"java.awt.image.ColorConvertOp", "java.awt.image.ColorModel", "java.awt.image.ComponentColorModel",
				"java.awt.image.ComponentSampleModel", "java.awt.image.ConvolveOp", "java.awt.image.CropImageFilter",
				"java.awt.image.DataBuffer", "java.awt.image.DataBufferByte", "java.awt.image.DataBufferDouble",
				"java.awt.image.DataBufferFloat", "java.awt.image.DataBufferInt", "java.awt.image.DataBufferShort",
				"java.awt.image.DataBufferUShort", "java.awt.image.DirectColorModel", "java.awt.image.FilteredImageSource",
				"java.awt.image.ImageConsumer", "java.awt.image.ImageFilter", "java.awt.image.ImageObserver",
				"java.awt.image.ImageProducer", "java.awt.image.ImagingOpException", "java.awt.image.IndexColorModel",
				"java.awt.image.Kernel", "java.awt.image.LookupOp", "java.awt.image.LookupTable",
				"java.awt.image.MemoryImageSource", "java.awt.image.MultiPixelPackedSampleModel", "java.awt.image.PackedColorModel",
				"java.awt.image.PixelGrabber", "java.awt.image.PixelInterleavedSampleModel", "java.awt.image.Raster",
				"java.awt.image.RasterFormatException", "java.awt.image.RasterOp", "java.awt.image.RenderedImage",
				"java.awt.image.ReplicateScaleFilter", "java.awt.image.RescaleOp", "java.awt.image.RGBImageFilter",
				"java.awt.image.SampleModel", "java.awt.image.ShortLookupTable", "java.awt.image.SinglePixelPackedSampleModel",
				"java.awt.image.TileObserver", "java.awt.image.VolatileImage", "java.awt.image.WritableRaster",
				"java.awt.image.WritableRenderedImage"
			));
		}
		
		return classes;
	}

	private static void generateReport(String format, String outputPath) {
		try {
			Path path;
			if (outputPath == null) {
				String ext = format.equals("html") ? "html" : "md";
				path = Paths.get("docs/coverage/report." + ext);
			} else {
				path = Paths.get(outputPath);
			}
			
			if (format.equalsIgnoreCase("html")) {
				new HtmlReportGenerator().generate(coverageData, path);
				System.out.println("HTML report generated: " + path.toAbsolutePath());
			} else if (format.equalsIgnoreCase("markdown")) {
				new MarkdownReportGenerator().generate(coverageData, path);
				System.out.println("Markdown report generated: " + path.toAbsolutePath());
			} else {
				System.err.println("Unknown format: " + format);
				System.err.println("Supported formats: html, markdown");
			}
		} catch (IOException e) {
			System.err.println("Failed to generate report: " + e.getMessage());
			e.printStackTrace();
		}
	}


	private static String[] findClassesInPackage(String pkg, ClassLoader loader) {
		String path = pkg.replace('.', '/');
		Set<String> classes = new HashSet<>();
		try {
			Enumeration<URL> resources = loader.getResources(path);
			while (resources.hasMoreElements()) {
				URL res = resources.nextElement();
				classes.addAll(scanResourceForClasses(res, path));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		String[] array = new String[classes.size()];
		classes.toArray(array);
		Arrays.sort(array);
		return array;
	}

	private static Set<String> scanResourceForClasses(URL url, String path) throws IOException {
		Set<String> result = new HashSet<>();
		URLConnection conn = url.openConnection();

		if (conn instanceof JarURLConnection) {
			JarURLConnection jarConn = (JarURLConnection) conn;
			// Inside a JAR
			JarFile jar = jarConn.getJarFile();
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements()) {
				JarEntry entry = e.nextElement();
				String name = entry.getName();
				if (name.endsWith(".class") && name.startsWith(path)) {
					result.add(name.replace('/', '.').substring(0, name.length() - 6));
				}
			}
		} else {
			// Directory on disk
			File dir = new File(url.getPath());
			if (dir.isDirectory()) {
				Files.walk(dir.toPath())
					.filter(p -> p.toString().endsWith(".class"))
					.forEach(p -> {
						String fullPath = p.toString().replace(File.separatorChar, '/');
						int idx = fullPath.indexOf(path);
						if (idx != -1) {
							String className = fullPath.substring(idx)
								.replace('/', '.')
								.replaceAll("\\.class$", "");
							result.add(className);
						}
					});
			}
		}
		return result;
	}

	private static String mapTeaVmToRuntimeClassName(String teavmName) {
		if (!teavmName.startsWith(AWTEA_PREFIX)) return null;

		String withoutPrefix = teavmName.substring(AWTEA_PREFIX.length());
		int lastDot = withoutPrefix.lastIndexOf('.');
		String pkg = (lastDot == -1) ? "" : withoutPrefix.substring(0, lastDot);
		String simple = (lastDot == -1) ? withoutPrefix : withoutPrefix.substring(lastDot + 1);

		// Handle nested types: TOuter$Inner
		int dollar = simple.indexOf('$');
		String outer = (dollar == -1) ? simple : simple.substring(0, dollar);
		String rest = (dollar == -1) ? "" : simple.substring(dollar);

		if (outer.startsWith(CLASS_PREFIX) &&
			outer.length() > 1 &&
			Character.isUpperCase(outer.charAt(1))) {
			outer = outer.substring(1);
		}

		return pkg.isEmpty()
			? outer + rest
			: pkg + "." + outer + rest;
	}

	private static String normalizeTypeForApi(Class<?> type) {
		if (type.isArray()) {
			return normalizeTypeForApi(type.getComponentType()) + "[]";
		}

		String name = type.getTypeName();

		if (type.isPrimitive()) {
			return type.getName();
		}

		String teavmName = mapTeaVmToRuntimeClassName(name);
		if (teavmName != null) {
			return teavmName;
		}

		return name;
	}

	private static void compareClasses(Class<?> teavmClass, Class<?> runtimeClass) {
		Set<String> runtimeMethods = describeMethods(runtimeClass);
		Set<String> runtimeFields  = describeFields(runtimeClass);
		Set<String> runtimeCtors   = describeCtors(runtimeClass);

		Set<String> teavmMethods = describeMethods(teavmClass);
		Set<String> teavmFields  = describeFields(teavmClass);
		Set<String> teavmCtors   = describeCtors(teavmClass);

		// Runtime API items that TeaVM *did not* implement
		Set<String> missingMethods = diff(runtimeMethods, teavmMethods);
		Set<String> missingFields  = diff(runtimeFields,  teavmFields);
		Set<String> missingCtors   = diff(runtimeCtors,   teavmCtors);
		
		// Implemented items
		Set<String> implementedMethods = diff(runtimeMethods, missingMethods);
		Set<String> implementedFields  = diff(runtimeFields,  missingFields);
		Set<String> implementedCtors   = diff(runtimeCtors,   missingCtors);

		// Totals
		int runtimeTotal = runtimeMethods.size() + runtimeFields.size() + runtimeCtors.size();
		int implementedTotal = runtimeTotal
			- (missingMethods.size() + missingFields.size() + missingCtors.size());

		// Update global totals
		globalRuntimeTotal += runtimeTotal;
		globalImplementedTotal += implementedTotal;

		// If generating coverage data, populate it
		if (coverageData != null) {
			ClassCoverage classCoverage = new ClassCoverage(
				teavmClass.getName(), 
				runtimeClass.getName()
			);
			
			implementedMethods.forEach(classCoverage::addImplementedMethod);
			missingMethods.forEach(classCoverage::addMissingMethod);
			implementedFields.forEach(classCoverage::addImplementedField);
			missingFields.forEach(classCoverage::addMissingField);
			implementedCtors.forEach(classCoverage::addImplementedConstructor);
			missingCtors.forEach(classCoverage::addMissingConstructor);
			
			String packageName = runtimeClass.getPackage() != null ? 
				runtimeClass.getPackage().getName() : "(default)";
			coverageData.addClassCoverage(packageName, classCoverage);
		}

		// Print console output only if not generating a report
		if (coverageData == null) {
			// If class fully covered, you can skip printing
			if (missingMethods.isEmpty() && missingFields.isEmpty() && missingCtors.isEmpty()) {
				System.out.printf("=== %s: FULL COVERAGE (%d/%d = 100%%)%n",
					runtimeClass.getName(), implementedTotal, runtimeTotal);
				return;
			}

			// Print diff
			System.out.printf("=== %s vs %s ===%n", teavmClass.getName(), runtimeClass.getName());
			System.out.printf("Coverage: %d/%d = %.1f%%%n",
				implementedTotal, runtimeTotal,
				(runtimeTotal == 0 ? 100.0 : (100.0 * implementedTotal / runtimeTotal)));

			if (!missingMethods.isEmpty()) {
				System.out.println("  Missing methods:");
				missingMethods.forEach(m -> System.out.println("    " + m));
			}
			if (!missingFields.isEmpty()) {
				System.out.println("  Missing fields:");
				missingFields.forEach(f -> System.out.println("    " + f));
			}
			if (!missingCtors.isEmpty()) {
				System.out.println("  Missing ctors:");
				missingCtors.forEach(c -> System.out.println("    " + c));
			}

			System.out.println();
		}
	}


	private static String modifiersFor(int mods) {
		StringBuilder sb = new StringBuilder();
		if (Modifier.isPublic(mods)){
			sb.append("public ");
		}
		else if (Modifier.isProtected(mods)){
			sb.append("protected ");
		}
		else if (Modifier.isPrivate(mods)){
			sb.append("private ");
		}

		if (Modifier.isStatic(mods)){
			sb.append("static ");
		}
		if (Modifier.isAbstract(mods)){
			sb.append("abstract ");
		}
		if (Modifier.isFinal(mods)){
			sb.append("final ");
		}

		// trim trailing space
		int len = sb.length();
		if (len > 0 && sb.charAt(len - 1) == ' ') {
			sb.setLength(len - 1);
		}
		return sb.toString();
	}

	private static boolean filterModifiers(int mods, int clazzModifiers) {
		return !Modifier.isPublic(mods) && !(
			// we only care about protected members in non-final classes, in case
			// a user-defined subclass wants to override them
			Modifier.isProtected(mods) && !Modifier.isFinal(clazzModifiers)
			);
	}


	private static Set<String> describeMethods(Class<?> c) {
		Method[] methods = Arrays.stream(c.getDeclaredMethods()).filter(
			m -> m.getDeclaringClass() == c
		).toArray(Method[]::new);
		Set<String> out = new HashSet<>();
		for (Method m : methods) {
			if (filterModifiers(m.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(methodKey(c, m));
		}
		return out;
	}

	private static Set<String> describeFields(Class<?> c) {
		Set<String> out = new HashSet<>();
		Field[] fields = Arrays.stream(c.getDeclaredFields()).filter(
			f -> f.getDeclaringClass() == c
		).toArray(Field[]::new);
		for (Field f : fields) {
			if (filterModifiers(f.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(fieldKey(c, f));
		}
		return out;
	}

	private static Set<String> describeCtors(Class<?> c) {
		Set<String> out = new HashSet<>();
		Constructor<?>[] ctors = Arrays.stream(c.getDeclaredConstructors()).filter(
			ctor -> ctor.getDeclaringClass() == c
		).toArray(Constructor<?>[]::new);
		for (Constructor<?> ctor : ctors) {
			if (filterModifiers(ctor.getModifiers(), c.getModifiers())) {
				continue;
			}
			out.add(constructorKey(c, ctor));
		}
		return out;
	}

	private static String methodKey(Class<?> owner, Method m) {
		StringBuilder sb = new StringBuilder(modifiersFor(m.getModifiers()));
		sb.append(" ")
			.append(normalizeTypeForApi(m.getReturnType()))
			.append(" ")
			.append(m.getName()).append("(");
		Class<?>[] params = m.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(normalizeTypeForApi(params[i]));
		}
		sb.append(")");
		return sb.toString();
	}

	private static String constructorKey(Class<?> owner, Constructor<?> c) {
		StringBuilder sb = new StringBuilder(modifiersFor(c.getModifiers()));
		sb.append(" ")
			.append(normalizeTypeForApi(owner))
			.append("(");
		Class<?>[] params = c.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			if (i > 0) sb.append(", ");
			sb.append(normalizeTypeForApi(params[i]));
		}
		sb.append(")");
		return sb.toString();
	}

	private static String fieldKey(Class<?> owner, Field f) {
		return modifiersFor(f.getModifiers()) + " "
			+ normalizeTypeForApi(f.getType()) +
			" " +
			f.getName();
	}

	private static Set<String> diff(Set<String> a, Set<String> b) {
		Set<String> result = new TreeSet<>(a);
		result.removeAll(b);
		return result;
	}
}
