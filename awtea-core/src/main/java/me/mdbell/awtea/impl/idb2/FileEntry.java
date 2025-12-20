package me.mdbell.awtea.impl.idb2;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBuffer;

/**
 * JSObject interface representing a file entry in IndexedDB.
 * Stores file metadata and data as a Blob.
 */
@JSClass
public interface FileEntry extends JSObject {
    
    @JSProperty
    String getPath();
    
    @JSProperty
    void setPath(String path);
    
    @JSProperty
    String getType();
    
    @JSProperty
    void setType(String type);
    
    @JSProperty
    String getParent();
    
    @JSProperty
    void setParent(String parent);
    
    @JSProperty
    long getSize();
    
    @JSProperty
    void setSize(long size);
    
    @JSProperty
    long getCreated();
    
    @JSProperty
    void setCreated(long created);
    
    @JSProperty
    long getModified();
    
    @JSProperty
    void setModified(long modified);
    
    @JSProperty
    long getAccessed();
    
    @JSProperty
    void setAccessed(long accessed);
    
    @JSProperty("readonly")
    boolean isReadonly();
    
    @JSProperty("readonly")
    void setReadonly(boolean readonly);
    
    @JSProperty
    Blob getData();
    
    @JSProperty
    void setData(Blob data);
    
    /**
     * Check if this entry is a directory
     */
    default boolean isDirectory() {
        return "dir".equals(getType());
    }
    
    /**
     * Check if this entry is a file
     */
    default boolean isFile() {
        return "file".equals(getType());
    }
    
    /**
     * Update the accessed timestamp
     */
    default void touch() {
        setAccessed(System.currentTimeMillis());
    }
    
    /**
     * Update the modified timestamp
     */
    default void markModified() {
        long now = System.currentTimeMillis();
        setModified(now);
        setAccessed(now);
    }
    
    /**
     * Create a new file entry
     * 
     * @param path the file path
     * @param type "file" or "dir"
     * @return new FileEntry
     */
    static FileEntry create(String path, String type) {
        FileEntry entry = JSObjects.create();
        entry.setPath(path);
        entry.setType(type);
        long now = System.currentTimeMillis();
        entry.setCreated(now);
        entry.setModified(now);
        entry.setAccessed(now);
        entry.setReadonly(false);
        entry.setSize(0);
        
        // Determine parent path
        String parent = VFSPath.getParent(path);
        entry.setParent(parent);
        
        return entry;
    }
    
    /**
     * Create a new file entry
     */
    static FileEntry createFile(String path) {
        return create(path, "file");
    }
    
    /**
     * Create a new directory entry
     */
    static FileEntry createDirectory(String path) {
        return create(path, "dir");
    }
    
    /**
     * Represents a Blob object in JavaScript
     */
    @JSClass
    interface Blob extends JSObject {
        @JSProperty
        long getSize();
        
        @JSProperty
        String getType();
        
        /**
         * Get the blob data as an ArrayBuffer
         * @return promise resolving to ArrayBuffer
         */
        org.teavm.jso.core.JSPromise<ArrayBuffer> arrayBuffer();
    }
}
