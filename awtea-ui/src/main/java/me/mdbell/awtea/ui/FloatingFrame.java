package me.mdbell.awtea.ui;

import lombok.Getter;
import lombok.Setter;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.css.CSSStyleDeclaration;

/**
 * Decorated floating window with title bar, controls, and resize handle.
 * Extends the base undecorated FloatingWindow class.
 */
public abstract class FloatingFrame extends FloatingWindow {

	protected HTMLElement headerControlsEl;
	protected HTMLElement titleEl;

	@Getter
	private String title;

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

	private HTMLElement taskbarButton;
	private boolean minimized = false;

	@Getter
	@Setter
	private MenuBar menuBar;

	static {
		// Inject the CSS from embedded file
		HTMLElement style = Window.current()
			.getDocument()
			.createElement("style");
		style.setTextContent(UiStyles.floatingFrameCSS());
		Window.current()
			.getDocument()
			.getHead()
			.appendChild(style);
	}

	protected FloatingFrame(String windowId) {
		super(windowId);
		Taskbar.get().registerWindow(this);
	}

	protected FloatingFrame(String windowId,
							String titleText,
							int widthPx,
							int heightPx,
							int refreshIntervalMs) {
		super(windowId, widthPx, heightPx, refreshIntervalMs);
		this.title = titleText;
		setTitle(titleText);
		Taskbar.get().registerWindow(this);
	}

	@Override
	protected void buildShell() {
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
			setStickToBottom(scrollBottom < 20); // threshold
		});
	}

	public void setTitle(String title) {
		this.title = title;
		if (titleEl != null) {
			titleEl.setTextContent("AWTea - " + title);
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

	@Override
	public void close() {
		super.close();
		Taskbar.get().unregisterWindow(this);
	}

	@Override
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
			}
		});
	}

	@Override
	protected void renderIfChanged() {
		// Handle menu bar positioning before parent rendering
		if (menuBar != null) {
			HTMLElement element = menuBar.getElement();
			HTMLElement menuParent = element.getParentNode() != null ? (HTMLElement) element.getParentNode() : null;
			// ensure menu bar is first child of body
			if (menuParent != container) {
				if (menuParent != null) {
					menuParent.removeChild(element);
				}
				// Insert before bodyEl (which comes after header in decorated windows)
				HTMLElement header = container.querySelector(".aw-window-header");
				if (header != null && header.getNextSibling() != null) {
					container.insertBefore(element, header.getNextSibling());
				} else {
					container.insertBefore(element, bodyEl);
				}
			}
		}

		// Delegate to parent for actual content rendering
		super.renderIfChanged();
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

	private void setStickToBottom(boolean stickToBottom) {
		this.stickToBottom = stickToBottom;
	}

	@Override
	protected void savePersistentData() {
		// Don't save while resizing (parent will check dragging)
		if (resizing) {
			return;
		}
		super.savePersistentData();
	}
}
