package me.mdbell.awtea.util.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import static me.mdbell.awtea.util.ui.UiDispatcher.invokeLater;

@ExtensionMethod({JSObjectsExtensions.class})
public abstract class FloatingWindow {

	protected final HTMLDocument document;
	protected final HTMLElement container;

	// shell elements
	protected HTMLElement bodyEl;
	protected HTMLElement headerControlsEl;
	protected HTMLElement titleEl;

	@Getter
	private String title;

	// window identity / persistence
	@Getter(AccessLevel.PROTECTED)
	private final String windowId;

	private final String storagePosLeftKey;
	private final String storagePosRightKey;

	// drag
	private boolean dragging = false;
	private int dragStartMouseX;
	private int dragStartMouseY;
	private int dragStartLeft;
	private int dragStartTop;

	// Resizing
	private boolean resizing = false;
	private int resizeStartMouseX;
	private int resizeStartMouseY;
	private int resizeStartWidth;
	private int resizeStartHeight;
	@Getter
	@Setter
	private int minWidth = 300;
	@Getter
	@Setter
	private int minHeight = 150;

	// Maximizing
	private boolean maximized = false;
	private int prevLeft, prevTop, prevWidth, prevHeight;

	private boolean pointerListenersAttached = false;

	// config
	private final int refreshIntervalMs;

	private static boolean stylesInjected = false;

	private HTMLElement bodyContent;   // current body root
	private String lastSignature;

	private int refreshIntervalId = -1;

	private static int zCounter = 10000;
	private HTMLElement taskbarButton;
	private boolean minimized = false;

	@Getter
	@Setter
	private boolean autoscroll = false;
	private boolean stickToBottom = true;


	static {
		Theme.get(); // ensure theme is initialized
	}

	protected FloatingWindow(String windowId,
							 String titleText,
							 int widthPx,
							 String heightCss,
							 int refreshIntervalMs) {
		this.windowId = windowId;
		this.refreshIntervalMs = refreshIntervalMs;
		this.title = titleText;

		storagePosLeftKey = windowId + ".left";
		storagePosRightKey = windowId + ".top";

		document = Window.current().getDocument();

		container = document.createElement("div");
		container.setClassName("aw-window");
		CSSStyleDeclaration style = container.getStyle();
		style.setProperty("width", widthPx + "px");
		style.setProperty("height", heightCss);

		container.addEventListener("onclick", evt -> bringToFront());

		injectStylesOnce();
		restorePersistentData();

		buildShell();

		Taskbar.get().registerWindow(this);
	}

	public boolean isVisible() {
		return container.getParentNode() != null;
	}

	public void setVisible(boolean visible) {
		if (!visible) {
			// remove from DOM
			if (container.getParentNode() != null) {
				container.getParentNode().removeChild(container);
			}
			// stop refresh timer
			if (refreshIntervalId != -1) {
				Window.clearInterval(refreshIntervalId);
				refreshIntervalId = -1;
			}
			return;
		}

		// initial render (no need to schedule, we're not in a JS method)
		render();

		// add to DOM
		if (container.getParentNode() == null) {
			document.getBody().appendChild(container);
		}

		bringToFront();

		// start refresh timer
		if (refreshIntervalMs > 0 && refreshIntervalId == -1) {
			refreshIntervalId = Window.setInterval(() -> {
				if (!dragging && container.getParentNode() != null) {
					schedule(this::refreshContent);
				}
			}, refreshIntervalMs);
		}
	}

	public void bringToFront() {
		zCounter++;
		CSSStyleDeclaration style = container.getStyle();
		style.setProperty("z-index", Integer.toString(zCounter));
	}

	void setTaskbarButton(HTMLElement btn) {
		this.taskbarButton = btn;
	}

