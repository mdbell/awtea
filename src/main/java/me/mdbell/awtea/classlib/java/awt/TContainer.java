package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @see java.awt.Container
 */
public class TContainer extends TComponent {

	private List<TComponent> children = new ArrayList<>();

	@Getter
	@Setter
	private boolean focusCycleRoot = false;

	public TComponent add(TComponent component) {
		this.children.add(component);
		component.setParent(this);
		return component;
	}

	public void remove(TComponent comp) {
		if (comp.getParent() != this) {
			// not us, do nothing
			return;
		}
		comp.setParent(null);
		this.children.remove(comp);
	}

	public boolean isValidateRoot() {
		return false;
	}

	public void update(TGraphics g) {
		paint(g);
	}

	@Override
	public void paint(TGraphics g) {
		for (TComponent component : children) {
			int x = component.x;
			int y = component.y;
			g.translate(x, y);
			component.paint(g);
			g.resetTranslate(x, y);
		}
	}

	public TComponent getComponentAt(int x, int y) {
		for (TComponent child : children) {
			if (x >= child.getX() && x < child.getX() + child.getWidth()
				&& y >= child.getY() && y < child.getY() + child.getHeight()) {
				if (child instanceof TContainer) {
					TComponent deeper = ((TContainer) child).getComponentAt(x - child.getX(), y - child.getY());
					if (deeper != null) {
						return deeper;
					}
				}
				return child;
			}
		}
		return this;
	}

//	@Override
//	public void dispatchEvent(TAWTEvent event) {
//		for (TComponent child : children) {
//			if (event.isConsumed()) {
//				break;
//			}
//			child.dispatchEvent(event);
//		}
//	}
}
