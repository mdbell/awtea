package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TComponentEvent;

import java.util.ArrayList;
import java.util.List;

public class TContainer extends TComponent {

    private List<TComponent> children = new ArrayList<>();

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

	public void setFocusCycleRoot(boolean focusCycleRoot) {
		// do nothing
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

    @Override
    public void dispatchEvent(TComponentEvent event) {
        for (TComponent child : children) {
            if (event.isConsumed()) {
                break;
            }
            child.dispatchEvent(event);
        }
    }
}
