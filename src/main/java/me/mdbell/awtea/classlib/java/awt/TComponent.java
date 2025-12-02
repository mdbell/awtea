package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.event.*;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class TComponent {

    @Getter
    @Setter
    TContainer parent;

    @Getter
    @Setter
    protected int x, y, width, height;

    protected List<TMouseListener> mouseListeners = new ArrayList<>();
    protected List<TMouseMotionListener> mouseMotionListeners = new ArrayList<>();
    protected List<TMouseWheelListener> mouseWheelListeners = new ArrayList<>();
    protected List<TKeyListener> keyListeners = new ArrayList<>();
    protected List<TFocusListener> focusListeners = new ArrayList<>();

    public TFontMetrics getFontMetrics(TFont font) {
        return getGraphics().measureText(font);
    }

    public TImage createImage(int width, int height) {
		return new TBufferedImage(width, height);
    }

    public TImage createImage(TImageProducer producer) {
        return TTeaVmToolkit.getDefaultToolkit().createImage(producer);
    }

    public void dispatchEvent(TComponentEvent event) {
        if (event.isConsumed()) {
            return;
        }
        if (event instanceof TKeyEvent) {
            dispatchKeyEvent((TKeyEvent) event);
        } else if (event instanceof TMouseEvent) {
            dispatchMouseEvent((TMouseEvent) event);
        } else if (event instanceof TFocusEvent) {
            dispatchFocusEvent((TFocusEvent) event);
        } else {
            System.err.println("Unhandled event type:" + event.getClass().getName());
        }
    }

    public void dispatchKeyEvent(TKeyEvent e) {
        switch (e.getId()) {
            case TKeyEvent.KEY_PRESSED:
                dispatchEvent(e, keyListeners, TKeyListener::keyPressed);
                break;
            case TKeyEvent.KEY_RELEASED:
                dispatchEvent(e, keyListeners, TKeyListener::keyReleased);
                break;
            case TKeyEvent.KEY_TYPED:
                dispatchEvent(e, keyListeners, TKeyListener::keyTyped);
                break;
            default:
                System.err.println("Unhandled key event:" + e.getId());
        }
    }

    public void dispatchMouseEvent(TMouseEvent e) {
        if (!this.contains(e.getX(), e.getY())) {
            return; //TODO maybe do this in the parent?
        }
        switch (e.getId()) {
            case TMouseEvent.MOUSE_MOVED:
                dispatchEvent(e, mouseMotionListeners, TMouseMotionListener::mouseMoved);
                break;
            case TMouseEvent.MOUSE_DRAGGED:
                dispatchEvent(e, mouseMotionListeners, TMouseMotionListener::mouseDragged);
                break;
            case TMouseEvent.MOUSE_CLICKED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseClicked);
                break;
            case TMouseEvent.MOUSE_PRESSED:
                dispatchEvent(e, mouseListeners, TMouseListener::mousePressed);
                break;
            case TMouseEvent.MOUSE_RELEASED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseReleased);
                break;
            case TMouseEvent.MOUSE_ENTERED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseEntered);
                break;
            case TMouseEvent.MOUSE_EXITED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseExited);
                break;
            case TMouseEvent.MOUSE_WHEEL:
                dispatchEvent((TMouseWheelEvent) e, mouseWheelListeners, TMouseWheelListener::mouseWheelMoved);
                break;
            default:
                System.err.println("Unhandled mouse event:" + e.getId());
        }
    }

    public void dispatchFocusEvent(TFocusEvent e) {
        switch (e.getId()) {
            case TFocusEvent.FOCUS_GAINED:
                dispatchEvent(e, focusListeners, TFocusListener::focusGained);
                break;
            case TFocusEvent.FOCUS_LOST:
                dispatchEvent(e, focusListeners, TFocusListener::focusLost);
                break;
            default:
                System.err.println("Unhandled focus event:" + e.getId());
        }
    }

    public <T extends TComponentEvent, L> void dispatchEvent(
            T event, List<L> listeners, BiConsumer<L, T> eventHandler) {
        for (L listener : listeners) {
            eventHandler.accept(listener, event);
            if (event.isConsumed()) {
                break; // Stop sending to subsequent listeners
            }
        }
    }

    public void addKeyListener(TKeyListener l) {
        this.keyListeners.add(l);
    }

    public void addMouseListener(TMouseListener l) {
        this.mouseListeners.add(l);
    }

    public void removeKeyListener(TKeyListener l) {
        this.keyListeners.remove(l);
    }

    public void removeMouseListener(TMouseListener l) {
        this.mouseListeners.add(l);
    }

    public void addFocusListener(TFocusListener l) {
        this.focusListeners.add(l);
    }

    public void addMouseMotionListener(TMouseMotionListener l) {
        this.mouseMotionListeners.add(l);
    }

    public void removeFocusListener(TFocusListener l) {
        this.focusListeners.remove(l);
    }

    public void removeMouseMotionListener(TMouseMotionListener l) {
        this.mouseMotionListeners.remove(l);
    }

    public void addMouseWheelListener(TMouseWheelListener l) {
        this.mouseWheelListeners.add(l);
    }

    public void removeMouseWheelListener(TMouseWheelListener l) {
        this.mouseWheelListeners.remove(l);
    }

    public void requestFocus() {

    }

	public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {

	}

    public TGraphics getGraphics() {
        return parent.getGraphics();
    }

    public abstract void paint(TGraphics g);

    public void repaint() {
        TGraphics gfx = getGraphics();
        gfx.reset();
        paint(gfx);
    }

    public final boolean contains(int px, int py) {
        return px >= x && px < this.x + this.width && py >= y && py < this.y + this.height;
    }

    public final void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setVisible(boolean b) {
        //stubbed
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    //TODO: we already load images kinda sync, so we just stub these for now.
    public int checkImage(TImage image, int width, int height, Object o) {
        return TImageObserver.ALLBITS;
    }

    public boolean prepareImage(TImage image, TImageObserver observer) {
        return true;
    }

    public boolean prepareImage(TImage image, int width, int height, TImageObserver observer) {
        return true;
    }
}
