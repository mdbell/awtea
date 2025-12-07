package me.mdbell.awtea.impl.idb;

import me.mdbell.awtea.util.ThreadUtils;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CachingVirtualFileAccessor implements VirtualFileAccessor {

	private final VirtualFileAccessor accessor;

	private int position = -1;

	// Write cache
	private static final int WRITE_BUFFER_SIZE = IndexedDBHelper.CHUNK_SIZE * 5;
	private final ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream(WRITE_BUFFER_SIZE);

	private int bufferStart = -1; // Position of the first buffered write

	// Read cache
	private static final int READ_CACHE_SIZE = IndexedDBHelper.CHUNK_SIZE * 5; // Read cache size
	private byte[] readCache = null;
	private int cacheStart = -1, cacheEnd = -1; // Read cache bounds

	// Metadata cache
	private int cachedSize = -1;

	// Flush timer
	private long nextFlush = -1;

	private static final List<CachingVirtualFileAccessor> instances = new ArrayList<>();

	static {
		ThreadUtils.runAtFixedRate(new CacheFlusher(), 500);
	}

	public CachingVirtualFileAccessor(VirtualFileAccessor accessor) {
		this.accessor = accessor;
		instances.add(this);
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		flush(); // Ensure buffered writes are committed before reading

		int position = tell();
		int fileSize = size();

		if (position >= fileSize) {
			return -1; // EOF
		}

		// Adjust read length if exceeding file bounds
		int remaining = fileSize - position;
		int bytesToRead = Math.min(length, remaining);

		int totalRead = 0;

		// Check if data is already in cache
		if (readCache != null && position >= cacheStart && (position + bytesToRead) <= cacheEnd) {
			// Fully cacheable read
			int cacheOffset = position - cacheStart;
			System.arraycopy(readCache, cacheOffset, buffer, offset, bytesToRead);
			this.position = position + bytesToRead;
			return bytesToRead;
		}

		// Handle a **partial** cache hit
		if (readCache != null && position < cacheEnd && (position + bytesToRead) > cacheEnd) {
			int cacheOffset = position - cacheStart;
			int cacheLength = cacheEnd - position;
			System.arraycopy(readCache, cacheOffset, buffer, offset, cacheLength);

			position += cacheLength;
			bytesToRead -= cacheLength;
			totalRead += cacheLength;
			offset += cacheLength;
		}

		// Load new data into cache
		readCache = new byte[READ_CACHE_SIZE];
		int actualRead = accessor.read(readCache, 0, READ_CACHE_SIZE);
		if (actualRead <= 0) {
			return (totalRead > 0) ? totalRead : -1; // Return bytes read so far, or EOF
		}

		cacheStart = position;
		cacheEnd = position + actualRead;

		// Copy the requested portion from the cache
		int bytesToCopy = Math.min(bytesToRead, actualRead);
		System.arraycopy(readCache, 0, buffer, offset, bytesToCopy);

		totalRead += bytesToCopy;

		this.position = position + bytesToCopy;

		return totalRead;
	}


	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException {
		if (bufferStart == -1) {
			bufferStart = position; // Mark buffer start on first write
			nextFlush = System.currentTimeMillis() + 5000; // 10 second after the first write
		}

		// Append data to buffer
		writeBuffer.write(buffer, offset, length);
		position += length;

		// Flush if buffer exceeds threshold
		if (writeBuffer.size() >= WRITE_BUFFER_SIZE) {
			flush();
		}
	}

	@Override
	public int tell() throws IOException {
		if (position == -1) {
			position = accessor.tell();
		}
		return position;
	}

	@Override
	public void seek(int target) throws IOException {
		if (tell() != target) {
			flush();
			accessor.seek(target);
			position = target;
		}
	}

	@Override
	public void skip(int amount) throws IOException {
		if (amount == 0) {
			return;
		}
		flush();
		accessor.skip(amount);
		position = accessor.tell(); // Fix double increment bug
	}


	@Override
	public int size() throws IOException {
		if (cachedSize == -1) {
			cachedSize = accessor.size();
		}
		return cachedSize;
	}

	@Override
	public void resize(int size) throws IOException {
		accessor.resize(size);
		cachedSize = -1; // Invalidate size cache
		position = -1; // Invalidate position cache
	}

	@Override
	public void close() throws IOException {
		flush();
		accessor.close();
		instances.remove(this);
	}

	@Override
	public void flush() throws IOException {
		int size = writeBuffer.size();
		if (size > 0) {

			accessor.write(writeBuffer.toByteArray(), 0, size);
			writeBuffer.reset(); // Clear buffer
			bufferStart = -1;
			cachedSize = -1; // Invalidate size cache
			position = -1; // Invalidate position cache
			nextFlush = -1;
			accessor.flush();
		}
	}

	private static class CacheFlusher implements Runnable {
		@Override
		public void run() {
			for (CachingVirtualFileAccessor accessor : instances.toArray(new CachingVirtualFileAccessor[0])) {
				try {
					if (accessor.nextFlush != -1 && accessor.nextFlush < System.currentTimeMillis()) {
						accessor.flush();
					}
				} catch (IOException ignored) {
				}
			}
		}
	}
}
