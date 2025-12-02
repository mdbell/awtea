package me.mdbell.awtea.util;

import me.mdbell.awtea.util.jso.JSRecord;

import java.util.function.Function;

public abstract class EnumLookup {

    private EnumLookup() {

    }

    public static class StringKey<T extends Enum<T>> {
        private final JSRecord lookup = JSRecord.create();

        public StringKey(T[] values, Function<T, String> keyExtractor) {
            for (T value : values) {
                lookup.put(keyExtractor.apply(value), value);
            }
        }

        public T get(String key) {
            return lookup.get(key);
        }

        public T getOrDefault(String key, T defaultValue) {
            return lookup.getOrDefault(key, defaultValue);
        }
    }

    public static class IntegerKey<T extends Enum<T>> {
        private final JSRecord lookup = JSRecord.create();

        public IntegerKey(T[] values, Function<T, Integer> keyExtractor) {
            for (T value : values) {
                lookup.put(keyExtractor.apply(value), value);
            }
        }

        public T get(Integer key) {
            return lookup.get(key);
        }

        public T getOrDefault(int key, T defaultValue) {
            return lookup.getOrDefault(key, defaultValue);
        }
    }

}
