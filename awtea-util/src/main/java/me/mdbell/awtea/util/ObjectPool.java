package me.mdbell.awtea.util;

import lombok.NonNull;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectPool<T> {

    private static final Logger log = LoggerFactory.getLogger(ObjectPool.class);

    private final T[] free;
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final Consumer<T> destroyer;
    private int freeIndex = 0;

    public ObjectPool(@NonNull Supplier<T> factory,
                      Consumer<T> resetter,
                      Consumer<T> destroyer,
                      int capacity) {
        // This seems wrong, but it's how TeaVM generates ArrayLists so like...
        //noinspection unchecked
        this.free = (T[]) new Object[capacity];
        this.factory = factory;
        this.resetter = resetter;
        this.destroyer = destroyer;
    }

    public T obtain() {
        if (freeIndex > 0) {
            return free[--freeIndex];
        }
        return factory.get();
    }

    public void release(T obj) {
        if (resetter != null) {
            resetter.accept(obj);
        }
        if (freeIndex < free.length) {
            free[freeIndex++] = obj;
            return;
        }

        log.warn("ObjectPool: Pool is full, destroying released object of type {}", obj.getClass().getName());

        if (destroyer != null) {
            destroyer.accept(obj);
        }
    }
}
