package me.mdbell.awtea.ui;

import lombok.Getter;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.function.Consumer;

public class MenuBar {

	static {
		// Inject the CSS from embedded file
		HTMLElement style = Window.current()
			.getDocument()
			.createElement("style");
		style.setTextContent(UiStyles.menubarCSS());
		Window.current()
			.getDocument()
			.getHead()
			.appendChild(style);
	}

	private final HTMLDocument document;
	@Getter
	private final HTMLElement element;
	private final Consumer<Runnable> scheduler; // usually FloatingWindow::schedule

	public MenuBar(Consumer<Runnable> scheduler) {
		this.document = Window.current().getDocument();
		this.element = document.createElement("div");
		this.element.setClassName("aw-menubar");
		this.scheduler = scheduler;
	}

	public interface MenuHandle {
		void addAction(String label, Runnable action);

		HTMLElement getElement();
	}

	public MenuHandle addMenu(String label) {
		HTMLElement item = document.createElement("div");
		item.setClassName("aw-menu-item");

		HTMLElement itemLabel = document.createElement("span");
		itemLabel.setClassName("aw-menu-item-label");
		itemLabel.setTextContent(label);
		item.appendChild(itemLabel);

		HTMLElement dropdown = document.createElement("div");
		dropdown.setClassName("aw-menu-dropdown");
		item.appendChild(dropdown);

		element.appendChild(item);

		return new MenuHandle() {
			@Override
			public void addAction(String entryLabel, Runnable action) {
				HTMLElement entry = document.createElement("div");
				entry.setClassName("aw-menu-entry");
				entry.setTextContent(entryLabel);
				entry.addEventListener("click", evt -> {
					evt.preventDefault();
					if (action != null) {
						scheduler.accept(action);
					}
				});
				dropdown.appendChild(entry);
			}

			@Override
			public HTMLElement getElement() {
				return item;
			}
		};
	}
}
