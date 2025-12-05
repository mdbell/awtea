package me.mdbell.awtea.impl.idb;

import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.UtilityClass;
import me.mdbell.awtea.util.ArrayUtils;
import me.mdbell.awtea.util.IDBUtils;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.crypto.Crypto;
import org.teavm.jso.indexeddb.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
@ExtensionMethod({JSObjectsExtensions.class})
public class IndexedDBHelper {

	public final String DB_NAME = "vfs";

	public final String FILE_STORE = "meta";
	public final String FILE_UUID_INDEX = "uuid_index";
	public final String CHUNK_STORE = "chunks";
	public final String CHUNK_UUID_INDEX = "uuid_index";

	public static final String CHUNK_UUID_PROPERTY = "uuid";
	public static final String CHUNK_INDEX_PROPERTY = "index";

	public static final String FILE_UUID_PROPERTY = "uuid";

	public final JSNumber DIRECTORY_MARKER = JSNumber.valueOf(-1);

	public final int CHUNK_SIZE = 1024 * 5;

	public final int DB_VERSION = 2;

	private final IDBDatabase db = IDBUtils.open(DB_NAME, DB_VERSION, IndexedDBHelper::upgrade);


	private void upgrade(IDBVersionChangeEvent evt) {
		IDBDatabase db = ((IDBOpenDBRequest) evt.getTarget()).getResult();

		// nuke the old stores
		for (String storeName : db.getObjectStoreNames()) {
			db.deleteObjectStore(storeName);
		}
		IDBObjectStoreParameters params = JSObjects.create();
		params.autoIncrement(true);

		String[] storeNames = db.getObjectStoreNames();
		if (!ArrayUtils.contains(storeNames, FILE_STORE)) {
			IDBObjectStore store = db.createObjectStore(FILE_STORE, params);
			IDBCreateStoreIndexOptions options = JSObjects.create();
			options.setUnique(true);
			createIndex(store, FILE_UUID_INDEX, FILE_UUID_PROPERTY, options);
		}
		if (!ArrayUtils.contains(storeNames, CHUNK_STORE)) {
			IDBObjectStore store = db.createObjectStore(CHUNK_STORE, params);
			store.createIndex(CHUNK_UUID_INDEX, new String[]{CHUNK_UUID_PROPERTY, CHUNK_INDEX_PROPERTY});
		}
		// any other upgrades we'd need to do would go here.
	}

	private void log(String str) {
		System.out.println(str);
	}

	public void resizeFile(String path, int size) {
		log("Resizing file: " + path);
		IndexedDBFile file = getFile(path);
		if (file.nullish()) {
			throw new IndexDBVirtualFSException("File does not exist");
		}

		if (file.getSize() == DIRECTORY_MARKER) {
			throw new IndexDBVirtualFSException("Cannot resize a directory");
		}

		int currentSize = file.getSize().intValue();
		if (size == currentSize) {
			return;
		}
		file.setSize(JSNumber.valueOf(size));

		setFile(file);
	}

	public int getFileSize(String path) {
		log("Getting file size: " + path);
		IndexedDBFile file = getFile(path);
		int size = getFileSizeImpl(file);
		log("File size of " + path + ": " + size);
		return size;
	}

	private int getFileSizeImpl(IndexedDBFile file) {
		if (file.nullish()) {
			return 0;
		}

		if (file.getSize() == DIRECTORY_MARKER) {
			return 0;
		}

		return file.getSize().intValue();
	}

	public void writeFile(String path, int position, byte[] buffer, int offset, int length) {
		log("Writing to file: " + path);
		IndexedDBFile file = getFile(path);
		if (file.nullish()) {
			file = IndexedDBFile.create(path, false);
		}

		if (file.getSize() == DIRECTORY_MARKER) {
			throw new IndexDBVirtualFSException("Cannot write to a directory");
		}

		if (file.getSize().doubleValue() < position + length) {
			resizeFile(path, position + length);
		}

		if (file.isReadOnly()) {
			throw new IndexDBVirtualFSException("File is read only");
		}

		writeFileImpl(path, file, position, buffer, offset, length);
	}

	@Async
	private native boolean writeFileImpl(String path, IndexedDBFile file, int position, byte[] buffer, int offset, int length);

