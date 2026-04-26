package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public final class JSAsyncIterators {

    private JSAsyncIterators() {
    }

    @JSBody(params = {"obj"}, script = "return obj != null && typeof obj[Symbol.asyncIterator] === 'function';")
    public static native boolean isAsyncIterator(JSObject obj);

    /**
     * Converts the given async iterator into a standard Java iterator that can be used in a for-each loop or with other Java collection utilities.
     *
     * @param iterator the async iterator to convert
     * @param <T>      the type of elements produced by the async iterator
     * @return a standard Java iterator that produces the elements of the async iterator
     */
    public static <T extends JSObject> Iterator<T> asIterator(JSAsyncIterator<T> iterator) {
        return new AsyncIteratorAdapter<>(iterator);
    }

    /**
     * Converts the given async iterator into an iterable that can be used in a for-each loop.
     *
     * @param iterator the async iterator to convert
     * @param <T>      the type of elements produced by the async iterator
     * @return an iterable that produces the elements of the async iterator
     */
    public static <T extends JSObject> Iterable<T> asIterable(JSAsyncIterator<T> iterator) {
        return new SingleUseIterable<>(() -> asIterator(iterator));
    }

    /**
     * Maps the elements of the given async iterator using the provided mapper function.
     *
     * @param iterator the async iterator to map
     * @param mapper   the function to apply to each element of the async iterator
     * @param <T>      the type of elements produced by the async iterator
     * @param <R>      the type of elements produced by the resulting iterable
     * @return an iterable that produces the mapped elements of the async iterator
     */
    public static <T extends JSObject, R> Iterable<R> map(JSAsyncIterator<T> iterator,
                                                          Function<? super T, ? extends R> mapper) {
        return new SingleUseIterable<>(() -> new Iterator<R>() {
            private final Iterator<T> delegate = asIterator(iterator);

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(delegate.next());
            }
        });
    }

    @FunctionalInterface
    private interface IteratorFactory<T> {
        Iterator<T> create();
    }

    private static final class SingleUseIterable<T> implements Iterable<T> {
        private final IteratorFactory<T> factory;
        private boolean consumed;

        private SingleUseIterable(IteratorFactory<T> factory) {
            this.factory = factory;
        }

        @Override
        public Iterator<T> iterator() {
            if (consumed) {
                throw new IllegalStateException("Async iterator can only be iterated once");
            }
            consumed = true;
            return factory.create();
        }
    }

    private static final class AsyncIteratorAdapter<T extends JSObject> implements Iterator<T> {
        private final JSAsyncIterator<T> iterator;
        private boolean loaded;
        private boolean hasNext;
        private T nextValue;

        private AsyncIteratorAdapter(JSAsyncIterator<T> iterator) {
            this.iterator = iterator;
        }

        private void loadNext() {
            if (loaded) {
                return;
            }
            JSAsyncIteratorResult<T> result = iterator.next().await();
            if (result == null || result.isDone()) {
                hasNext = false;
                nextValue = null;
            } else {
                hasNext = true;
                nextValue = result.getValue();
            }
            loaded = true;
        }

        @Override
        public boolean hasNext() {
            loadNext();
            return hasNext;
        }

        @Override
        public T next() {
            loadNext();
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            T value = nextValue;
            loaded = false;
            nextValue = null;
            return value;
        }
    }
}
