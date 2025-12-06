package me.mdbell.awtea.util.ui;


import me.mdbell.awtea.util.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

public final class UiDispatcher {
	private static final List<Runnable> queue = new ArrayList<>();
	private static final int INVOKE_INTERVAL_MS = 50;

	static {
		ThreadUtils.runAtFixedRate("AWTea-dispatcher", () -> {
			List<Runnable> toRun;
			synchronized (queue) {
				toRun = new ArrayList<>(queue);
				queue.clear();
			}
			for (Runnable r : toRun) {
				try {
					r.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}, INVOKE_INTERVAL_MS);
	}

	private UiDispatcher() {
	}

	public static void invokeLater(Runnable r) {
		synchronized (queue) {
			queue.add(r);
		}
	}
}