	@SuppressWarnings("unused")
	public void writeFileImpl(String path, IndexedDBFile file, int position, byte[] buffer, int offset, int length, AsyncCallback<Boolean> callback) {

		int lastPosition = position + length;

		IDBTransaction transaction = db.transaction(new String[]{CHUNK_STORE, FILE_STORE}, "readwrite");
		IDBObjectStore chunkStore = transaction.objectStore(CHUNK_STORE);
		IDBIndex index = chunkStore.index(CHUNK_UUID_INDEX);
		IDBObjectStore fileStore = transaction.objectStore(FILE_STORE);

		List<IDBRequest> requests = new ArrayList<>();
		do {
			log("Writing chunk: " + path + ", position: " + position + ", offset: " + offset + ", length: " + length);

			int chunkIndex = position / CHUNK_SIZE;
			int chunkOffset = position % CHUNK_SIZE;
			int chunkLength = Math.min(CHUNK_SIZE - chunkOffset, length);

			JSObject key = makeKey(file.getUUID(), chunkIndex);
			IDBKeyRange range = IDBKeyRange.only(key);

			IDBGetRequest getChunkRequest = index.get(range);

			final int finalOffset = offset;

			getChunkRequest.setOnSuccess(() -> {

				IDBGetRequest request = index.getKey(range);

				request.setOnSuccess(() -> {
					JSObject dbKey = request.getResult();
					IndexDBChunk chunk = (IndexDBChunk) getChunkRequest.getResult();
					if (chunk.nullish() || dbKey.nullish()) {
						chunk = IndexDBChunk.create(file.getUUID(), chunkIndex);
					}

					byte[] data = chunk.getData();
					System.arraycopy(buffer, finalOffset, data, chunkOffset, chunkLength);
					chunk.setData(data);

					// Batch chunk update
					IDBRequest putRequest = chunkStore.put(chunk, dbKey);
					requests.add(putRequest);
				});
			});

			getChunkRequest.setOnError(() -> {
				log("Failed to get chunk: " + getChunkRequest.getError());
				throw new IndexDBVirtualFSException("Failed to get chunk");
			});

			requests.add(getChunkRequest);

			position += chunkLength;
			offset += chunkLength;
			length -= chunkLength;
		} while (length > 0);

		file.setLastModified(JSNumber.valueOf(System.currentTimeMillis()));

		if (lastPosition > file.getSize().intValue()) {
			file.setSize(JSNumber.valueOf(lastPosition));
			JSObject key = file.getKey();
			IDBRequest filePutRequest = fileStore.put(file, key);
			requests.add(filePutRequest);
		}

		// Handle transaction completion
		transaction.setOnComplete(() -> {
			callback.complete(true);
		});

		transaction.setOnError(() -> {
			log("Transaction failed: " + transaction.getError());
			callback.complete(false);
		});
	}

	public int readFile(String path, int position, byte[] buffer, int offset, int length) {
		log("Reading from file: '" + path + "', position: " + position + ", offset: " + offset + ", length: " + length);
		IndexedDBFile file = getFile(path);
		if (file.nullish()) {
			return 0;
		}

		if (position + length > file.getSize().intValue()) {
			length = file.getSize().intValue() - position;
		}

		int read = 0;
		do {
			int chunkIndex = position / CHUNK_SIZE;
			int chunkOffset = position % CHUNK_SIZE;
			int chunkLength = Math.min(CHUNK_SIZE - chunkOffset, length);

			IndexDBChunk chunk = getChunk(file.getUUID(), chunkIndex);

			if (chunk.nullish()) {
				// assume the missing chunk is 0s
				for (int i = 0; i < chunkLength; i++) {
					buffer[offset + i] = 0;
				}
			} else {

				byte[] data = chunk.getData();
				System.arraycopy(data, chunkOffset, buffer, offset, chunkLength);
			}

			position += chunkLength;
			offset += chunkLength;
			length -= chunkLength;
			read += chunkLength;
		} while (length > 0);
		log("Read " + read + " bytes");
		return read;
	}

	public boolean setLastModified(String path, long lastModified) {
		log("Setting last modified: " + path + ", timestamp: " + lastModified);
		IndexedDBFile file = getFile(path);
		if (file.nullish()) {
			return false;
		}
		file.setLastModified(JSNumber.valueOf(lastModified));
		return setFile(file);
	}

	public boolean setReadOnly(String path, boolean readOnly) {
		log("Setting read only: " + path + ", " + readOnly);
		IndexedDBFile file = getFile(path);

		if (file.nullish()) {
			return false;
		}

		file.setReadOnly(readOnly);

		return setFile(file);
	}

