package me.mdbell.awtea.impl.idb2;

import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Caching wrapper for VirtualFileAccessor with adaptive random/sequential access optimization.
 * Provides write buffering and read caching for better performance.
 */
public class CachingVirtualFileAccessor2 implements VirtualFileAccessor {
    
    private static final int WRITE_BUFFER_SIZE = 32 * 1024; // 32KB write buffer
    private static final int READ_CACHE_SIZE = 32 * 1024;   // 32KB read cache
    
    private final VirtualFileAccessor delegate;
    private final VFSStats stats;
    
    // Write cache
    private final ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream(WRITE_BUFFER_SIZE);
    private int writeBufferStart = -1;
    
    // Read cache
    private byte[] readCache = null;
    private int cacheStart = -1;
    private int cacheEnd = -1;
    
    // Metadata cache
    private Integer cachedSize = null;
    private Integer cachedPosition = null;
    
    public CachingVirtualFileAccessor2(VirtualFileAccessor delegate) {
        this.delegate = delegate;
        this.stats = IndexedDBHelper2.getStats();
    }
    
    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        // Ensure buffered writes are flushed before reading
        flushWriteBuffer();
        
        int position = tell();
        int fileSize = size();
        
        if (position >= fileSize) {
            return -1; // EOF
        }
        
        int bytesToRead = Math.min(length, fileSize - position);
        
        // Check for full cache hit
        if (readCache != null && position >= cacheStart && (position + bytesToRead) <= cacheEnd) {
            int cacheOffset = position - cacheStart;
            System.arraycopy(readCache, cacheOffset, buffer, offset, bytesToRead);
            cachedPosition = position + bytesToRead;
            stats.recordCacheHit();
            return bytesToRead;
        }
        
        // Check for partial cache hit
        int totalRead = 0;
        if (readCache != null && position >= cacheStart && position < cacheEnd) {
            int cacheOffset = position - cacheStart;
            int cacheLength = cacheEnd - position;
            int partialRead = Math.min(cacheLength, bytesToRead);
            
            System.arraycopy(readCache, cacheOffset, buffer, offset, partialRead);
            
            position += partialRead;
            bytesToRead -= partialRead;
            totalRead += partialRead;
            offset += partialRead;
            
            stats.recordCacheHit();
        }
        
        if (bytesToRead > 0) {
            stats.recordCacheMiss();
            
            // Position delegate to current position
            delegate.seek(position);
            
            // Read into cache
            readCache = new byte[READ_CACHE_SIZE];
            int actualRead = delegate.read(readCache, 0, READ_CACHE_SIZE);
            
            if (actualRead <= 0) {
                return totalRead > 0 ? totalRead : -1;
            }
            
            cacheStart = position;
            cacheEnd = position + actualRead;
            
            // Copy requested portion
            int bytesToCopy = Math.min(bytesToRead, actualRead);
            System.arraycopy(readCache, 0, buffer, offset, bytesToCopy);
            
            totalRead += bytesToCopy;
            position += bytesToCopy;
        }
        
        cachedPosition = position;
        return totalRead;
    }
    
    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        int position = tell();
        
        if (writeBufferStart == -1) {
            writeBufferStart = position;
        }
        
        // Append to write buffer
        writeBuffer.write(buffer, offset, length);
        cachedPosition = position + length;
        
        // Invalidate read cache
        readCache = null;
        cachedSize = null;
        
        // Flush if buffer is full
        if (writeBuffer.size() >= WRITE_BUFFER_SIZE) {
            flushWriteBuffer();
        }
    }
    
    @Override
    public int tell() throws IOException {
        if (cachedPosition != null) {
            return cachedPosition;
        }
        cachedPosition = delegate.tell();
        return cachedPosition;
    }
    
    @Override
    public void seek(int target) throws IOException {
        if (cachedPosition != null && cachedPosition == target) {
            return; // Already at target position
        }
        
        flushWriteBuffer();
        delegate.seek(target);
        cachedPosition = target;
    }
    
    @Override
    public void skip(int amount) throws IOException {
        if (amount == 0) {
            return;
        }
        
        flushWriteBuffer();
        delegate.skip(amount);
        cachedPosition = delegate.tell();
    }
    
    @Override
    public int size() throws IOException {
        if (cachedSize != null) {
            return cachedSize;
        }
        cachedSize = delegate.size();
        return cachedSize;
    }
    
    @Override
    public void resize(int size) throws IOException {
        flushWriteBuffer();
        delegate.resize(size);
        
        // Invalidate caches
        cachedSize = null;
        cachedPosition = null;
        readCache = null;
    }
    
    @Override
    public void flush() throws IOException {
        flushWriteBuffer();
        delegate.flush();
    }
    
    @Override
    public void close() throws IOException {
        flushWriteBuffer();
        delegate.close();
        
        // Clear caches
        readCache = null;
        cachedSize = null;
        cachedPosition = null;
    }
    
    private void flushWriteBuffer() throws IOException {
        if (writeBuffer.size() == 0) {
            return;
        }
        
        // Position delegate to write start
        delegate.seek(writeBufferStart);
        
        // Write buffered data
        byte[] data = writeBuffer.toByteArray();
        delegate.write(data, 0, data.length);
        
        // Reset write buffer
        writeBuffer.reset();
        writeBufferStart = -1;
        
        // Invalidate caches
        cachedSize = null;
        readCache = null;
    }
}
