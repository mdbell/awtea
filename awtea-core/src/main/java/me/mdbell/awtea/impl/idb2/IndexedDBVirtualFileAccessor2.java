package me.mdbell.awtea.impl.idb2;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

/**
 * VirtualFileAccessor implementation using Blob storage for better random access performance.
 * Stores entire file as a single Blob, making reads/writes more efficient.
 */
public class IndexedDBVirtualFileAccessor2 implements VirtualFileAccessor {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexedDBVirtualFileAccessor2.class);
    
    private final String path;
    private final VFSStats stats;
    private int position = 0;
    private byte[] fileData = null;
    private boolean dirty = false;
    
    public IndexedDBVirtualFileAccessor2(String path, boolean append) {
        this.path = VFSPath.normalize(path, false);
        this.stats = IndexedDBHelper2.getStats();
        
        // Load existing file data
        loadFileData();
        
        if (append) {
            this.position = fileData != null ? fileData.length : 0;
        }
    }
    
    private void loadFileData() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry != null && entry.isFile()) {
            FileEntry.Blob blob = entry.getData();
            if (blob != null) {
                // Read blob data
                fileData = IndexedDBHelper2.readBlobDataSync(blob);
            } else {
                fileData = new byte[0];
            }
        } else {
            // New file
            fileData = new byte[0];
        }
    }
    
    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (fileData == null || position >= fileData.length) {
            return -1; // EOF
        }
        
        int available = fileData.length - position;
        int bytesToRead = Math.min(length, available);
        
        System.arraycopy(fileData, position, buffer, offset, bytesToRead);
        position += bytesToRead;
        
        stats.recordRead(bytesToRead);
        
        return bytesToRead;
    }
    
    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        ensureCapacity(position + length);
        
        System.arraycopy(buffer, offset, fileData, position, length);
        position += length;
        dirty = true;
        
        stats.recordWrite(length);
    }
    
    private void ensureCapacity(int requiredSize) {
        if (fileData == null) {
            fileData = new byte[requiredSize];
        } else if (fileData.length < requiredSize) {
            // Grow the array
            byte[] newData = new byte[requiredSize];
            System.arraycopy(fileData, 0, newData, 0, fileData.length);
            fileData = newData;
        }
    }
    
    @Override
    public int tell() throws IOException {
        return position;
    }
    
    @Override
    public void seek(int target) throws IOException {
        if (target < 0) {
            throw new IOException("Invalid seek position: " + target);
        }
        position = target;
    }
    
    @Override
    public void skip(int amount) throws IOException {
        int newPosition = position + amount;
        if (newPosition < 0) {
            throw new IOException("Skip would result in negative position");
        }
        position = newPosition;
    }
    
    @Override
    public int size() {
        return fileData != null ? fileData.length : 0;
    }
    
    @Override
    public void resize(int size) throws IOException {
        if (size < 0) {
            throw new IOException("Invalid file size: " + size);
        }
        
        if (size == 0) {
            fileData = new byte[0];
        } else {
            byte[] newData = new byte[size];
            if (fileData != null) {
                int copyLength = Math.min(fileData.length, size);
                System.arraycopy(fileData, 0, newData, 0, copyLength);
            }
            fileData = newData;
        }
        
        dirty = true;
        
        // Adjust position if needed
        if (position > size) {
            position = size;
        }
    }
    
    @Override
    public void flush() throws IOException {
        if (!dirty) {
            return;
        }
        
        // Load or create entry
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry == null) {
            // Create new file entry
            entry = FileEntry.createFile(path);
        }
        
        // Update entry metadata
        entry.markModified();
        entry.setSize(fileData != null ? fileData.length : 0);
        
        // Convert byte array to Blob
        if (fileData != null && fileData.length > 0) {
            FileEntry.Blob blob = IndexedDBHelper2.createBlobFromBytes(fileData);
            entry.setData(blob);
        } else {
            entry.setData(null);
        }
        
        // Store to IndexedDB
        IndexedDBHelper2.putFileSync(entry);
        
        dirty = false;
        
        logger.debug("Flushed file {} ({} bytes)", path, fileData != null ? fileData.length : 0);
    }
    
    @Override
    public void close() throws IOException {
        flush();
        fileData = null;
    }
}