	public static boolean isReadOnly(String path) {

		IndexedDBFile file = getFile(path);

		if (file.nullish()) {
			return false;
		}

		return file.isReadOnly();
	}

	public long getLastModified(String path) {
		IndexedDBFile file = getFile(path);
		if (file.nullish()) {
			return 0;
		}
		return (long) file.getLastModified().doubleValue();
	}

	public boolean moveFile(String oldPath, String newPath) {
		log("Moving file: " + oldPath + " to " + newPath);
		IndexedDBFile file = getFile(oldPath);
		if (file.nullish()) {
			return false;
		}

		IndexedDBFile newFile = getFile(newPath);
		if (!newFile.nullish()) {
			return false;
		}

		file.setPath(newPath);

		return setFile(file);
	}

	public boolean deleteFile(String path) {
		log("Deleting file: " + path);
		IndexedDBFile file = getFile(path);

		if (file.nullish()) {
			log("File does not exist: " + path);
			return false;
		}

		return deleteFileImpl(file, path);
	}

	@Async
	private native boolean deleteFileImpl(IndexedDBFile file, String path);

	@SuppressWarnings("unused")
	private void deleteFileImpl(IndexedDBFile file, String path, AsyncCallback<Boolean> callback) {

		IDBTransaction transaction = db.transaction(new String[]{CHUNK_STORE, FILE_STORE}, "readwrite");
		IDBObjectStore chunkStore = transaction.objectStore(CHUNK_STORE);
		IDBObjectStore fileStore = transaction.objectStore(FILE_STORE);

		IDBCursorRequest chunkCursor = chunkStore.openCursor();

		chunkCursor.setOnSuccess(() -> {
			IDBCursor cursor = chunkCursor.getResult();
			if (cursor.nullish()) {
				// no more chunks to delete
				IDBRequest deleteRequest = fileStore.delete(file.getKey());
				deleteRequest.setOnError(() -> {
					log("Failed to delete file: " + deleteRequest.getError());
					callback.complete(false);
				});
				return;
			}

			IndexDBChunk chunk = (IndexDBChunk) cursor.getValue();
			if (chunk.getFileUUID().equals(file.getUUID())) {
				IDBRequest deleteRequest = cursor.delete();
				deleteRequest.setOnError(() -> {
					log("Failed to delete chunk: " + deleteRequest.getError());
					callback.complete(false);
				});
			}
			cursor.doContinue();
		});

		chunkCursor.setOnError(() -> {
			log("Failed to delete file: " + chunkCursor.getError());
			callback.complete(false);
		});

		transaction.setOnComplete(() -> {
			callback.complete(true);
		});

		transaction.setOnError(() -> {
			callback.complete(false);
			System.err.println("Transaction failed: " + transaction.getError());
		});
	}

	@Async
	private native boolean create(String path, boolean isDirectory) throws IOException;

	@SuppressWarnings("unused")
	private void create(String path, boolean isDirectory, AsyncCallback<Boolean> callback) throws IOException {
		log("Creating entry: " + path + ", isDirectory: " + isDirectory);
		JSString pathString = JSString.valueOf(path);
		IDBTransaction transaction = db.transaction(FILE_STORE, "readwrite");
		IDBObjectStore store = transaction.objectStore(FILE_STORE);
		IDBIndex index = store.index(FILE_UUID_INDEX);

		IDBGetRequest existsRequest = index.get(pathString);

		existsRequest.setOnSuccess(() -> {
			JSObject result = existsRequest.getResult();
			if (!result.nullish()) {
				callback.complete(false);
				return;
			}

			IndexedDBFile entry = IndexedDBFile.create(path, isDirectory);
			entry.setPath(path);

			IDBRequest addRequest = store.put(entry);

			addRequest.setOnSuccess(() -> callback.complete(true));

			addRequest.setOnError(() -> {
				log("Failed to create entry: " + addRequest.getError());
				callback.error(new IOException("Failed to create entry"));
			});
		});

		existsRequest.setOnError(() -> {
			log("Failed to check if entry exists: " + existsRequest.getError());
			callback.error(new IOException("Failed to check if entry exists"));
		});
	}

	@SneakyThrows
	public boolean createDirectory(String path) {
		IndexedDBFile file = getFile(path);

		if (!file.nullish()) {
			return false;
		}

		return create(path, true);
	}


