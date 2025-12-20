package me.mdbell.awtea.impl.idb2;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;

/**
 * VirtualFile implementation for IndexedDB VFS v2.
 */
public class IndexedDBVirtualFile2 implements VirtualFile {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexedDBVirtualFile2.class);
    
    private final String path;
    
    public IndexedDBVirtualFile2(String path) {
        this.path = VFSPath.normalize(path);
    }
    
    @Override
    public String getName() {
        return VFSPath.getName(path);
    }
    
    @Override
    public boolean isDirectory() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        return entry != null && entry.isDirectory();
    }
    
    @Override
    public boolean isFile() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        return entry != null && entry.isFile();
    }
    
    @Override
    public String[] listFiles() {
        if (!isDirectory()) {
            return new String[0];
        }
        
        return IndexedDBHelper2.listDirectorySync(path);
    }
    
    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        IndexedDBVirtualFileAccessor2 accessor = new IndexedDBVirtualFileAccessor2(path, append);
        return new CachingVirtualFileAccessor2(accessor);
    }
    
    @Override
    public boolean createFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IOException("Invalid file name");
        }
        
        String childPath = VFSPath.resolve(path, fileName);
        
        // Check if already exists
        if (IndexedDBHelper2.exists(childPath)) {
            return false;
        }
        
        // Ensure parent exists
        if (!IndexedDBHelper2.exists(path)) {
            return false;
        }
        
        FileEntry parent = IndexedDBHelper2.getFileSync(path);
        if (parent == null || !parent.isDirectory()) {
            return false;
        }
        
        // Create new file entry
        FileEntry entry = FileEntry.createFile(childPath);
        return IndexedDBHelper2.putFileSync(entry);
    }
    
    @Override
    public boolean createDirectory(String dirName) {
        if (dirName == null || dirName.isEmpty()) {
            return false;
        }
        
        String childPath = VFSPath.resolve(path, dirName);
        childPath = VFSPath.normalize(childPath, true);
        
        // Check if already exists
        if (IndexedDBHelper2.exists(childPath)) {
            return false;
        }
        
        // Ensure parent exists
        if (!IndexedDBHelper2.exists(path)) {
            return false;
        }
        
        FileEntry parent = IndexedDBHelper2.getFileSync(path);
        if (parent == null || !parent.isDirectory()) {
            return false;
        }
        
        // Create new directory entry
        FileEntry entry = FileEntry.createDirectory(childPath);
        return IndexedDBHelper2.putFileSync(entry);
    }
    
    @Override
    public boolean delete() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry == null) {
            return false;
        }
        
        // Check if directory is empty
        if (entry.isDirectory()) {
            boolean isEmpty = IndexedDBHelper2.isDirectoryEmptySync(path);
            if (!isEmpty) {
                logger.warn("Cannot delete non-empty directory: {}", path);
                return false;
            }
        }
        
        return IndexedDBHelper2.deleteFileSync(path);
    }
    
    @Override
    public boolean adopt(VirtualFile file, String fileName) {
        if (file == null || fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        // Get source file
        String sourcePath = null;
        if (file instanceof IndexedDBVirtualFile2) {
            sourcePath = ((IndexedDBVirtualFile2) file).path;
        } else {
            // Can't adopt files from other VFS implementations
            return false;
        }
        
        FileEntry sourceEntry = IndexedDBHelper2.getFileSync(sourcePath);
        if (sourceEntry == null) {
            return false;
        }
        
        // Ensure target directory exists
        if (!isDirectory()) {
            return false;
        }
        
        // Create new path
        String targetPath = VFSPath.resolve(path, fileName);
        
        // Check if target already exists
        if (IndexedDBHelper2.exists(targetPath)) {
            return false;
        }
        
        // Update entry path and parent
        sourceEntry.setPath(targetPath);
        sourceEntry.setParent(path);
        sourceEntry.markModified();
        
        // Delete old entry and create new one
        IndexedDBHelper2.deleteFileSync(sourcePath);
        return IndexedDBHelper2.putFileSync(sourceEntry);
    }
    
    @Override
    public boolean canRead() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        return entry != null; // All files are readable
    }
    
    @Override
    public boolean canWrite() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        return entry != null && !entry.isReadonly();
    }
    
    @Override
    public long lastModified() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        return entry != null ? entry.getModified() : 0;
    }
    
    @Override
    public boolean setLastModified(long lastModified) {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry == null) {
            return false;
        }
        
        entry.setModified(lastModified);
        return IndexedDBHelper2.putFileSync(entry);
    }
    
    @Override
    public boolean setReadOnly(boolean readOnly) {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry == null) {
            return false;
        }
        
        entry.setReadonly(readOnly);
        return IndexedDBHelper2.putFileSync(entry);
    }
    
    @Override
    public int length() {
        FileEntry entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry == null || entry.isDirectory()) {
            return 0;
        }
        
        return (int) entry.getSize();
    }
}
