package me.mdbell.awtea.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.io.File;
import java.util.*;

@ExtensionMethod({JSObjectsExtensions.class})
public class FsViewer {

	private final HTMLDocument document;
	private final HTMLElement container;

	private HTMLElement bodyEl;
	private HTMLElement listEl;
	private HTMLElement pathSpanEl;
	private HTMLElement darkBtnEl;
	private HTMLButtonElement closeBtn;

	private static final File ROOT = new File("/");

	private File currentDir = ROOT;

	// Position persistence keys
	private static final String POS_LEFT_KEY = "fsviewer.left";
	private static final String POS_TOP_KEY = "fsviewer.top";
	private static final String MODE_DARK_KEY = "fsviewer.darkmode";

	// Theme + refresh
	private boolean darkMode = isDarkMode();
	private static final int REFRESH_INTERVAL_MS = 5000;
	private static final int INVOKE_INTERVAL_MS = 50;

	// Dragging
	private boolean dragging = false;
	private int dragStartMouseX;
	private int dragStartMouseY;
	private int dragStartLeft;
	private int dragStartTop;

	private final List<Runnable> invokedLater = new ArrayList<>();

	private String lastDirSignature = null;

	public void invokeLater(Runnable r) {
		invokedLater.add(r);
	}

	public FsViewer() {
		document = Window.current().getDocument();

		// Main container: overlay on existing page
		HTMLElement body = document.getBody();
		container = document.createElement("div");
		container.setClassName("fs-viewer");
		body.appendChild(container);

		restorePersistentData();

		injectStyles();

		currentDir = ROOT;

		render();

		new Thread(() -> {
			while (true) {
				for (Runnable r : invokedLater) {
					r.run();
				}
				invokedLater.clear();

				try {
					Thread.sleep(INVOKE_INTERVAL_MS);
				} catch (InterruptedException ex) {
					// ignore
				}
			}
		}).start();

		Window.setInterval(() -> {
			if (!dragging && container.getParentNode() != null) {
				invokeLater(this::refreshDirectoryView);
			}
		}, REFRESH_INTERVAL_MS);
	}

	public static void main(String[] args) {
		new FsViewer();
	}

	private void injectStyles() {
		HTMLElement style = document.createElement("style");
		style.setTextContent(
			".fs-viewer { " +
				"position: fixed;" +
				"top: 10px;" +
				"right: 10px;" +
				"width: 500px;" +
				"height: 50vh;" +
				"overflow: auto;" +
				"font-family: sans-serif;" +
				"font-size: 13px;" +
				"border: 1px solid #ccc;" +
				"border-radius: 4px;" +
				"background: rgba(255,255,255,0.97);" +
				"color: #000;" +
				"box-shadow: 0 2px 8px rgba(0,0,0,0.25);" +
				"z-index: 9999;" +
				"}\n" +

				".fs-viewer-header { " +
				"display: flex;" +
				"align-items: center;" +
				"justify-content: space-between;" +
				"padding: 0.25rem 0.5rem;" +
				"background: #f0f0f0;" +
				"border-bottom: 1px solid #ddd;" +
				"border-radius: 4px 4px 0 0;" +
				"cursor: move;" +
				"}\n" +

				".fs-viewer-title { " +
				"margin: 0;" +
				"font-size: 13px;" +
				"font-weight: 600;" +
				"}\n" +

				".fs-viewer-header-controls { " +
				"display: flex;" +
				"gap: 4px;" +
				"}\n" +

				".fs-viewer-body { " +
				"padding: 0.5rem 0.6rem 0.6rem 0.6rem;" +
				"}\n" +

				".fs-pathbar { " +
				"display: flex;" +
				"align-items: center;" +
				"justify-content: space-between;" +
				"margin-bottom: 0.5rem;" +
				"gap: 0.5rem;" +
				"}\n" +
				".fs-path { " +
				"flex: 1;" +
				"white-space: nowrap;" +
				"overflow: hidden;" +
				"text-overflow: ellipsis;" +
				"}\n" +
				".fs-path strong { " +
				"font-weight: 600;" +
				"}\n" +
				".fs-button { " +
				"padding: 0.1rem 0.4rem;" +
				"border-radius: 3px;" +
				"border: 1px solid #888;" +
				"background: #f5f5f5;" +
				"cursor: pointer;" +
				"font-size: 12px;" +
				"}\n" +
				".fs-button:disabled { " +
				"opacity: 0.4;" +
				"cursor: default;" +
				"}\n" +
				".fs-list { " +
				"list-style: none;" +
				"padding-left: 0;" +
				"margin: 0;" +
				"border-top: 1px solid #ddd;" +
				"}\n" +
				".fs-entry { " +
				"display: flex;" +
				"align-items: baseline;" +
				"padding: 0.25rem 0;" +
				"border-bottom: 1px solid #f0f0f0;" +
				"}\n" +
				".fs-entry a { " +
				"text-decoration: none;" +
				"cursor: pointer;" +
				"color: inherit;" +
				"}\n" +
				".fs-entry a.fs-dir { " +
				"font-weight: 600;" +
				"}\n" +
				".fs-entry-type { " +
				"width: 50px;" +
				"flex-shrink: 0;" +
				"color: #666;" +
				"font-family: monospace;" +
				"}\n" +
				".fs-entry-name { " +
				"flex: 1;" +
				"min-width: 0;" +
				"}\n" +
				".fs-entry-meta { " +
				"flex-shrink: 0;" +
				"margin-left: 0.5rem;" +
				"color: #777;" +
				"font-size: 11px;" +
				"white-space: nowrap;" +
				"}\n" +
				".fs-empty { " +
				"padding: 0.4rem 0;" +
				"color: #777;" +
				"}\n" +
				".fs-error { " +
				"color: #b00020;" +
				"padding: 0.4rem 0;" +
				"}\n" +

				// Dark theme
				".fs-viewer.fs-dark { " +
				"background: #1e1e1e;" +
				"color: #eee;" +
				"border-color: #444;" +
				"box-shadow: 0 2px 12px rgba(0,0,0,0.6);" +
				"}\n" +
				".fs-viewer.fs-dark .fs-viewer-header { " +
				"background: #2b2b2b;" +
				"border-bottom-color: #444;" +
				"}\n" +
				".fs-viewer.fs-dark .fs-button { " +
				"background: #333;" +
				"border-color: #777;" +
				"color: #eee;" +
				"}\n" +
				".fs-viewer.fs-dark .fs-list { " +
				"border-top-color: #444;" +
				"}\n" +
				".fs-viewer.fs-dark .fs-entry { " +
				"border-bottom-color: #333;" +
				"}\n" +
				".fs-viewer.fs-dark .fs-entry-type { " +
				"color: #aaa;" +
				"}\n" +
				".fs-viewer.fs-dark .fs-entry-meta { " +
				"color: #aaa;" +
				"}\n"
		);
		document.getHead().appendChild(style);
	}

