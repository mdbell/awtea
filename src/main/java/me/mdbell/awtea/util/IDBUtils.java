package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBFactory;
import org.teavm.jso.indexeddb.IDBOpenDBRequest;
import org.teavm.jso.indexeddb.IDBVersionChangeEvent;
import me.mdbell.awtea.impl.idb.IndexDBVirtualFSException;

@UtilityClass
public class IDBUtils {

    private final IDBFactory factory = IDBFactory.getInstance();

    static {
        requestPersistentStorage();
    }

    @Async
    public native IDBDatabase open(String name, int version, EventListener<IDBVersionChangeEvent> upgradeHandler);

    @SuppressWarnings("unused")
    private void open(String name, int version, EventListener<IDBVersionChangeEvent> upgradeHandler, AsyncCallback<IDBDatabase> callback) {
        IDBOpenDBRequest request = factory.open(name, version);

        request.setOnUpgradeNeeded(upgradeHandler);

        request.setOnError(() -> {
            System.err.println(request.getError());
            callback.error(new IndexDBVirtualFSException("Failed to open database"));
        });

        request.setOnSuccess(() -> callback.complete(request.getResult()));
    }


    @Async
    private native boolean requestPersistentStorage();

    private void requestPersistentStorage(AsyncCallback<Boolean> callback) {

        if (!hasStorage()) {
            System.err.println("Persistent storage not supported.");
            callback.complete(false);
            return;
        }

        requestPersistentStorageImpl().then(value -> {
            callback.complete(value.booleanValue());
            return value;
        }).catchError(err -> {
            System.err.println("Failed to request persistent storage: " + err);
            callback.complete(false);
            return false;
        });

    }

    @JSBody(script = "return navigator.storage && navigator.storage.persist;")
    private native boolean hasStorage();

    @JSBody(script = "return navigator.storage.persist();")
    private static native JSPromise<JSBoolean> requestPersistentStorageImpl();
}
