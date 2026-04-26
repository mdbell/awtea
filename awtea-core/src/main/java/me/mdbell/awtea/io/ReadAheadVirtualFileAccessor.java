package me.mdbell.awtea.io;

import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReadAheadVirtualFileAccessor implements VirtualFileAccessor {

    private static final int MAX_WRITE_GAP_BYTES = 1024;
    private static final byte[] ZERO_PADDING = new byte[1024];

    private final VirtualFileAccessor delegate;
    private final int readAheadSize;
    private final int writeBufferSize;

    private int position = -1;
    private int cachedSize = -1;

    private byte[] readCache;
    private int readCacheStart = -1;
    private int readCacheEnd = -1;

    private final ExposedByteArrayOutputStream writeBuffer;
    private int writeBufferStart = -1;

    public ReadAheadVirtualFileAccessor(VirtualFileAccessor delegate, int readAheadSize, int writeBufferSize) {
        this.delegate = delegate;
        this.readAheadSize = Math.max(1024, readAheadSize);
        this.writeBufferSize = Math.max(0, writeBufferSize);
        this.writeBuffer = new ExposedByteArrayOutputStream(Math.max(1024, this.writeBufferSize));
    }

    private void ensurePositionLoaded() throws IOException {
        if (position == -1) {
            position = delegate.tell();
        }
    }

    private void invalidateReadCache() {
        readCache = null;
        readCacheStart = -1;
        readCacheEnd = -1;
    }

    private boolean inReadCache(int pos) {
        return readCache != null && pos >= readCacheStart && pos < readCacheEnd;
    }

    private void flushWriteBuffer() throws IOException {
        int buffered = writeBuffer.size();
        if (buffered == 0) {
            return;
        }

        int start = writeBufferStart;
        delegate.seek(start);
        delegate.write(writeBuffer.buffer(), 0, buffered);
        writeBuffer.reset();
        writeBufferStart = -1;

        if (cachedSize != -1) {
            cachedSize = Math.max(cachedSize, start + buffered);
        }
    }

    private int currentSizeEstimate() throws IOException {
        if (cachedSize == -1) {
            cachedSize = delegate.size();
        }
        if (writeBufferStart != -1) {
            cachedSize = Math.max(cachedSize, writeBufferStart + writeBuffer.size());
        }
        return cachedSize;
    }

    private void recordPotentialGrowth(int endPositionExclusive) {
        if (cachedSize != -1) {
            cachedSize = Math.max(cachedSize, endPositionExclusive);
        }
    }

    private void fillReadCache(int start, int minBytes) throws IOException {
        int target = Math.max(readAheadSize, minBytes);
        if (writeBufferStart != -1 && start < writeBufferStart) {
            target = Math.min(target, writeBufferStart - start);
        }
        if (target <= 0) {
            readCacheStart = start;
            readCacheEnd = start;
            return;
        }
        if (readCache == null || readCache.length < target) {
            readCache = new byte[target];
        }

        delegate.seek(start);
        int actual = delegate.read(readCache, 0, target);
        if (actual < 0) {
            actual = 0;
        }
        readCacheStart = start;
        readCacheEnd = start + actual;
    }

    private int readFromPendingWriteBuffer(byte[] out, int outOffset, int requested) {
        int buffered = writeBuffer.size();
        if (writeBufferStart == -1 || buffered == 0) {
            return 0;
        }
        int writeEnd = writeBufferStart + buffered;
        if (position < writeBufferStart || position >= writeEnd) {
            return 0;
        }

        int available = writeEnd - position;
        int toCopy = Math.min(requested, available);
        int sourceOffset = position - writeBufferStart;
        System.arraycopy(writeBuffer.buffer(), sourceOffset, out, outOffset, toCopy);
        return toCopy;
    }

    private void appendZeroPadding(int bytes) {
        int remaining = bytes;
        while (remaining > 0) {
            int chunk = Math.min(remaining, ZERO_PADDING.length);
            writeBuffer.write(ZERO_PADDING, 0, chunk);
            remaining -= chunk;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int limit) throws IOException {
        if (limit <= 0) {
            return 0;
        }

        ensurePositionLoaded();

        int fileSize = currentSizeEstimate();
        if (position >= fileSize) {
            return -1;
        }

        int remaining = Math.min(limit, fileSize - position);
        int totalRead = 0;

        while (remaining > 0) {
            int fromWriteBuffer = readFromPendingWriteBuffer(buffer, offset + totalRead, remaining);
            if (fromWriteBuffer > 0) {
                position += fromWriteBuffer;
                totalRead += fromWriteBuffer;
                remaining -= fromWriteBuffer;
                continue;
            }

            if (!inReadCache(position)) {
                fillReadCache(position, remaining);
                if (!inReadCache(position)) {
                    break;
                }
            }

            int fromCache = Math.min(remaining, readCacheEnd - position);
            int cacheOffset = position - readCacheStart;
            System.arraycopy(readCache, cacheOffset, buffer, offset + totalRead, fromCache);

            position += fromCache;
            totalRead += fromCache;
            remaining -= fromCache;
        }

        return totalRead > 0 ? totalRead : -1;
    }

    @Override
    public void write(byte[] buffer, int offset, int limit) throws IOException {
        if (limit <= 0) {
            return;
        }

        ensurePositionLoaded();
        invalidateReadCache();

        if (writeBufferSize == 0) {
            delegate.seek(position);
            delegate.write(buffer, offset, limit);
            position += limit;
            recordPotentialGrowth(position);
            return;
        }

        int expectedEnd = writeBufferStart == -1 ? -1 : writeBufferStart + writeBuffer.size();
        if (writeBufferStart == -1) {
            writeBufferStart = position;
        } else if (position != expectedEnd) {
            int forwardGap = position - expectedEnd;
            boolean canMergeForwardGap = forwardGap > 0
                    && forwardGap <= MAX_WRITE_GAP_BYTES
                    && writeBuffer.size() + forwardGap + limit <= writeBufferSize;
            if (canMergeForwardGap) {
                appendZeroPadding(forwardGap);
            } else {
                flushWriteBuffer();
                writeBufferStart = position;
            }
        }

        if (writeBuffer.size() == 0 && limit >= writeBufferSize) {
            delegate.seek(position);
            delegate.write(buffer, offset, limit);
            position += limit;
            recordPotentialGrowth(position);
            return;
        }

        if (writeBuffer.size() + limit > writeBufferSize) {
            flushWriteBuffer();
            writeBufferStart = position;
        }

        writeBuffer.write(buffer, offset, limit);
        position += limit;
        recordPotentialGrowth(position);
    }

    @Override
    public int tell() throws IOException {
        ensurePositionLoaded();
        return position;
    }

    @Override
    public void seek(int target) throws IOException {
        ensurePositionLoaded();
        if (position == target) {
            return;
        }
        flushWriteBuffer();
        position = target;
        if (!inReadCache(target)) {
            invalidateReadCache();
        }
    }

    @Override
    public void skip(int amount) throws IOException {
        if (amount == 0) {
            return;
        }
        seek(tell() + amount);
    }

    @Override
    public int size() throws IOException {
        return currentSizeEstimate();
    }

    @Override
    public void resize(int size) throws IOException {
        flushWriteBuffer();
        invalidateReadCache();
        delegate.resize(size);
        cachedSize = size;
        ensurePositionLoaded();
        if (position > size) {
            position = size;
        }
    }

    @Override
    public void close() throws IOException {
        flush();
        delegate.close();
    }

    @Override
    public void flush() throws IOException {
        flushWriteBuffer();
        delegate.flush();
    }

    private static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        private ExposedByteArrayOutputStream(int size) {
            super(size);
        }

        private byte[] buffer() {
            return buf;
        }
    }
}
