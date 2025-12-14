package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @see java.awt.Container
 */
public class TContainer extends TComponent {

    private static final Logger log = LoggerFactory.getLogger(TContainer.class);

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

    @Override
    public void paint(TGraphics g) {
        for (TComponent component : children) {
            int x = component.x;
            int y = component.y;
            int width = component.width;
            int height = component.height;
            
            // Skip components with zero or negative dimensions
            if (width <= 0 || height <= 0) {
                continue;
            }
            
            // Create a new graphics context for the child component
            TGraphics childGraphics = g.create();
            if (childGraphics != null) {
                // Translate to the child's position
                childGraphics.translate(x, y);
                // Clip to the child's bounds (in the child's coordinate system, so at 0, 0)
                childGraphics.clipRect(0, 0, width, height);
                // Paint the child
                component.paint(childGraphics);
                childGraphics.dispose();
            }
        }
    }

    public TComponent[] getComponents() {
        return children.toArray(TComponent[]::new);
    }

    public TComponent getComponentAt(int x, int y) {
        log.trace("TContainer.getComponentAt({}, {}) called on {}", x, y, this.getClass().getName());
        for (TComponent child : children) {
            if (child.contains(x, y)) {
                log.trace("Point ({}, {}) is within component {}", x, y, child.getClass().getName());
                if (child instanceof TContainer) {
                    TComponent deeper = ((TContainer) child).getComponentAt(x, y);
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
