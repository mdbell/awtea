package me.mdbell.awtea.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.ElementUtils;
import me.mdbell.awtea.util.ThreadUtils;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.css.CSSStyleDeclaration;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.xml.Node;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ExtensionMethod({ElementUtils.class})
public abstract class FloatingWindow {

	protected HTMLDocument document;
	protected HTMLElement container;

	// shell elements
	protected HTMLElement bodyEl;

	// window identity / persistence
	@Getter(AccessLevel.PROTECTED)
	private final String windowId;

	private String storagePosLeftKey;
	private String storagePosRightKey;

	// drag
	protected boolean dragging = false;
	protected int dragStartMouseX;
	protected int dragStartMouseY;
	protected int dragStartLeft;
	protected int dragStartTop;

	protected boolean pointerListenersAttached = false;

	// config
	private final int refreshIntervalMs;

	private HTMLElement bodyContent;   // current body root
	protected String lastSignature;

	private int refreshIntervalId = -1;

	@Getter
	@Setter
	private boolean autoscroll = false;
	protected boolean stickToBottom = true;

	private boolean delegatedClicksInstalled = false;

	private boolean fixedIntervalStarted = false;

	// Simple registry: "actionId" -> Runnable
	private final Map<String, Runnable> clickHandlers = new HashMap<>();

	protected static int zCounter = 10000;

	static {
		// Inject the CSS from embedded file
		HTMLElement style = Window.current()
			.getDocument()
			.createElement("style");
		style.setTextContent(UiStyles.floatingWindowCSS());
		Window.current()
			.getDocument()
			.getHead()
			.appendChild(style);
	}

	protected FloatingWindow(String windowId) {
		this.windowId = windowId;
		this.refreshIntervalMs = 0;
		init();
		buildShell();
	}

	protected FloatingWindow(String windowId,
							 int widthPx,
							 int heightPx,
							 int refreshIntervalMs) {
		this.windowId = windowId;
		this.refreshIntervalMs = refreshIntervalMs;

		init();
		buildShell();

		setSize(widthPx, heightPx);
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

	protected void renderIfChanged() {
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

	protected void buildShell() {
		container.setInnerHTML("");

		// Click to bring to front
		container.addEventListener("onclick", evt -> bringToFront());

		// Body
		bodyEl = document.createElement("div");
		bodyEl.setClassName("aw-window-body");

		bodyEl.addEventListener("mousedown", evt -> {
			bringToFront();
		});

		container.appendChild(bodyEl);

		bodyEl.addEventListener("scroll", evt -> {
			int scrollBottom = bodyEl.getScrollHeight() - (bodyEl.getScrollTop() + bodyEl.getClientHeight());
			stickToBottom = scrollBottom < 20; // threshold
		});
	}

	// ----- Dragging -----

	protected void startDrag(MouseEvent e) {
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

	protected void attachPointerListeners() {
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
			}
		});

		document.addEventListener("mouseup", evt -> {
			if (dragging) {
				dragging = false;
				schedule(this::savePersistentData);
			}
		});
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

	protected void savePersistentData() {
		if (dragging) {
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
