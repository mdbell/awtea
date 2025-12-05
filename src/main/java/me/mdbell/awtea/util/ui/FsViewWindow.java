package me.mdbell.awtea.util.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.file.FileList;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

@ExtensionMethod({JSObjectsExtensions.class})
public class FsViewWindow extends FloatingWindow {

	private static final File ROOT = new File("/");

	private File currentDir = ROOT;

	private boolean isRoot = true;

	private File[] files = new File[0];

	public FsViewWindow() {
		this(1000);
		injectStyles();
	}

	public FsViewWindow(int refreshIntervalMs) {
		super("fsviewer", "AWTea Filesystem Viewer", 500, "500px", refreshIntervalMs);
	}

	@Override
	protected String computeSignature() {

		if (currentDir == null || !currentDir.isDirectory()) {
			return null;
		}

		// update state
		files = currentDir.listFiles();
		isRoot = currentDir.equals(ROOT);

		if (files != null) {
			// sort files by name
			Arrays.sort(files, (a, b) -> {
				if (a.isDirectory() && !b.isDirectory()) {
					return -1;
				}
				if (!a.isDirectory() && b.isDirectory()) {
					return 1;
				}
				return a.getName().compareToIgnoreCase(b.getName());
			});
		}

		StringBuilder sb = new StringBuilder();
		sb.append(currentDir.getAbsolutePath()).append('|');
		sb.append(isRoot ? 'R' : 'N').append('|');

		if (files == null) {
			sb.append("NULL");
			return sb.toString();
		}

		sb.append(files.length).append('|');
		for (File f : files) {
			sb.append(f.getName()).append('\u0000');
			sb.append(f.isDirectory() ? 'D' : 'F').append('\u0000');
			sb.append(f.length()).append('\u0000');
			sb.append(f.lastModified()).append('\u0000');
		}
		return sb.toString();
	}

	@Override
	protected HTMLElement buildBodyContent() {

		HTMLElement body = createElement("div");
		body.setClassName("fs-viewer-body");

		HTMLElement pathBar = createElement("div");
		pathBar.setClassName("fs-pathbar");

		pathBar.appendChild(constructPathSpan());

		HTMLElement listEl = constructListView();

		body.appendChild(pathBar);

		body.appendChild(listEl);

		return body;
	}

	private HTMLElement constructListView() {
		HTMLElement list = createElement("ul");
		list.setClassName("fs-list");

		list.appendChild(createRow(FileType.NAVIGATION, "[.]", () -> navigateTo(currentDir)));

		// Parent directory link, if not at root
		if (!isRoot) {
			File parent = currentDir.getParentFile();
			if (parent != null) {
				list.appendChild(createRow(FileType.NAVIGATION, "[..]", () -> navigateTo(parent)));
			}
			list.appendChild(createRow(FileType.UPLOAD, "[Upload]", () -> uploadTo(currentDir)));
		}

		if (files == null) {
			HTMLElement errorRow = createElement("li");
			errorRow.setClassName("fs-error");
			errorRow.setTextContent("Error: Unable to list directory contents.");
			list.appendChild(errorRow);
		} else if (files.length == 0) {
			HTMLElement emptyRow = createElement("li");
			emptyRow.setClassName("fs-empty");
			emptyRow.setTextContent("Directory is empty.");
			list.appendChild(emptyRow);
		}


		for (File file : files) {
			FileType type = file.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
			String name = file.getName();
			Runnable action = file.isDirectory() ? () -> navigateTo(file) : null;
			HTMLElement row = createRow(type, name, action);

			if (!file.isDirectory()) {
				long size = file.length();
				long modified = file.lastModified();

				HTMLElement metaCol = createElement("span");
				metaCol.setClassName("fs-entry-meta");
				String meta = humanReadableSize(size);
				if (modified > 0) {
					meta += " • " + humanReadableTimestamp(modified);
				}
				metaCol.setTextContent(meta);
				row.appendChild(metaCol);
			}
			list.appendChild(row);
		}

		return list;
	}

