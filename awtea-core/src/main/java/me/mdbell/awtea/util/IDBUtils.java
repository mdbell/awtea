package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(IDBUtils.class);

    private final IDBFactory factory = IDBFactory.getInstance();

    static {
		requestPersistentStorageImpl().await();
    }

    @Async
    public native IDBDatabase open(String name, int version, EventListener<IDBVersionChangeEvent> upgradeHandler);

    @SuppressWarnings("unused")
    private void open(String name, int version, EventListener<IDBVersionChangeEvent> upgradeHandler, AsyncCallback<IDBDatabase> callback) {
        IDBOpenDBRequest request = factory.open(name, version);

        request.setOnUpgradeNeeded(upgradeHandler);

        request.setOnError(() -> {
            log.error("Failed to open database: {}", request.getError());
            callback.error(new IndexDBVirtualFSException("Failed to open database"));
        });

        request.setOnSuccess(() -> callback.complete(request.getResult()));
    }

    @JSBody(script = "return navigator.storage && navigator.storage.persist;")
    private native boolean hasStorage();

    @JSBody(script = "return navigator.storage.persist();")
    private static native JSPromise<JSBoolean> requestPersistentStorageImpl();
}
