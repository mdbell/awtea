package me.mdbell.awtea.impl.idb2;

import java.io.IOException;

/**
 * Exception thrown when IndexedDB VFS v2 operations fail.
 */
public class IndexedDBVFSException2 extends IOException {
    
    public IndexedDBVFSException2(String message) {
        super(message);
    }
    
    public IndexedDBVFSException2(String message, Throwable cause) {
        super(message, cause);
    }
}