	private void uploadTo(File currentDir) {
		HTMLElement fileInput = document.createElement("input");
		fileInput.setAttribute("type", "file");
		fileInput.setAttribute("multiple", "multiple");
		fileInput.getStyle().setProperty("display", "none");
		fileInput.addEventListener("change", evt -> {
			org.teavm.jso.dom.html.HTMLInputElement input = (org.teavm.jso.dom.html.HTMLInputElement) evt.getTarget();
			schedule(() -> {
				FileList fileList = input.getFiles();
				System.out.println("Uploading " + (fileList != null ? fileList.getLength() : 0) + " files to " + currentDir.getAbsolutePath());
				if (fileList != null) {
					for (int i = 0; i < fileList.getLength(); i++) {
						org.teavm.jso.file.File jsFile = fileList.item(i);
						Uint8ClampedArray nativeArray = new Uint8ClampedArray(jsFile.arrayBuffer().await());
						byte[] buffer = nativeArray.getArrayFromJS();
						File destFile = new File(currentDir, jsFile.getName());
						System.out.println(" - " + jsFile.getName() + " (" + buffer.length + " bytes)");
						// Write to local FS
						try (java.io.FileOutputStream fos = new java.io.FileOutputStream(destFile)) {
							fos.write(buffer);
						} catch (Exception e) {
							e.printStackTrace();
						}
						destFile.setLastModified((long) jsFile.getLastModified());
					}
				}
			});
			// Clear the input value to allow re-uploading the same files
			//input.setValue("");
		});
		// Trigger the file dialog
		fileInput.click();
	}

	private HTMLElement createRow(FileType type) {
		HTMLElement row = document.createElement("li");
		row.setClassName("fs-entry");
		HTMLElement typeCol = document.createElement("span");
		typeCol.setClassName("fs-entry-type");

		typeCol.setTextContent(type.getLabel());
		row.appendChild(typeCol);
		return row;
	}

	private HTMLElement createRow(FileType type, String text, Runnable action) {
		HTMLElement row = createRow(type);
		HTMLElement nameCol = document.createElement("span");
		nameCol.setClassName("fs-entry-name");

		HTMLElement link = document.createElement("a");
		link.setClassName(type != FileType.FILE ? "fs-dir" : "");
		link.setTextContent(text);
		if (action != null) {
			link.setAttribute("href", "#");
			link.addEventListener("click", evt -> {
				evt.preventDefault();
				schedule(action);
			});
		} else {
			link.setAttribute("href", "javascript:void(0)");
		}

		nameCol.appendChild(link);

		row.appendChild(nameCol);
		return row;
	}

	private void deleteDirectory(File file) {

	}

	protected HTMLElement constructPathSpan() {
		HTMLElement span = createElement("span");
		span.setClassName("fs-path");

		HTMLElement label = createElement("strong");
		label.setTextContent("Current: ");

		span.appendChild(label);

		HTMLElement rootLink = document.createElement("a");

		HTMLElement rootSpan = document.createElement("span");
		rootSpan.setTextContent("<root>");
		CSSStyleDeclaration style = rootSpan.getStyle();
		style.setProperty("font-style", "italic");
		rootLink.appendChild(rootSpan);

		rootLink.setAttribute("href", "#");
		rootLink.addEventListener("click", evt -> {
			evt.preventDefault();
			schedule(() -> navigateTo(ROOT));
		});
		span.appendChild(rootLink);

		if (isRoot) {
			// at root, nothing else to add
			return span;
		}

		System.out.println(currentDir);
		System.out.println(ROOT);

		// Absolute path of current directory
		String absPath = currentDir.getAbsolutePath();
		String rootPath = ROOT.getAbsolutePath();

		// Get the part of the path relative to ROOT
		String relative;
		if (absPath.startsWith(rootPath)) {
			relative = absPath.substring(rootPath.length());
		} else {
			// Fallback: treat entire path as relative
			relative = absPath;
		}

		// Normalize: remove leading slash
		if (relative.startsWith("/")) {
			relative = relative.substring(1);
		}

		if (relative.isEmpty()) {
			return span; // nothing more to add
		}

		String[] segments = relative.split("/");

		// Rebuild target directories step by step from ROOT
		File target = ROOT;
		for (String seg : segments) {
			if (seg.isEmpty()) continue;

			target = new File(target, seg);   // accumulate

			// separator: " / "
			span.appendChild(document.createTextNode(" / "));

			final File thisDir = target;
			HTMLElement segLink = document.createElement("a");
			segLink.setTextContent(seg);
			segLink.setAttribute("href", "#");
			segLink.addEventListener("click", evt -> {
				evt.preventDefault();
				schedule(() -> navigateTo(thisDir));
			});
			span.appendChild(segLink);
		}
		return span;
	}

