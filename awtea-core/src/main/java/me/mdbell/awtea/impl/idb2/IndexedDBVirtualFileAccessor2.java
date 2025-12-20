package me.mdbell.awtea.impl.idb2;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * VirtualFileAccessor implementation using Blob storage with lazy loading for large files.
 * Uses chunk-based lazy loading to support files of any size without loading entire file into memory.
 * Modified chunks are kept in memory, unmodified chunks are read from Blob on demand.
 */
public class IndexedDBVirtualFileAccessor2 implements VirtualFileAccessor {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexedDBVirtualFileAccessor2.class);
    
    // Chunk size for lazy loading (64KB chunks)
    private static final int CHUNK_SIZE = 64 * 1024;
    
    private final String path;
    private final VFSStats stats;
    private int position = 0;
    
    // File metadata
    private FileEntry entry;
    private FileEntry.Blob originalBlob;
    private int fileSize;
    
    // Modified chunks map (chunk index -> chunk data)
    private final Map<Integer, byte[]> modifiedChunks = new HashMap<>();
    private boolean hasModifications = false;
    
    public IndexedDBVirtualFileAccessor2(String path, boolean append) {
        this.path = VFSPath.normalize(path, false);
        this.stats = IndexedDBHelper2.getStats();
        
        // Load file metadata
        loadFileMetadata();
        
        if (append) {
            this.position = fileSize;
        }
    }
    
    private void loadFileMetadata() {
        entry = IndexedDBHelper2.getFileSync(path);
        
        if (entry != null && entry.isFile()) {
            originalBlob = entry.getData();
            if (originalBlob != null) {
                fileSize = (int) originalBlob.getSize();
            } else {
                fileSize = 0;
            }
        } else {
            // New file
            fileSize = 0;
            originalBlob = null;
        }
    }
    
    /**
     * Get chunk data, either from modified chunks or by reading from original Blob
     */
    private byte[] getChunk(int chunkIndex) {
        // Check if chunk was modified
        if (modifiedChunks.containsKey(chunkIndex)) {
            return modifiedChunks.get(chunkIndex);
        }
        
        // Read from original Blob if available
        if (originalBlob != null) {
            int chunkStart = chunkIndex * CHUNK_SIZE;
            int chunkEnd = Math.min(chunkStart + CHUNK_SIZE, fileSize);
            
            if (chunkStart < fileSize) {
                return IndexedDBHelper2.readBlobSliceSync(originalBlob, chunkStart, chunkEnd);
            }
        }
        
        // Return empty chunk for new data beyond file size
        return new byte[0];
    }
    
    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (position >= fileSize) {
            return -1; // EOF
        }
        
        int available = fileSize - position;
        int bytesToRead = Math.min(length, available);
        int totalRead = 0;
        
        while (totalRead < bytesToRead) {
            int chunkIndex = position / CHUNK_SIZE;
            int chunkOffset = position % CHUNK_SIZE;
            
            byte[] chunk = getChunk(chunkIndex);
            int chunkAvailable = chunk.length - chunkOffset;
            int readFromChunk = Math.min(bytesToRead - totalRead, chunkAvailable);
            
            if (readFromChunk > 0) {
                System.arraycopy(chunk, chunkOffset, buffer, offset + totalRead, readFromChunk);
                position += readFromChunk;
                totalRead += readFromChunk;
            } else {
                // No more data in this chunk
                break;
            }
        }
        
        stats.recordRead(totalRead);
        return totalRead;
    }
    
    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        int writeEnd = position + length;
        
        // Extend file size if writing beyond current end
        if (writeEnd > fileSize) {
            fileSize = writeEnd;
        }
        
        int written = 0;
        while (written < length) {
            int chunkIndex = position / CHUNK_SIZE;
            int chunkOffset = position % CHUNK_SIZE;
            
            // Get or create chunk
            byte[] chunk = modifiedChunks.get(chunkIndex);
            if (chunk == null) {
                // Load original chunk data or create new chunk
                chunk = getChunk(chunkIndex);
                if (chunk.length < CHUNK_SIZE) {
                    // Expand chunk to full size
                    byte[] expanded = new byte[CHUNK_SIZE];
                    System.arraycopy(chunk, 0, expanded, 0, chunk.length);
                    chunk = expanded;
                }
                modifiedChunks.put(chunkIndex, chunk);
            }
            
            int writeToChunk = Math.min(length - written, CHUNK_SIZE - chunkOffset);
            System.arraycopy(buffer, offset + written, chunk, chunkOffset, writeToChunk);
            
            position += writeToChunk;
            written += writeToChunk;
        }
        
        hasModifications = true;
        stats.recordWrite(length);
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
        return fileSize;
    }
    
    @Override
    public void resize(int size) throws IOException {
        if (size < 0) {
            throw new IOException("Invalid file size: " + size);
        }
        
        if (size < fileSize) {
            // Truncate: remove chunks beyond new size
            int lastChunkIndex = (size + CHUNK_SIZE - 1) / CHUNK_SIZE;
            modifiedChunks.keySet().removeIf(idx -> idx >= lastChunkIndex);
            
            // Truncate the last chunk if needed
            int lastChunkSize = size % CHUNK_SIZE;
            if (lastChunkSize > 0) {
                int lastIdx = size / CHUNK_SIZE;
                byte[] chunk = modifiedChunks.get(lastIdx);
                if (chunk != null && chunk.length > lastChunkSize) {
                    byte[] truncated = new byte[lastChunkSize];
                    System.arraycopy(chunk, 0, truncated, 0, lastChunkSize);
                    modifiedChunks.put(lastIdx, truncated);
                }
            }
        }
        // If expanding, no need to do anything - writes will handle it
        
        fileSize = size;
        hasModifications = true;
        
        // Adjust position if needed
        if (position > size) {
            position = size;
        }
    }
    
    @Override
    public void flush() throws IOException {
        if (!hasModifications) {
            return;
        }
        
        // Load or create entry
        if (entry == null) {
            entry = FileEntry.createFile(path);
        }
        
        // Update entry metadata
        entry.markModified();
        entry.setSize(fileSize);
        
        // Reconstruct full file data from chunks
        if (fileSize > 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
            
            int numChunks = (fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE;
            for (int i = 0; i < numChunks; i++) {
                byte[] chunk = getChunk(i);
                
                // Calculate how much of this chunk to write
                int chunkStart = i * CHUNK_SIZE;
                int chunkSize = Math.min(CHUNK_SIZE, fileSize - chunkStart);
                
                baos.write(chunk, 0, Math.min(chunk.length, chunkSize));
            }
            
            byte[] fileData = baos.toByteArray();
            FileEntry.Blob blob = IndexedDBHelper2.createBlobFromBytes(fileData);
            entry.setData(blob);
        } else {
            entry.setData(null);
        }
        
        // Store to IndexedDB
        IndexedDBHelper2.putFileSync(entry);
        
        // Update state
        originalBlob = entry.getData();
        modifiedChunks.clear();
        hasModifications = false;
        
        logger.debug("Flushed file {} ({} bytes)", path, fileSize);
    }
    
    @Override
    public void close() throws IOException {
        flush();
        modifiedChunks.clear();
        originalBlob = null;
        entry = null;
    }
}