	public boolean createFile(String path) throws IOException {
		log("Creating file: " + path);
		// we can let directories be created without a trailing slash (as we'll add it)
		// but files must _not_ have a trailing slash
		if (path.endsWith("/")) {
			log("Attempted to create file with trailing slash: " + path);
			throw new IllegalArgumentException("Path must not end with a slash");
		}

		IndexedDBFile file = getFile(path);

		if (!file.nullish()) {
			return false;
		}

		return create(path, false);
	}

	@Async
	public native String[] listDirectory(String path);

	@SuppressWarnings("unused")
	private void listDirectory(String path, AsyncCallback<String[]> callback) {

		String dirPath = path.endsWith("/") ? path : path + "/";

		log("Listing directory: " + dirPath);

		IDBTransaction tx = db.transaction(FILE_STORE, "readonly");
		IDBObjectStore store = tx.objectStore(FILE_STORE);
		IDBIndex index = store.index(FILE_UUID_INDEX);

		IDBCursorRequest req = index.openCursor();
		Set<String> results = new HashSet<>();
		req.setOnSuccess(() -> {
			IDBCursor cursor = req.getResult();

			if (cursor.nullish()) {
				callback.complete(results.toArray(new String[0]));
				return;
			}

			IndexedDBFile file = (IndexedDBFile) cursor.getValue();
			String filePath = file.getPath();
			if (filePath.startsWith(dirPath)) {
				// all files are stored in their absolute path form, so we need to extract
				// only the immediate children of the directory.
				String relativePath = filePath.substring(dirPath.length());
				if (relativePath.startsWith("/")) {
					relativePath = relativePath.substring(1);
				}
				int slashIndex = relativePath.indexOf("/");
				if (slashIndex != -1) {
					relativePath = relativePath.substring(0, slashIndex);
				}
				if (!relativePath.isEmpty()) {
					results.add(relativePath);
				}
			}
			cursor.doContinue();
		});

		req.setOnError(() -> {
			log("Failed to list directory: " + req.getError());
			callback.error(new IndexDBVirtualFSException("Failed to list directory"));
		});
	}

	public boolean isDirectory(String path) {
		IndexedDBFile file = getFile(path);
		return !file.nullish() && file.getSize() == DIRECTORY_MARKER;
	}

	public boolean isFile(String path) {
		IndexedDBFile file = getFile(path);
		return !file.nullish() && file.getSize() != DIRECTORY_MARKER;
	}

	@Async
	private native IndexedDBFile getFile(String path);

	@SuppressWarnings("unused")
	private void getFile(String path, AsyncCallback<IndexedDBFile> callback) {
		String pathDir = path.endsWith("/") ? path : path + "/";
		IDBTransaction transaction = db.transaction(FILE_STORE, "readonly");
		IDBObjectStore store = transaction.objectStore(FILE_STORE);
		IDBIndex index = store.index(FILE_UUID_INDEX);

		IDBCursorRequest cursor = index.openCursor();

		cursor.setOnSuccess(() -> {
			IDBCursor c = cursor.getResult();
			if (c.nullish()) {
				callback.complete(null);
				return;
			}

			IndexedDBFile entry = (IndexedDBFile) c.getValue();
			if (path.equals(entry.getPath()) || pathDir.equals(entry.getPath())) {
				entry.setKey(c.getPrimaryKey());
				callback.complete(entry);
			} else {
				c.doContinue();
			}
		});
		cursor.setOnError(() -> {
			log("Failed to get file: " + cursor.getError());
			callback.error(new IndexDBVirtualFSException("Failed to get file" + cursor.getError().toString()));
		});
	}

	@Async
	private native boolean setFile(IndexedDBFile file);

