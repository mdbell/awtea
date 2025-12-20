package me.mdbell.awtea.impl.idb2;

import lombok.experimental.ExtensionMethod;
import lombok.experimental.UtilityClass;
import me.mdbell.awtea.util.IDBUtils;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.*;
import org.teavm.jso.indexeddb.*;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8Array;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Core IndexedDB operations for VFS v2 using JSPromise and Blob storage.
 */
@UtilityClass
@ExtensionMethod({JSObjectsExtensions.class})
public class IndexedDBHelper2 {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexedDBHelper2.class);
    
    public static final String DB_NAME = "vfs_v3";
    public static final String STORE_NAME = "files";
    public static final String PARENT_INDEX = "parent_index";
    public static final int DB_VERSION = 3;
    
    private static final IDBDatabase db = IDBUtils.open(DB_NAME, DB_VERSION, IndexedDBHelper2::upgrade);
    private static final VFSStats stats = new VFSStats();
    
    private static void upgrade(IDBVersionChangeEvent evt) {
        logger.info("Upgrading IndexedDB from version {} to {}", evt.getOldVersion(), evt.getNewVersion());
        
        IDBDatabase db = ((IDBOpenDBRequest) evt.getTarget()).getResult();
        
        // Delete old stores if upgrading
        String[] storeNames = db.getObjectStoreNames();
        for (String storeName : storeNames) {
            if (STORE_NAME.equals(storeName)) {
                db.deleteObjectStore(storeName);
            }
        }
        
        // Create the files store
        IDBObjectStoreParameters params = JSObjects.create();
        params.keyPath("path");
        IDBObjectStore store = db.createObjectStore(STORE_NAME, params);
        
        // Create index on parent field for directory listings
        IDBCreateIndexOptions indexOptions = JSObjects.create();
        indexOptions.setUnique(false);
        createIndexWithOptions(store, PARENT_INDEX, "parent", indexOptions);
        
        logger.info("IndexedDB upgrade complete");
    }
    
    @JSBody(params = {"store", "name", "keyPath", "options"}, script = "store.createIndex(name, keyPath, options);")
    private static native void createIndexWithOptions(IDBObjectStore store, String name, String keyPath, IDBCreateIndexOptions options);
    
    /**
     * Get a file entry by path (async)
     * 
     * @param path the file path
     * @return promise resolving to FileEntry or null
     */
    public static JSPromise<FileEntry> getFile(String path) {
        stats.recordDbOperation();
        path = VFSPath.normalize(path);
        
        String finalPath = path;
        return new JSPromise<>((resolve, reject) -> {
            IDBTransaction tx = db.transaction(STORE_NAME, "readonly");
            IDBObjectStore store = tx.objectStore(STORE_NAME);
            IDBGetRequest req = store.get(JSString.valueOf(finalPath));
            
            req.setOnSuccess(() -> {
                FileEntry entry = (FileEntry) req.getResult();
                resolve.accept(entry.nullish() ? null : entry);
            });
            
            req.setOnError(() -> {
                stats.recordError();
                logger.error("Failed to get file {}: {}", finalPath, req.getError());
                reject.accept(new IndexedDBVFSException2("Failed to get file: " + req.getError()));
            });
        });
    }
    
    /**
     * Get a file entry by path (sync via await)
     */
    public static FileEntry getFileSync(String path) {
        return getFile(path).await();
    }
    
    /**
     * Put a file entry (async)
     * 
     * @param entry the file entry to store
     * @return promise resolving to true on success
     */
    public static JSPromise<Boolean> putFile(FileEntry entry) {
        stats.recordDbOperation();
        
        return new JSPromise<>((resolve, reject) -> {
            IDBTransaction tx = db.transaction(STORE_NAME, "readwrite");
            IDBObjectStore store = tx.objectStore(STORE_NAME);
            IDBRequest req = store.put(entry);
            
            req.setOnSuccess(() -> {
                resolve.accept(true);
            });
            
            req.setOnError(() -> {
                stats.recordError();
                logger.error("Failed to put file {}: {}", entry.getPath(), req.getError());
                reject.accept(new IndexedDBVFSException2("Failed to put file: " + req.getError()));
            });
        });
    }
    
    /**
     * Put a file entry (sync via await)
     */
    public static boolean putFileSync(FileEntry entry) {
        return putFile(entry).await();
    }
    
    /**
     * Delete a file entry (async)
     * 
     * @param path the file path
     * @return promise resolving to true on success
     */
    public static JSPromise<Boolean> deleteFile(String path) {
        stats.recordDbOperation();
        path = VFSPath.normalize(path);
        
        String finalPath = path;
        return new JSPromise<>((resolve, reject) -> {
            IDBTransaction tx = db.transaction(STORE_NAME, "readwrite");
            IDBObjectStore store = tx.objectStore(STORE_NAME);
            IDBRequest req = store.delete(JSString.valueOf(finalPath));
            
            req.setOnSuccess(() -> {
                resolve.accept(true);
            });
            
            req.setOnError(() -> {
                stats.recordError();
                logger.error("Failed to delete file {}: {}", finalPath, req.getError());
                reject.accept(new IndexedDBVFSException2("Failed to delete file: " + req.getError()));
            });
        });
    }
    
    /**
     * Delete a file entry (sync via await)
     */
    public static boolean deleteFileSync(String path) {
        return deleteFile(path).await();
    }
    
    /**
     * List all files in a directory (async)
     * 
     * @param path the directory path
     * @return promise resolving to array of file names
     */
    public static JSPromise<String[]> listDirectory(String path) {
        stats.recordDbOperation();
        path = VFSPath.normalize(path, true);
        
        String finalPath = path;
        return new JSPromise<>((resolve, reject) -> {
            IDBTransaction tx = db.transaction(STORE_NAME, "readonly");
            IDBObjectStore store = tx.objectStore(STORE_NAME);
            IDBIndex index = store.index(PARENT_INDEX);
            IDBKeyRange range = IDBKeyRange.only(JSString.valueOf(finalPath));
            IDBCursorRequest req = index.openCursor(range);
            
            List<String> results = new ArrayList<>();
            
            req.setOnSuccess(() -> {
                IDBCursor cursor = req.getResult();
                if (cursor.nullish()) {
                    // Done
                    String[] arr = results.toArray(new String[0]);
                    resolve.accept(arr);
                    return;
                }
                
                FileEntry entry = (FileEntry) cursor.getValue();
                String entryPath = entry.getPath();
                
                // Extract just the name part (relative to parent)
                String name = entryPath.substring(finalPath.length());
                results.add(name);
                
                cursor.doContinue();
            });
            
            req.setOnError(() -> {
                stats.recordError();
                logger.error("Failed to list directory {}: {}", finalPath, req.getError());
                reject.accept(new IndexedDBVFSException2("Failed to list directory: " + req.getError()));
            });
        });
    }
    
    /**
     * List directory (sync via await)
     */
    public static String[] listDirectorySync(String path) {
        return listDirectory(path).await();
    }
    
    /**
     * Check if a directory is empty (async)
     */
    public static JSPromise<Boolean> isDirectoryEmpty(String path) {
        stats.recordDbOperation();
        path = VFSPath.normalize(path, true);
        
        String finalPath = path;
        return new JSPromise<>((resolve, reject) -> {
            IDBTransaction tx = db.transaction(STORE_NAME, "readonly");
            IDBObjectStore store = tx.objectStore(STORE_NAME);
            IDBIndex index = store.index(PARENT_INDEX);
            IDBKeyRange range = IDBKeyRange.only(JSString.valueOf(finalPath));
            IDBCursorRequest req = index.openCursor(range);
            
            req.setOnSuccess(() -> {
                IDBCursor cursor = req.getResult();
                // Empty if cursor is null (no children)
                boolean empty = cursor.nullish();
                resolve.accept(empty);
            });
            
            req.setOnError(() -> {
                stats.recordError();
                logger.error("Failed to check if directory empty {}: {}", finalPath, req.getError());
                reject.accept(new IndexedDBVFSException2("Failed to check directory: " + req.getError()));
            });
        });
    }
    
    /**
     * Check if directory is empty (sync via await)
     */
    public static boolean isDirectoryEmptySync(String path) {
        return isDirectoryEmpty(path).await();
    }
    
    /**
     * Check if a path exists
     */
    public static boolean exists(String path) {
        FileEntry entry = getFileSync(path);
        return entry != null;
    }
    
    /**
     * Create a Blob from byte array
     */
    @JSBody(params = {"data"}, script = "return new Blob([data]);")
    public static native FileEntry.Blob createBlob(Uint8Array data);
    
    /**
     * Create a Blob from byte array
     */
    public static FileEntry.Blob createBlobFromBytes(byte[] data) {
        Uint8Array arr = Uint8Array.create(data.length);
        arr.set(data);
        return createBlob(arr);
    }
    
    /**
     * Read blob data into byte array (async)
     */
    public static JSPromise<byte[]> readBlobData(FileEntry.Blob blob) {
        return blob.arrayBuffer().then(buffer -> {
            return convertArrayBufferToBytes(buffer);
        });
    }
    
    /**
     * Convert ArrayBuffer to byte array using JSByRef
     */
    @org.teavm.jso.JSByRef
    @JSBody(params = {"buffer"}, script = "return new Uint8Array(buffer);")
    private static native byte[] convertArrayBufferToBytes(ArrayBuffer buffer);
    
    /**
     * Read blob data into byte array (sync via await)
     */
    public static byte[] readBlobDataSync(FileEntry.Blob blob) {
        return readBlobData(blob).await();
    }
    
    /**
     * Get VFS statistics
     */
    public static VFSStats getStats() {
        return stats;
    }
    
    /**
     * Helper interface for IndexedDB index options
     */
    private interface IDBCreateIndexOptions extends org.teavm.jso.JSObject {
        @org.teavm.jso.JSProperty
        void setUnique(boolean unique);
    }
}