	private void render() {
		container.setClassName(darkMode ? "fs-viewer fs-dark" : "fs-viewer");

		if (bodyEl == null) {
			container.setInnerHTML("");

			HTMLElement header = document.createElement("div");
			header.setClassName("fs-viewer-header");

			header.addEventListener("mousedown", new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					MouseEvent e = (MouseEvent) evt;
					startDrag(e);
					evt.preventDefault();
				}
			});

			HTMLElement title = document.createElement("div");
			title.setClassName("fs-viewer-title");
			title.setTextContent("AWTea FS Viewer");
			header.appendChild(title);

			HTMLElement headerControls = document.createElement("div");
			headerControls.setClassName("fs-viewer-header-controls");

			// Dark mode toggle (keep reference so we can update its icon)
			darkBtnEl = document.createElement("button");
			darkBtnEl.setClassName("fs-button");
			darkBtnEl.setTextContent(darkMode ? "☀" : "☾");
			darkBtnEl.addEventListener("click", new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					darkMode = !darkMode;
					savePersistentData();
					invokeLater(FsViewer.this::render); // re-apply theme + update icon
					evt.preventDefault();
				}
			});
			headerControls.appendChild(darkBtnEl);

			// Close button
			closeBtn = (HTMLButtonElement) document.createElement("button");
			closeBtn.setClassName("fs-button");
			closeBtn.setTextContent("×");
			closeBtn.addEventListener("click", new EventListener<Event>() {
				@Override
				public void handleEvent(Event evt) {
					if (container.getParentNode() != null) {
						container.getParentNode().removeChild(container);
					}
					evt.preventDefault();
				}
			});
			headerControls.appendChild(closeBtn);

			header.appendChild(headerControls);
			container.appendChild(header);

			// Body wrapper
			bodyEl = document.createElement("div");
			bodyEl.setClassName("fs-viewer-body");
			container.appendChild(bodyEl);

			// Path bar
			HTMLElement pathBar = document.createElement("div");
			pathBar.setClassName("fs-pathbar");
			bodyEl.appendChild(pathBar);

			pathSpanEl = document.createElement("div");
			pathSpanEl.setClassName("fs-path");
			pathBar.appendChild(pathSpanEl);

			// List container
			listEl = document.createElement("ul");
			listEl.setClassName("fs-list");
			bodyEl.appendChild(listEl);

			// Drag listeners once
			attachGlobalDragListeners();
		}

		// Theme toggle icon
		if (darkBtnEl != null) {
			darkBtnEl.setTextContent(darkMode ? "☀" : "☾");
		}

		refreshDirectoryView();
	}


	private void refreshDirectoryView() {
		if (bodyEl == null || listEl == null || pathSpanEl == null) {
			return; // not initialized yet
		}

		boolean isRoot = currentDir.getAbsolutePath().equals(ROOT.getAbsolutePath());
		File[] files = currentDir.listFiles();

		// ----- 1) detect no-op refresh -----
		String signature = computeDirSignature(currentDir, isRoot, files);
		if (signature.equals(lastDirSignature)) {
			// Directory contents didn't change: skip DOM updates entirely
			return;
		}
		lastDirSignature = signature;

		// ----- 2) update path text -----

		updatePathBreadcrumb(isRoot);

		// ----- 3) build a fresh list off-DOM -----
		HTMLElement newList = document.createElement("ul");
		newList.setClassName("fs-list");

		// Navigation entries
		newList.appendChild(createRow(FileType.NAVIGATION, "[.]", () -> navigateTo(currentDir)));

		if (!isRoot) {
			newList.appendChild(createRow(FileType.NAVIGATION, "[..]", this::goUp));
		}

		if (files == null) {
			HTMLElement error = document.createElement("div");
			error.setClassName("fs-error");
			error.setTextContent("Unable to read directory contents.");
			newList.appendChild(error);
		} else if (files.length == 0) {
			HTMLElement empty = document.createElement("div");
			empty.setClassName("fs-empty");
			empty.setTextContent("(empty directory)");
			newList.appendChild(empty);
		} else {
			Arrays.sort(files, (a, b) -> {
				if (a.isDirectory() && !b.isDirectory()) return -1;
				if (!a.isDirectory() && b.isDirectory()) return 1;
				return a.getName().compareToIgnoreCase(b.getName());
			});

			for (final File file : files) {
				FileType type = file.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
				String text = file.getName().isEmpty() ? "(unnamed)" : escapeHtml(file.getName());

				HTMLElement row = createRow(
					type,
					text,
					file.isDirectory() ? () -> navigateTo(file) : null
				);

				if (!file.isDirectory()) {
					HTMLElement metaCol = document.createElement("span");
					metaCol.setClassName("fs-entry-meta");

					long size = 0;
					long modified = 0;
					try {
						size = file.length();
						modified = file.lastModified();
					} catch (SecurityException ex) {
						// ignore
					}

					String meta = humanReadableSize(size);
					if (modified > 0) {
						meta += " • " + humanReadableTimestamp(modified);
					}
					metaCol.setTextContent(meta);
					row.appendChild(metaCol);
				}

				newList.appendChild(row);
			}
		}

		// ----- 4) swap lists in a single DOM operation -----
		bodyEl.replaceChild(newList, listEl);
		listEl = newList;
	}


	@RequiredArgsConstructor
	@Getter
	enum FileType {
		NAVIGATION("[NAV]"),
		FILE("FILE"),
		DIRECTORY("[DIR]");

		private final String label;
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
		link.setAttribute("href", "#");
		if (action != null) {
			link.addEventListener("click", evt -> {
				evt.preventDefault();
				invokeLater(action);
			});
		}

		nameCol.appendChild(link);

		row.appendChild(nameCol);
		return row;
	}

	private void goUp() {
		File parent = currentDir.getParentFile();
		if (parent == null) {
			parent = ROOT;
		}
		currentDir = parent;
		refreshDirectoryView();
	}

	private void navigateTo(File dir) {
		if (dir != null && dir.isDirectory()) {
			currentDir = dir;
			refreshDirectoryView();
		}
	}

	// --- Dragging logic ---

	private void startDrag(MouseEvent e) {
		dragging = true;
		dragStartMouseX = e.getClientX();
		dragStartMouseY = e.getClientY();
		dragStartLeft = container.getOffsetLeft();
		dragStartTop = container.getOffsetTop();

		// Ensure we use left/top instead of right so dragging behaves nicely
		CSSStyleDeclaration style = container.getStyle();
		style.setProperty("left", dragStartLeft + "px");
		style.setProperty("top", dragStartTop + "px");
		style.setProperty("right", "auto");
		style.setProperty("bottom", "auto");
	}

	private void attachGlobalDragListeners() {
		// Avoid re-attaching multiple times by checking a flag on document
		if (document.getElementById("fs-viewer-drag-listeners") != null) {
			return;
		}
		HTMLElement marker = document.createElement("div");
		marker.setId("fs-viewer-drag-listeners");
		marker.getStyle().setProperty("display", "none");
		document.getBody().appendChild(marker);

		// Mouse move on whole document
		document.addEventListener("mousemove", new EventListener<Event>() {
			@Override
			public void handleEvent(Event evt) {
				if (!dragging) return;
				MouseEvent e = (MouseEvent) evt;
				int dx = e.getClientX() - dragStartMouseX;
				int dy = e.getClientY() - dragStartMouseY;

				int newLeft = dragStartLeft + dx;
				int newTop = dragStartTop + dy;

				CSSStyleDeclaration style = container.getStyle();
				style.setProperty("left", newLeft + "px");
				style.setProperty("top", newTop + "px");
				evt.preventDefault();
			}
		});

		// Stop dragging on mouse up
		document.addEventListener("mouseup", new EventListener<Event>() {
			@Override
			public void handleEvent(Event evt) {
				dragging = false;
				invokeLater(FsViewer.this::savePersistentData);
			}
		});
	}

	private static boolean isDarkMode() {
		// Simple heuristic: check if the OS/browser prefers dark mode
		String mediaQuery = "(prefers-color-scheme: dark)";
		return Window.current().matchMedia(mediaQuery).getMatches();
	}

	private void restorePersistentData() {
		Storage storage = Window.current().getLocalStorage();
		if (storage == null) {
			return;
		}

		String modeStr = storage.getItem(MODE_DARK_KEY);
		if (modeStr != null) {
			try {
				darkMode = Boolean.parseBoolean(modeStr);
			} catch (Exception ignored) {
				// ignore, odds are we're on first run
			}
		}

		String leftStr = storage.getItem(POS_LEFT_KEY);
		String topStr = storage.getItem(POS_TOP_KEY);
		if (leftStr == null || topStr == null) {
			return; // first run, fall back to CSS top/right
		}

		try {
			int left = Integer.parseInt(leftStr);
			int top = Integer.parseInt(topStr);

			CSSStyleDeclaration style = container.getStyle();
			style.setProperty("left", left + "px");
			style.setProperty("top", top + "px");
			style.setProperty("right", "auto");
			style.setProperty("bottom", "auto");
		} catch (NumberFormatException ignored) {
			// If storage is corrupted, just ignore and use defaults
		}
	}

	private void savePersistentData() {
		Storage storage = Window.current().getLocalStorage();
		if (storage == null) {
			return;
		}

		int left = container.getOffsetLeft();
		int top = container.getOffsetTop();

		storage.setItem(POS_LEFT_KEY, Integer.toString(left));
		storage.setItem(POS_TOP_KEY, Integer.toString(top));

		storage.setItem(MODE_DARK_KEY, Boolean.toString(darkMode));
	}

	// --- Helpers ---

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


	private String humanReadableSize(long bytes) {
		if (bytes < 0) return "?";
		if (bytes < 1024) return bytes + " B";
		int unit = 1024;
		String[] units = {"KB", "MB", "GB", "TB"};
		double value = bytes;
		int idx = 0;
		while (value >= unit && idx < units.length - 1) {
			value /= unit;
			idx++;
		}
		return String.format("%.1f %s", value, units[idx]);
	}

	private String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private String computeDirSignature(File dir, boolean isRoot, File[] files) {
		StringBuilder sb = new StringBuilder();
		sb.append(dir.getAbsolutePath()).append('|');
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

	private void updatePathBreadcrumb(boolean isRoot) {
		// Clear existing content
		pathSpanEl.setInnerHTML("");

		// "Current: " label
		HTMLElement label = document.createElement("strong");
		label.setTextContent("Current: ");
		pathSpanEl.appendChild(label);

		// We'll build anchors like: / home / example / 123
		// Root link: always present
		HTMLElement rootLink = document.createElement("a");

		HTMLElement rootSpan = document.createElement("span");
		rootSpan.setTextContent("<root>");
		CSSStyleDeclaration style = rootSpan.getStyle();
		style.setProperty("font-style", "italic");
		rootLink.appendChild(rootSpan);

		rootLink.setAttribute("href", "#");
		rootLink.addEventListener("click", evt -> {
			evt.preventDefault();
			invokeLater(() -> navigateTo(ROOT));
		});
		pathSpanEl.appendChild(rootLink);

		// If we are at root, nothing more to add
		if (isRoot) {
			return;
		}

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
			return;
		}

		String[] segments = relative.split("/");

		// Rebuild target directories step by step from ROOT
		File target = ROOT;
		for (String seg : segments) {
			if (seg.isEmpty()) continue;

			target = new File(target, seg);   // accumulate

			// separator: " / "
			pathSpanEl.appendChild(document.createTextNode(" / "));

			final File thisDir = target;
			HTMLElement segLink = document.createElement("a");
			segLink.setTextContent(seg);
			segLink.setAttribute("href", "#");
			segLink.addEventListener("click", evt -> {
				evt.preventDefault();
				invokeLater(() -> navigateTo(thisDir));
			});

			pathSpanEl.appendChild(segLink);
		}
	}

}
