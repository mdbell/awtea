package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThreadUtils {

    public void runAtFixedRate(String name, Runnable runnable, long period) {
        Thread thread = new Thread(() -> {
            while (true) {
                runnable.run();
                try {
                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
    }
}
