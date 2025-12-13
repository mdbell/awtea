package me.mdbell.awtea.ui;

import lombok.Getter;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.function.Consumer;

public class MenuBar {

	static {
		AwCss.sheet()
			.createClass("aw-menubar")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("gap", "0.5rem")
			.prop("padding", "0 0.5rem")
			.prop("height", "24px")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.HEADER_BORDER)
			.end()
			.prop("background-color", Theme.Var.HEADER_BACKGROUND)
			.prop("font-size", "12px")
			.prop("user-select", "none")
			.end()

			.createClass("aw-menu-item")
			.prop("width", "fit-content")
			.prop("position", "relative")
			.prop("padding", "0 0.5rem")
			.prop("line-height", "24px")
			.prop("cursor", "default")
			.prop("color", Theme.Var.FOREGROUND)
			.end()

			.createClass("aw-menu-item-label")
			.prop("cursor", "pointer")
			.end()

			// simple hover highlight
			.createClass("aw-menu-item:hover")
			.prop("background-color", Theme.Var.BUTTON_BACKGROUND)
			.end()

			// dropdown
			.createClass("aw-menu-dropdown")
			.prop("position", "absolute")
			.prop("top", "100%")
			.prop("left", "0")
			.prop("min-width", "120px")
			.prop("background-color", Theme.Var.BACKGROUND)
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("box-shadow")
			.value("0 2px 6px")
			.value(Theme.Var.SHADOW)
			.end()
			.prop("z-index", "99999")
			.prop("padding", "0.25rem 0")
			.prop("display", "none")
			.end()

			.createClass("aw-menu-item:hover")
			.prop("z-index", "100000") // so dropdown is above neighbours
			.end()

			.createClass("aw-menu-item:hover .aw-menu-dropdown")
			.prop("display", "block")
			.end()

			.createClass("aw-menu-entry")
			.prop("padding", "0.15rem 0.75rem")
			.prop("white-space", "nowrap")
			.prop("cursor", "pointer")
			.prop("font-size", "12px")
			.end()

			.createClass("aw-menu-entry:hover")
			.prop("background-color", Theme.Var.BUTTON_BACKGROUND)
			.end()

			.inject();

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