	private void navigateTo(File dir) {
		if (dir != null && dir.isDirectory()) {
			currentDir = dir;
			render();
		}
	}

	private void injectStyles() {
		HTMLElement style = document.createElement("style");
		style.setTextContent(
//			Path viewer styles
			".fs-pathbar { " +
				"display: flex;" +
				"align-items: center;" +
				"justify-content: space-between;" +
				"margin-bottom: 0.5rem;" +
				"gap: 0.5rem;" +
				"}" +
				".fs-path { " +
				"flex: 1;" +
				"white-space: nowrap;" +
				"overflow: hidden;" +
				"text-overflow: ellipsis;" +
				"}" +
				".fs-path a { " +
				"color: inherit;" +
				"}" +
				".fs-path strong { " +
				"font-weight: 600;" +
				"}" +
				".fs-list { " +
				"list-style: none;" +
				"padding-left: 0;" +
				"margin: 0;" +
				"border-top: 1px solid var(--aw-header-border)" +
				"}" +
				".fs-entry { " +
				"display: flex;" +
				"align-items: baseline;" +
				"padding: 0.25rem 0;" +
				"border-bottom: 1px solid var(--aw-entry-border);" +
				"}" +
				".fs-entry a { " +
				"text-decoration: none;" +
				"cursor: pointer;" +
				"color: inherit;" +
				"}" +
				".fs-entry a.fs-dir { " +
				"font-weight: 600;" +
				"}" +
				".fs-entry-type { " +
				"width: 50px;" +
				"flex-shrink: 0;" +
				"color: var(--aw-type-fg);" +
				"font-family: monospace;" +
				"}" +
				".fs-entry-name { " +
				"flex: 1;" +
				"min-width: 0;" +
				"}" +
				".fs-entry-meta { " +
				"flex-shrink: 0;" +
				"margin-left: 0.5rem;" +
				"color: var(--aw-meta-fg);" +
				"font-size: 11px;" +
				"white-space: nowrap;" +
				"}" +
				".fs-empty { " +
				"padding: 0.4rem 0;" +
				"color: var(--aw-meta-fg);" +
				"}" +
				".fs-error { " +
				"color: var(--aw-error-fq);" +
				"padding: 0.4rem 0;" +
				"}"
		);
		document.getHead().appendChild(style);
	}

	private String humanReadableSize(long bytes) {
		if (bytes < 0) {
			return "?";
		}
		if (bytes < 1024) {
			return bytes + " B";
		}

		int unit = 1024;
		String[] units = {"B", "KB", "MB", "GB", "TB"};
		double value = bytes;
		int idx = 0;
		while (value >= unit && idx < units.length - 1) {
			value /= unit;
			idx++;
		}
		return String.format("%.1f %s", value, units[idx]);
	}

	private String humanReadableTimestamp(long epochMillis) {
		long now = System.currentTimeMillis();
		long diff = now - epochMillis;

		long seconds = diff / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (seconds < 60) {
			return "just now";
		}
		if (minutes < 60) {
			return plural(minutes, "min");
		}
		if (hours < 24) {
			return plural(hours, "hour");
		}
		if (days < 7) {
			return plural(days, "day");
		}

		// Fallback: calendar style
		Date d = new Date(epochMillis);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		return cal.get(Calendar.YEAR) + "-" +
			pad(cal.get(Calendar.MONTH) + 1) + "-" +
			pad(cal.get(Calendar.DAY_OF_MONTH)) + " " +
			pad(cal.get(Calendar.HOUR_OF_DAY)) + ":" +
			pad(cal.get(Calendar.MINUTE));
	}

	private static String plural(long n, String unit) {
		return n + " " + unit + (n == 1 ? "" : "s") + " ago";
	}

	private String pad(int n) {
		return (n < 10 ? "0" : "") + n;
	}

	@RequiredArgsConstructor
	@Getter
	private enum FileType {
		NAVIGATION("[NAV]"),
		UPLOAD("[UPL]"),
		FILE("FILE"),
		DIRECTORY("[DIR]");

		private final String label;
	}
}
