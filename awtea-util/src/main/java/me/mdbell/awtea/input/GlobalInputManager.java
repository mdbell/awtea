package me.mdbell.awtea.input;

import lombok.Getter;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GlobalInputManager {

    @Getter
    private static final GlobalInputManager instance = new GlobalInputManager();

    private final List<GlobalInputInterceptor> interceptors = new ArrayList<>();

    private GlobalInputManager() {

    }

    /**
     * Adds a global input interceptor.
     *
     * @param interceptor the interceptor to add
     */
    public void addInterceptor(GlobalInputInterceptor interceptor) {
        synchronized (interceptors) {
            interceptors.add(interceptor);
        }
    }

    public void removeInterceptor(GlobalInputInterceptor interceptor) {
        synchronized (interceptors) {
            interceptors.remove(interceptor);
        }
    }

    public boolean handleEvent(Object event) {
        if (event instanceof MouseEvent) {
            return handleMouseEvent((MouseEvent) event);
        } else if (event instanceof java.awt.event.KeyEvent) {
            return handleKeyEvent((java.awt.event.KeyEvent) event);
        }
        return false;
    }

    public boolean handleMouseEvent(MouseEvent e) {
        synchronized (interceptors) {
            for (GlobalInputInterceptor interceptor : interceptors) {
                if (interceptor.onMouseEvent(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean handleKeyEvent(java.awt.event.KeyEvent e) {
        synchronized (interceptors) {
            for (GlobalInputInterceptor interceptor : interceptors) {
                if (interceptor.onKeyEvent(e)) {
                    return true;
                }
            }
        }
        return false;
    }

}
