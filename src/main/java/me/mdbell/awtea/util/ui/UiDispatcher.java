package me.mdbell.awtea.util.ui;


import java.util.ArrayList;
import java.util.List;

public final class UiDispatcher {
	private static final List<Runnable> queue = new ArrayList<>();
	private static final int INVOKE_INTERVAL_MS = 50;

	static {
		new Thread(() -> {
			while (true) {
				List<Runnable> toRun;
				synchronized (queue) {
					if (queue.isEmpty()) {
						toRun = null;
					} else {
						toRun = new ArrayList<>(queue);
						queue.clear();
					}
				}
				if (toRun != null) {
					for (Runnable r : toRun) {
						try {
							r.run();
						} catch (Throwable ignored) {
						}
					}
				}
				try {
					Thread.sleep(INVOKE_INTERVAL_MS);
				} catch (InterruptedException ignored) {
				}
			}
		}).start();
	}

	private UiDispatcher() {
	}

	public static void invokeLater(Runnable r) {
		synchronized (queue) {
			queue.add(r);
		}
	}
}