	public void toggleMinimized() {
		if (minimized) {
			// restore
			container.getStyle().setProperty("display", "flex");
			minimized = false;
			bringToFront();
		} else {
			// minimize
			container.getStyle().setProperty("display", "none");
			minimized = true;
			if (taskbarButton != null) {
				Taskbar.get().setActive(this, false);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected <T extends HTMLElement> T createElement(String tagName) {
		return (T) document.createElement(tagName);
	}

	/**
	 * Use this instead of starting your own thread
	 */
	protected void schedule(Runnable r) {
		invokeLater(r);
	}

	/**
	 * Called when content should re-render (directory change, timer, etc.)
	 */
	protected void refreshContent() {
		renderIfChanged(); // default: just re-render content
	}

	/**
	 * Force a full render (theme change, etc.)
	 */
	protected final void render() {
		renderIfChanged();
	}

	private void renderIfChanged() {
		String sig = computeSignature();
		if (sig != null && sig.equals(lastSignature)) {
			return; // no-op, no flicker
		}
		lastSignature = sig;

		HTMLElement newContent = buildBodyContent();  // built off-DOM

		if (newContent == null) {
			newContent = document.createElement("div");
			CSSStyleDeclaration style = newContent.getStyle();
			style.setProperty("font-style", "italic");
			// vertical center
			style.setProperty("position", "relative");
			style.setProperty("top", "50%");
			style.setProperty("transform", "translateY(-50%)");

			// horizontal center
			style.setProperty("text-align", "center");

			newContent.setTextContent("(no content)");
		}

		if (bodyContent == null) {
			bodyEl.appendChild(newContent);
		} else {
			bodyEl.replaceChild(newContent, bodyContent);
		}
		bodyContent = newContent;

		if (autoscroll) {
			autoscroll(bodyEl);
		}
	}

	public void close() {
		setVisible(false);
		Taskbar.get().unregisterWindow(this);
	}

	/**
	 * Child: return a string that changes when your UI state changes.
	 */
	protected abstract String computeSignature();

	/**
	 * Child: build a NEW HTMLElement for the body content.
	 */
	protected abstract HTMLElement buildBodyContent();

	// ----- Shell construction -----

	private void buildShell() {
		container.setInnerHTML("");

		// Header
		HTMLElement header = document.createElement("div");
		header.setClassName("aw-window-header");

		header.addEventListener("mousedown", evt -> {
			MouseEvent e = (MouseEvent) evt;
			startDrag(e);
			evt.preventDefault();
		});

		header.addEventListener("dblclick", evt -> {
			evt.preventDefault();
			toggleMaximize();
		});


		titleEl = document.createElement("div");
		titleEl.setClassName("aw-window-title");
		titleEl.setTextContent("AWTea - " + title);
		header.appendChild(titleEl);

		headerControlsEl = document.createElement("div");
		headerControlsEl.setClassName("aw-window-header-controls");

		// Minimize button
		HTMLButtonElement minBtn = (HTMLButtonElement) document.createElement("button");
		minBtn.setClassName("aw-window-button");
		minBtn.setTextContent("-");
		minBtn.addEventListener("click", evt -> {
			evt.preventDefault();
			schedule(this::toggleMinimized);
		});
		headerControlsEl.appendChild(minBtn);

		// Close button
		HTMLButtonElement closeBtn = (HTMLButtonElement) document.createElement("button");
		closeBtn.setClassName("aw-window-button");
		closeBtn.setTextContent("×");
		closeBtn.addEventListener("click", evt -> {
			evt.preventDefault();
			schedule(this::close);
		});
		headerControlsEl.appendChild(closeBtn);

		header.appendChild(headerControlsEl);
		container.appendChild(header);

		// Body
		bodyEl = document.createElement("div");
		bodyEl.setClassName("aw-window-body");

		// resizer
		HTMLElement resizer = document.createElement("div");
		resizer.setClassName("aw-window-resizer");
		container.appendChild(resizer);

		resizer.addEventListener("mousedown", evt -> {
			MouseEvent e = (MouseEvent) evt;
			startResize(e);
			evt.preventDefault();
		});

		container.appendChild(bodyEl);

		bodyEl.addEventListener("scroll", evt -> {
			int scrollBottom = bodyEl.getScrollHeight() - (bodyEl.getScrollTop() + bodyEl.getClientHeight());
			stickToBottom = scrollBottom < 20; // threshold
		});
	}

	// ----- Style -----

	private void injectStylesOnce() {
		if (stylesInjected) return;
		stylesInjected = true;

		HTMLElement style = document.createElement("style");
		style.setTextContent(
//				Main Window CSS
			".aw-window { " +
				"position: fixed;" +
				"top: 10px;" +
				"right: 10px;" +
				"font-family: sans-serif;" +
				"font-size: 13px;" +
				"border: 1px solid var(--aw-border);" +
				"border-radius: 4px;" +
				"background: var(--aw-bg);" +
				"color: var(--aw-fg);" +
				"box-shadow: 0 2px 12px var(--aw-shadow);" +
				"z-index: 9999;" +
//				Main window should be a flex column to allow header + body layout, with body expanding
				"display: flex;" +
				"flex-direction: column;" +
				"overflow: hidden;" +
				"}" +
//				Header CSS
				".aw-window-header { " +
				"display: flex;" +
				"align-items: center;" +
				"justify-content: space-between;" +
				"padding: 0.25rem 0.5rem;" +
				"background:var(--aw-header-bg);" +
				"border-bottom: 1px solid var(--aw-header-border);" +
				"border-radius: 4px 4px 0 0;" +
				"cursor: move;" +
				"}" +
//				Title CSS
				".aw-window-title { " +
				"margin: 0;" +
				"font-size: 13px;" +
				"font-weight: 600;" +
				"}" +
//				Controls CSS
				".aw-window-header-controls { " +
				"display: flex;" +
				"gap: 4px;" +
				"}" +
//				Button CSS
				".aw-window-button { " +
				"min-width: 1.2rem;" +
				"height: 1.2rem;" +
				"padding: 0.1rem 0.4rem;" +
				"border-radius: 3px;" +
				"border: 1px solid var(--aw-button-border);" +
				"background: var(--aw-button-bg);" +
				"color: var(--aw-fg);" +
				"cursor: pointer;" +
				"font-size: 12px;" +
				"}" +
//				Window Body CSS
				".aw-window-body { " +
				"padding: 0.5rem 0.6rem 0.6rem 0.6rem;" +
				"flex: 1;" +
				"min-height: 0;" +  // allow flex to shrink
				"overflow: auto;" +
				"}" +
				".aw-window-resizer { " +
				"position: absolute;" +
				"right: 0;" +
				"bottom: 0;" +
				"width: 12px;" +
				"height: 12px;" +
				"cursor: se-resize;" +
				"background: transparent;" +
				"}" +
				".aw-window-resizer::before { " +
				"content: '';" +
				"position: absolute;" +
				"right: 3px;" +
				"bottom: 3px;" +
				"width: 8px;" +
				"height: 8px;" +
				"border-right: 2px solid rgba(255,255,255,0.7);" +
				"border-bottom: 2px solid rgba(255,255,255,0.7);" +
				"pointer-events: none;" +
				"}"
		);
		document.getHead().appendChild(style);
	}

	// ----- Dragging -----

	private void startDrag(MouseEvent e) {
		dragging = true;
		dragStartMouseX = e.getClientX();
		dragStartMouseY = e.getClientY();
		dragStartLeft = container.getOffsetLeft();
		dragStartTop = container.getOffsetTop();

		CSSStyleDeclaration style = container.getStyle();
		style.setProperty("left", dragStartLeft + "px");
		style.setProperty("top", dragStartTop + "px");
		style.setProperty("right", "auto");
		style.setProperty("bottom", "auto");

		attachPointerListeners();
		bringToFront();
	}

	private void attachPointerListeners() {
		if (pointerListenersAttached) {
			return;
		}
		pointerListenersAttached = true;

		document.addEventListener("mousemove", evt -> {
			MouseEvent e = (MouseEvent) evt;

			if (dragging) {
				int dx = e.getClientX() - dragStartMouseX;
				int dy = e.getClientY() - dragStartMouseY;

				int newLeft = dragStartLeft + dx;
				int newTop  = dragStartTop  + dy;

				CSSStyleDeclaration style = container.getStyle();
				style.setProperty("left", newLeft + "px");
				style.setProperty("top",  newTop  + "px");

				evt.preventDefault();
			} else if (resizing) {
				int dx = e.getClientX() - resizeStartMouseX;
				int dy = e.getClientY() - resizeStartMouseY;

				int newWidth  = Math.max(minWidth,  resizeStartWidth  + dx);
				int newHeight = Math.max(minHeight, resizeStartHeight + dy);

				CSSStyleDeclaration style = container.getStyle();
				style.setProperty("width",  newWidth  + "px");
				style.setProperty("height", newHeight + "px");

				evt.preventDefault();
			}
		});

		document.addEventListener("mouseup", evt -> {
			if (dragging) {
				dragging = false;
				schedule(this::savePersistentData);
			}
			if (resizing) {
				resizing = false;
				// schedule(this::saveSize);
			}
		});
	}


	// ----- Resizing -----

	private void startResize(MouseEvent e) {
		resizing = true;
		resizeStartMouseX = e.getClientX();
		resizeStartMouseY = e.getClientY();
		resizeStartWidth = container.getOffsetWidth();
		resizeStartHeight = container.getOffsetHeight();

		attachPointerListeners();
		bringToFront();
	}

	private void toggleMaximize() {
		CSSStyleDeclaration s = container.getStyle();
		if (!maximized) {
			prevLeft = container.getOffsetLeft();
			prevTop = container.getOffsetTop();
			prevWidth = container.getOffsetWidth();
			prevHeight = container.getOffsetHeight();

			s.setProperty("left", "0px");
			s.setProperty("top", "0px");
			s.setProperty("width", "100vw");
			s.setProperty("height", "100vh");

			maximized = true;
		} else {
			s.setProperty("left", prevLeft + "px");
			s.setProperty("top", prevTop + "px");
			s.setProperty("width", prevWidth + "px");
			s.setProperty("height", prevHeight + "px");

			maximized = false;
		}
	}


	// ----- Persistence -----

	private void restorePersistentData() {
		Storage storage = Window.current().getLocalStorage();
		if (storage == null) return;

		String leftStr = storage.getItem(storagePosLeftKey);
		String topStr = storage.getItem(storagePosRightKey);
		if (leftStr == null || topStr == null) return;

		try {
			int left = Integer.parseInt(leftStr);
			int top = Integer.parseInt(topStr);

			CSSStyleDeclaration style = container.getStyle();
			style.setProperty("left", left + "px");
			style.setProperty("top", top + "px");
			style.setProperty("right", "auto");
			style.setProperty("bottom", "auto");
		} catch (NumberFormatException ignored) {
		}
	}

	private void savePersistentData() {
		if (dragging || resizing) {
			return; // don't save while dragging
		}

		if (!isVisible()) {
			return; // not visible
		}

		Storage storage = Window.current().getLocalStorage();
		if (storage == null) {
			return;
		}

		int left = container.getOffsetLeft();
		int top = container.getOffsetTop();

		storage.setItem(storagePosLeftKey, Integer.toString(left));
		storage.setItem(storagePosRightKey, Integer.toString(top));
	}

	// ----- Scroll helpers -----

	private void autoscroll(HTMLElement body) {
		if (stickToBottom) {
			body.setScrollTop(body.getScrollHeight());
		}
	}
}
