package me.mdbell.awtea.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.ThreadUtils;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ExtensionMethod({JSObjectsExtensions.class})
public abstract class FloatingWindow {

	protected HTMLDocument document;
	protected HTMLElement container;

	// shell elements
	protected HTMLElement bodyEl;
	protected HTMLElement headerControlsEl;
	protected HTMLElement titleEl;

	@Getter
	private String title;

	// window identity / persistence
	@Getter(AccessLevel.PROTECTED)
	private final String windowId;

	private String storagePosLeftKey;
	private String storagePosRightKey;

	// drag
	private boolean dragging = false;
	private int dragStartMouseX;
	private int dragStartMouseY;
	private int dragStartLeft;
	private int dragStartTop;

	// Resizing
	private boolean resizable = true;
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

	@Getter
	@Setter
	private MenuBar menuBar;

	private boolean delegatedClicksInstalled = false;

	private boolean fixedIntervalStarted = false;

	// Simple registry: "actionId" -> Runnable
	private final Map<String, Runnable> clickHandlers = new HashMap<>();

	static {
		AwCss.sheet()
			.createClass("aw-window")
			.prop("position", "fixed")
			.prop("top", "10px")
			.prop("right", "10px")
			.prop("font-family", "sans-serif")
			.prop("font-size", "13px")
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("border-radius", "4px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("box-shadow")
			.value("0 0 0 1px")
			.value(Theme.Var.SHADOW)
			.end()
			.prop("z-index", "9999")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("overflow", "hidden")
			.prop("scrollbar-width", "thin")
			.prop("scrollbar-color")
			.value(Theme.Var.SCROLLBAR_THUMB)
			.value(Theme.Var.SCROLLBAR_TRACK)
			.end()
			.createClass("aw-window::-webkit-scrollbar")
			.prop("width", "8px")
			.end()
			.createClass("aw-window::-webkit-scrollbar-track")
			.prop("background", Theme.Var.SCROLLBAR_TRACK)
			.end()
			.createClass("aw-window::-webkit-scrollbar-thumb")
			.prop("background", Theme.Var.SCROLLBAR_THUMB)
			.prop("border-radius", "4px")
			.end()
			.createClass("aw-window-header")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("justify-content", "space-between")
			.prop("padding", "0.25rem 0.5rem")
			.prop("background", Theme.Var.HEADER_BACKGROUND)
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.HEADER_BORDER)
			.end()
			.prop("border-radius", "4px 4px 0 0")
			.prop("cursor", "move")
			.createClass("aw-window-title")
			.prop("margin", "0")
			.prop("font-size", "13px")
			.prop("font-weight", "600")
			.createClass("aw-window-header-controls")
			.prop("display", "flex")
			.prop("gap", "4px")
			.createClass("aw-window-button")
			.prop("min-width", "1.2rem")
			.prop("height", "1.2rem")
			.prop("padding", "0.1rem 0.4rem")
			.prop("border-radius", "3px")
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BUTTON_BORDER)
			.end()
			.prop("background", Theme.Var.BUTTON_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("cursor", "pointer")
			.prop("font-size", "12px")
			.createClass("aw-window-body")
			.prop("padding", "0.5rem 0.6rem 0.6rem 0.6rem")
			.prop("flex", "1")
			.prop("min-height", "0") // allow flex to shrink
			.prop("overflow", "auto")
			.prop("color", Theme.Var.FOREGROUND)
			.createClass("aw-window-body:has(.aw-menubar)")
			.prop("padding-top", "0")
			.createClass("aw-window-resizer")
			.prop("position", "absolute")
			.prop("right", "0")
			.prop("bottom", "0")
			.prop("width", "12px")
			.prop("height", "12px")
			.prop("cursor", "se-resize")
			.prop("background", "transparent")
			.before()
			.prop("content", "''")
			.prop("position", "absolute")
			.prop("right", "3px")
			.prop("bottom", "3px")
			.prop("width", "8px")
			.prop("height", "8px")
			.prop("border-right")
			.value("2px", "solid")
			.value(Theme.Var.FOREGROUND)
			.end()
			.prop("border-bottom")
			.value("2px", "solid")
			.value(Theme.Var.FOREGROUND)
			.end()
			.prop("pointer-events", "none")
			.end()

			// input styling (for filter boxes, selects, etc)
			.createClass("aw-window  select, .aw-window input[type=\"text\"], .aw-window  input[type=\"number\"]")
			.prop("padding", "2px 4px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("border-radius", "3px")
			.end()

			// table styling
			.createClass("aw-window table")
			.prop("width", "100%")
			.prop("border-collapse", "collapse")
			.prop("font-size", "12px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)

			// table headers
			.createClass("aw-window th")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("padding", "2px 4px")
			.prop("text-align", "left")
			.prop("background", Theme.Var.TABLE_HEADER_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)

			// table data cells

			.createClass("aw-window td")
			.prop("padding", "2px 4px")
			.prop("white-space", "nowrap")
			.prop("color", Theme.Var.FOREGROUND)

			// table rows (alternating colors)
			.createClass("aw-window tr")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("cursor", "default")
			.subClass(":hover")
			.prop("background", Theme.Var.TABLE_ROW_HOVER_BACKGROUND)
			.end()

			.createClass("aw-window tr:nth-child(odd)")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.end()

			.createClass("aw-window tr:nth-child(even)")
			.prop("background", Theme.Var.TABLE_ROW_ALT_BACKGROUND)
			.end()


			.inject();
	}

	protected FloatingWindow(String windowId) {
		this.windowId = windowId;
		this.refreshIntervalMs = 0;
		init();
		buildShell();
		Taskbar.get().registerWindow(this);
	}

	protected FloatingWindow(String windowId,
							 String titleText,
							 int widthPx,
							 int heightPx,
							 int refreshIntervalMs) {
		this.windowId = windowId;
		this.refreshIntervalMs = refreshIntervalMs;
		this.title = titleText;


		init();
		buildShell();

		setTitle(titleText);
		setSize(widthPx, heightPx);

		Taskbar.get().registerWindow(this);
	}

	private void init() {
		storagePosLeftKey = windowId + ".left";
		storagePosRightKey = windowId + ".top";

		document = Window.current().getDocument();

		container = document.createElement("div");
		container.setClassName("aw-window");

		restorePersistentData();
	}

	public void setScrollable(boolean scrollable) {
		if (scrollable) {
			bodyEl.getStyle().removeProperty("overflow");
		} else {
			bodyEl.getStyle().setProperty("overflow", "hidden");
		}
	}

	public void setResizeable(boolean resizable) {
		HTMLElement resizer = container.querySelector(".aw-window-resizer");
		if (resizable) {
			resizer.getStyle().setProperty("display", "block");
		} else {
			resizer.getStyle().setProperty("display", "none");
		}
		this.resizable = resizable;
	}

	public void setSize(int widthPx, int heightPx) {
		CSSStyleDeclaration style = container.getStyle();
		String width = widthPx == 0 ? "auto" : widthPx + "px";
		String height = heightPx == 0 ? "auto" : heightPx + "px";
		style.setProperty("width", width);
		style.setProperty("height", height);
	}

	public int getWidth() {
		return container.getOffsetWidth();
	}

	public int getHeight() {
		return container.getOffsetHeight();
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
		if (refreshIntervalMs > 0 && !fixedIntervalStarted) {
			ThreadUtils.runAtFixedRate(windowId, () -> {
				if (!dragging && container.getParentNode() != null) {
					this.refreshContent();
				}
			}, refreshIntervalMs);
			fixedIntervalStarted = true;
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

	protected void registerClickHandler(String id, Runnable handler) {
		clickHandlers.put(id, handler);
	}

	/**
	 * Use this instead of starting your own thread
	 */
	protected void schedule(Runnable r) {
		EventQueue.invokeLater(r);
	}

	protected void scheduleAndWait(Runnable r) {
		try {
			EventQueue.invokeAndWait(r);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

		if (menuBar != null) {
			HTMLElement element = menuBar.getElement();
			HTMLElement menuParent = element.getParentNode() != null ? (HTMLElement) element.getParentNode() : null;
			// ensure menu bar is first child of body
			if (menuParent != container) {
				if (menuParent != null) {
					menuParent.removeChild(element);
				}
				container.insertBefore(element, bodyEl);
			}
		}

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
			bodyContent = newContent;
			bodyEl.appendChild(newContent);
		} else {
			patchElement(bodyContent, newContent);
		}

		if (autoscroll) {
			autoscroll(bodyEl);
		}
	}

	/**
	 * Patch an existing element to match the structure and content of a fresh one.
	 * Tag name is assumed to be the same for the root; if it's not, we just replace.
	 */
	private void patchElement(HTMLElement live, HTMLElement fresh) {
		String liveTag = live.getTagName();
		String freshTag = fresh.getTagName();
		if (!liveTag.equalsIgnoreCase(freshTag)) {
			replaceNode(live, fresh);
			return;
		}

		if (!Objects.equals(live.getClassName(), fresh.getClassName())) {
			live.setClassName(fresh.getClassName());
		}

		syncAttributes(live, fresh);
		syncStyle(live, fresh);

		boolean liveHasChildren = hasElementChildren(live);
		boolean freshHasChildren = hasElementChildren(fresh);

		// ⚠️ KEY: if old node had only text and new one has element children,
		// just replace it entirely; our fine-grained patching isn't worth it here.
		if (!liveHasChildren && freshHasChildren) {
			replaceNode(live, fresh);
			return;
		}

		if (freshHasChildren || liveHasChildren) {
			patchChildren(live, fresh);
		} else if (!Objects.equals(live.getTextContent(), fresh.getTextContent())) {
			// both are leaf nodes → sync text only
			live.setTextContent(fresh.getTextContent());
		}
	}

	private void replaceNode(HTMLElement live, HTMLElement fresh) {
		HTMLElement parent = (HTMLElement) live.getParentNode();
		if (parent != null) {
			parent.replaceChild(fresh, live);
		}
		if (live == bodyContent) {
			bodyContent = fresh;
		}
	}

	private void syncStyle(HTMLElement live, HTMLElement fresh) {
		String freshStyle = fresh.getAttribute("style");
		String liveStyle = live.getAttribute("style");

		if (freshStyle == null) {
			if (liveStyle != null) {
				live.removeAttribute("style");
			}
		} else if (!freshStyle.equals(liveStyle)) {
			live.setAttribute("style", freshStyle);
		}
	}


	private boolean hasElementChildren(HTMLElement el) {
		return el.getFirstElementChild() != null;
	}

	private void syncAttributes(HTMLElement live, HTMLElement fresh) {

		// data attributes
		var attributes = fresh.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			var attr = attributes.item(i);
			String name = attr.getName();
			String freshData = fresh.getAttribute(name);
			String liveData = live.getAttribute(name);
			if (freshData != null && !freshData.equals(liveData)) {
				live.setAttribute(name, freshData);
			} else if (freshData == null && liveData != null) {
				live.removeAttribute(name);
			}
		}

		// remove any old attributes not in fresh
		var liveAttributes = live.getAttributes();
		for (int i = 0; i < liveAttributes.getLength(); i++) {
			var attr = liveAttributes.item(i);
			String name = attr.getName();
			if (fresh.getAttribute(name) == null) {
				live.removeAttribute(name);
			}
		}
	}

	private void patchChildren(HTMLElement live, HTMLElement fresh) {
		Node liveChild = live.getFirstChild();
		Node freshChild = fresh.getFirstChild();

		while (freshChild != null || liveChild != null) {
			if (freshChild == null) {
				// extra old nodes → remove
				Node nextLive = liveChild.getNextSibling();
				live.removeChild(liveChild);
				liveChild = nextLive;
				continue;
			}

			if (liveChild == null) {
				// new nodes to append
				live.appendChild(freshChild.cloneNode(true));
				freshChild = freshChild.getNextSibling();
				continue;
			}

			Node nextLive = liveChild.getNextSibling();
			Node nextFresh = freshChild.getNextSibling();

			patchNode(live, liveChild, freshChild);

			liveChild = nextLive;
			freshChild = nextFresh;
		}
	}

	private void patchNode(HTMLElement parent, Node live, Node fresh) {
		short liveType = live.getNodeType();
		short freshType = fresh.getNodeType();

		// Case 1: both text nodes
		if (liveType == Node.TEXT_NODE && freshType == Node.TEXT_NODE) {
			if (!safeEquals(live.getNodeValue(), fresh.getNodeValue())) {
				live.setNodeValue(fresh.getNodeValue());
			}
			return;
		}

		// Case 2: node type changed (text <-> element, comment, etc.)
		if (liveType != freshType) {
			Node replacement = fresh.cloneNode(true);
			parent.replaceChild(replacement, live);
			return;
		}

		// Case 3: both elements
		if (liveType == Node.ELEMENT_NODE) {
			patchElement((HTMLElement) live, (HTMLElement) fresh);
			return;
		}

		// Other node types – simplest: replace
		Node replacement = fresh.cloneNode(true);
		parent.replaceChild(replacement, live);
	}

	private boolean safeEquals(String a, String b) {
		if (a == null) return b == null;
		return a.equals(b);
	}


	public void close() {
		setVisible(false);
		Taskbar.get().unregisterWindow(this);
	}

	protected void ensureDelegatedClicks() {
		if (delegatedClicksInstalled) {
			return;
		}
		delegatedClicksInstalled = true;

		bodyEl.addEventListener("click", evt -> {
			org.teavm.jso.dom.events.Event event =
				(org.teavm.jso.dom.events.Event) evt;

			HTMLElement target = (HTMLElement) event.getTarget();

			HTMLElement actionEl = findActionElement(target);
			if (actionEl == null) {
				return;
			}

			String handlerId = actionEl.getAttribute("data-aw-handler");
			if (handlerId == null) {
				return;
			}

			Runnable handler = clickHandlers.get(handlerId);
			if (handler != null) {
				event.preventDefault();
				schedule(handler);
			}
		});
	}

	// Walk up DOM until we find something with data-aw-handler
	private HTMLElement findActionElement(HTMLElement el) {
		HTMLElement cur = el;
		while (cur != null) {
			String id = cur.getAttribute("data-aw-handler");
			if (id != null && !id.isEmpty()) {
				return cur;
			}
			if (!(cur.getParentNode() instanceof HTMLElement)) {
				return null;
			}
			cur = (HTMLElement) cur.getParentNode();
		}
		return null;
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

		// Click to bring to front
		container.addEventListener("onclick", evt -> bringToFront());

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

		bodyEl.addEventListener("mousedown", evt -> {
			bringToFront();
		});

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

	public void setTitle(String title) {
		this.title = title;
		if (titleEl != null) {
			titleEl.setTextContent("AWTea - " + title);
		}
	}

	// ----- Dragging -----

	private void startDrag(MouseEvent e) {
		dragging = true;
		dragStartMouseX = e.getClientX();
		dragStartMouseY = e.getClientY();
		dragStartLeft = Math.max(container.getOffsetLeft(), 0);
		dragStartTop = Math.max(container.getOffsetTop(), 0);

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
				int newTop = dragStartTop + dy;

				CSSStyleDeclaration style = container.getStyle();
				style.setProperty("left", newLeft + "px");
				style.setProperty("top", newTop + "px");

				evt.preventDefault();
			} else if (resizing) {
				int dx = e.getClientX() - resizeStartMouseX;
				int dy = e.getClientY() - resizeStartMouseY;

				int newWidth = Math.max(minWidth, resizeStartWidth + dx);
				int newHeight = Math.max(minHeight, resizeStartHeight + dy);

				CSSStyleDeclaration style = container.getStyle();
				style.setProperty("width", newWidth + "px");
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

		if (!resizable) {
			return;
		}

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

		if (!resizable) {
			return;
		}

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
