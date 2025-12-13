package me.mdbell.awtea.classlib.java.awt.awtea;

import me.mdbell.awtea.classlib.java.awt.TComponent;

public class TFocusManager {

	private static final TFocusManager instance = new TFocusManager();

	private TComponent globalFocusOwner;

	private TFocusManager() {

	}

	public static TFocusManager get() {
		return instance;
	}

	public TComponent getGlobalFocusOwner() {
		return globalFocusOwner;
	}

	public void clearGlobalFocusOwner() {
		if (globalFocusOwner == null) {
			return;
		}
		TComponent oldFocusOwner = globalFocusOwner;
		globalFocusOwner = null;
		oldFocusOwner.fireFocusLost();
	}

	public void setGlobalFocusOwner(TComponent component) {
		if (globalFocusOwner == component) {
			return;
		}

		clearGlobalFocusOwner();

		if (component == null || !component.isFocusable()) {
			return;
		}

		component.fireFocusGained();
		globalFocusOwner = component;
	}
}
