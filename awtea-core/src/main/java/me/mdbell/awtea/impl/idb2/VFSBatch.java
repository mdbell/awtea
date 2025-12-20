package me.mdbell.awtea.impl.idb2;

import org.teavm.jso.core.JSPromise;
import org.teavm.jso.indexeddb.IDBObjectStore;
import org.teavm.jso.indexeddb.IDBRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for batch operations on the VFS.
 * Allows multiple operations to be queued and executed in a single transaction.
 */
public class VFSBatch {
    
    private final List<BatchOperation> operations = new ArrayList<>();
    
    /**
     * Add a file entry to be stored
     */
    public VFSBatch put(FileEntry entry) {
        operations.add(new PutOperation(entry));
        return this;
    }
    
    /**
     * Add a file path to be deleted
     */
    public VFSBatch delete(String path) {
        operations.add(new DeleteOperation(path));
        return this;
    }
    
    /**
     * Execute all queued operations in a single transaction
     * 
     * @param store the object store to use
     * @return promise that resolves when all operations complete
     */
    public JSPromise<Boolean> execute(IDBObjectStore store) {
        if (operations.isEmpty()) {
            return JSPromise.resolve(true);
        }
        
        return new JSPromise<>((resolve, reject) -> {
            executeOperation(store, 0, resolve, reject);
        });
    }
    
    private void executeOperation(IDBObjectStore store, int index,
                                  Object resolve, Object reject) {
        if (index >= operations.size()) {
            // Use reflection-like approach to call accept
            callResolve(resolve, true);
            return;
        }
        
        BatchOperation op = operations.get(index);
        IDBRequest request = op.execute(store);
        
        request.setOnSuccess(() -> {
            executeOperation(store, index + 1, resolve, reject);
        });
        
        request.setOnError(() -> {
            callReject(reject, new IndexedDBVFSException2("Batch operation failed: " + request.getError()));
        });
    }
    
    @org.teavm.jso.JSBody(params = {"callback", "value"}, script = "callback(value);")
    private static native void callResolve(Object callback, boolean value);
    
    @org.teavm.jso.JSBody(params = {"callback", "error"}, script = "callback(error);")
    private static native void callReject(Object callback, Throwable error);
    
    /**
     * Get the number of queued operations
     */
    public int size() {
        return operations.size();
    }
    
    /**
     * Clear all queued operations
     */
    public void clear() {
        operations.clear();
    }
    
    private interface BatchOperation {
        IDBRequest execute(IDBObjectStore store);
    }
    
    private static class PutOperation implements BatchOperation {
        private final FileEntry entry;
        
        PutOperation(FileEntry entry) {
            this.entry = entry;
        }
        
        @Override
        public IDBRequest execute(IDBObjectStore store) {
            return store.put(entry);
        }
    }
    
    private static class DeleteOperation implements BatchOperation {
        private final String path;
        
        DeleteOperation(String path) {
            this.path = path;
        }
        
        @Override
        public IDBRequest execute(IDBObjectStore store) {
            return store.delete(org.teavm.jso.core.JSString.valueOf(path));
        }
    }
}
