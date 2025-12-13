package me.mdbell.awtea.impl.idb;

import lombok.experimental.UtilityClass;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.indexeddb.*;
import me.mdbell.awtea.util.IDBUtils;
import me.mdbell.awtea.util.JSObjectsExtensions;

@UtilityClass
public class IDBStore {

    public static final String DB_NAME = "filestore";

    public static final String STORE_NAME = "files";

    public static final String INDEX_PROPERTY = "index";
    public static final String FILE_PROPERTY = "file";
    public static final String DATA_PROPERTY = "data";

    public static final int DB_VERSION = 1;

    private static final IDBDatabase db = IDBUtils.open(DB_NAME, DB_VERSION, IDBStore::upgrade);

    private static void upgrade(IDBVersionChangeEvent evt) {
        IDBDatabase db = ((IDBOpenDBRequest) evt.getTarget()).getResult();
        if (evt.getOldVersion() == 0) {
            IDBObjectStoreParameters params = JSObjects.create();
            params.keyPath(INDEX_PROPERTY, FILE_PROPERTY);
            IDBObjectStore store = db.createObjectStore(STORE_NAME, params);
        }
    }

    @Async
    public static native boolean put(int index, int file, byte[] data);

    @SuppressWarnings("unused")
    private static void put(int index, int file, byte[] data, AsyncCallback<Boolean> callback) {
        IDBTransaction transaction = db.transaction(STORE_NAME, "readwrite");
        IDBObjectStore store = transaction.objectStore(STORE_NAME);

        IDBStoreEntry entry = IDBStoreEntry.create(index, file);

        entry.setData(data);

        IDBRequest request = store.put(entry);

        request.setOnSuccess(() -> {
            callback.complete(true);
        });

        request.setOnError(() -> {
            // reusing the same exception for now.
            callback.error(new IndexDBVirtualFSException("Failed to put file"));
        });
    }

    @Async
    public static native byte[] get(int index, int file);

    @SuppressWarnings("unused")
    private static void get(int index, int file, AsyncCallback<byte[]> callback) {
        IDBTransaction transaction = db.transaction(STORE_NAME, "readonly");
        IDBObjectStore store = transaction.objectStore(STORE_NAME);
        IDBGetRequest request = store.get(JSArray.of(JSNumber.valueOf(index), JSNumber.valueOf(file)));

        request.setOnSuccess(() -> {
            IDBStoreEntry entry = (IDBStoreEntry) request.getResult();

            if (JSObjectsExtensions.nullish(entry)) {
                callback.complete(null);
                return;
            }

            callback.complete(entry.getData());
        });

        request.setOnError(() -> {
            // reusing the same exception for now.
            callback.error(new IndexDBVirtualFSException("Failed to get file"));
        });
    }

    private interface IDBStoreEntry extends JSObject {

        @JSProperty(INDEX_PROPERTY)
        int getIndex();

        @JSProperty(INDEX_PROPERTY)
        void setIndex(int index);

        @JSProperty(FILE_PROPERTY)
        int getFile();

        @JSProperty(FILE_PROPERTY)
        void setFile(int file);

        @JSProperty(DATA_PROPERTY)
        byte[] getData();

        @JSProperty(DATA_PROPERTY)
        void setData(byte[] data);

        public static IDBStoreEntry create(int index, int file) {
            IDBStoreEntry result = JSObjects.create();
            result.setIndex(index);
            result.setFile(file);
            result.setData(null);
            return result;
        }
    }
}