	@SuppressWarnings("unused")
	private void setFile(IndexedDBFile file, AsyncCallback<Boolean> callback) {
		JSString uuidString = JSString.valueOf(file.getUUID());
		IDBTransaction transaction = db.transaction(FILE_STORE, "readwrite");
		IDBObjectStore store = transaction.objectStore(FILE_STORE);
		IDBIndex index = store.index(FILE_UUID_INDEX);

		IDBCursorRequest cursorReq = store.openCursor();

		cursorReq.setOnSuccess(() -> {
			IDBCursor cursor = cursorReq.getResult();

			if (cursor.nullish()) {
				IDBRequest addRequest = store.put(file);
				addRequest.setOnSuccess(() -> callback.complete(true));
				addRequest.setOnError(() -> callback.error(new IndexDBVirtualFSException("Failed to set file")));
			} else {
				IndexedDBFile entry = (IndexedDBFile) cursor.getValue();
				if (file.getPath().equals(entry.getPath())) {
					file.setUUID(entry.getUUID());
					IDBRequest putRequest = store.put(file, cursor.getPrimaryKey());
					putRequest.setOnSuccess(() -> callback.complete(true));
					putRequest.setOnError(() -> callback.error(new IndexDBVirtualFSException("Failed to set file")));
					return;
				}
				cursor.doContinue();
			}
		});
	}

	@Async
	private native IndexDBChunk getChunk(String uuid, int chunkIndex);

	@SuppressWarnings("unused")
	private void getChunk(String uuid, int chunkIndex, AsyncCallback<IndexDBChunk> callback) {
		JSObject key = makeKey(uuid, chunkIndex);

		IDBTransaction transaction = db.transaction(CHUNK_STORE, "readonly");
		IDBObjectStore store = transaction.objectStore(CHUNK_STORE);
		IDBIndex index = store.index(CHUNK_UUID_INDEX);

		IDBKeyRange range = IDBKeyRange.only(key);

		IDBGetRequest getRequest = index.get(range);

		getRequest.setOnSuccess(() -> {
			IndexDBChunk chunk = (IndexDBChunk) getRequest.getResult();
			callback.complete(chunk.nullish() ? null : chunk);
		});

		getRequest.setOnError(() -> {
			System.err.println(getRequest.getError().toString());
			callback.complete(null);
		});
	}

	private static JSObject makeKey(String uuid, int index) {
		return JSArray.of(JSString.valueOf(uuid), JSNumber.valueOf(index));
	}

	@JSBody(params = {"store", "name", "keypath", "options"}, script = "store.createIndex(name, keypath, options);")
	private native void createIndex(IDBObjectStore store, String name, String keypath, IDBCreateStoreIndexOptions options);

	public static boolean exists(String path) {
		IndexedDBFile file = getFile(path);
		return !file.nullish();
	}

	public interface IndexedDBFile extends JSObject {

		@JSProperty
		JSObject getKey();

		@JSProperty
		void setKey(JSObject key);

		@JSProperty(FILE_UUID_PROPERTY)
		String getUUID();

		@JSProperty(FILE_UUID_PROPERTY)
		void setUUID(String uuid);

		@JSProperty
		String getPath();

		@JSProperty
		void setPath(String path);

		@JSProperty
		JSNumber getSize();

		@JSProperty
		void setSize(JSNumber size);

		@JSProperty("readonly")
		boolean isReadOnly();

		@JSProperty("readonly")
		void setReadOnly(boolean readOnly);

		@JSProperty
		JSNumber getLastModified();

		@JSProperty
		void setLastModified(JSNumber lastModified);

		static IndexedDBFile create(String path, boolean directory) {
			IndexedDBFile file = JSObjects.create();
			file.setUUID(Crypto.current().randomUUID());
			file.setPath(path);
			file.setSize(directory ? DIRECTORY_MARKER : JSNumber.valueOf(0));
			file.setReadOnly(false);
			file.setLastModified(JSNumber.valueOf(System.currentTimeMillis()));
			return file;
		}
	}

	public interface IndexDBChunk extends JSObject {

		@JSProperty(CHUNK_UUID_PROPERTY)
		String getFileUUID();

		@JSProperty(CHUNK_UUID_PROPERTY)
		void setFileUUID(String uuid);

		@JSProperty(CHUNK_INDEX_PROPERTY)
		int getIndex();

		@JSProperty(CHUNK_INDEX_PROPERTY)
		void setIndex(int index);

		@JSByRef
		@JSProperty
		byte[] getData();

		@JSProperty
		void setData(@JSByRef byte[] data);

		static IndexDBChunk create(String uuid, int index) {
			IndexDBChunk chunk = JSObjects.create();
			chunk.setFileUUID(uuid);
			chunk.setIndex(index);
			chunk.setData(new byte[CHUNK_SIZE]);
			return chunk;
		}
	}

	interface IDBCreateStoreIndexOptions extends JSObject {
		@JSProperty("unique")
		void setUnique(boolean unique);
	}
}
