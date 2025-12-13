package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public abstract class JSRecord implements JSObject {

    @JSBody(params = "key", script = "return this[key];")
    public native <T> T get(String key);

    @JSBody(params = {"key", "defaultValue"}, script = "return this[key] || defaultValue;")
    public native <T> T getOrDefault(String key, T defaultValue);

    public final <T> T getOrCompute(String key, Provider<T> provider) {
        if (!has(key)) {
            T value = provider.get();
            put(key, value);
            return value;
        }
        return get(key);
    }

    @JSBody(params = {"key", "value"}, script = "this[key] = value;")
    public native <T> void put(String key, T value);

    @JSBody(params = "key", script = "return this[key];")
    public native <T> T get(int key);

    @JSBody(params = {"key", "defaultValue"}, script = "return this[key] || defaultValue;")
    public native <T> T getOrDefault(int key, T defaultValue);

    @JSBody(params = {"key", "value"}, script = "this[key] = value;")
    public native <T> void put(int key, T value);

    @JSBody(params = {"key"}, script = "return !!this[key];")
    public native boolean has(int key);

    @JSBody(params = {"key"}, script = "return !!this[key];")
    public native boolean has(String key);

    @JSBody(script = "return {};")
    public static native JSRecord create();

    public interface Provider<T> {
        T get();
    }
}
