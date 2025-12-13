package me.mdbell.awtea.impl.idb;

import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.UtilityClass;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.util.ArrayUtils;
import me.mdbell.awtea.util.IDBUtils;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.*;
import org.teavm.jso.crypto.Crypto;
import org.teavm.jso.indexeddb.*;
import org.teavm.jso.json.JSON;

import java.io.IOException;
import java.util.*;

@UtilityClass
@ExtensionMethod({JSObjectsExtensions.class})
public class IndexedDBHelper {

	private static final Logger logger = LoggerFactory.getLogger(IndexedDBHelper.class);

	public final String DB_NAME = "vfs";

	public final String FILE_STORE = "meta";

	public static final String FILE_PATH_PROPERTY = "path";
	public final String FILE_PATH_INDEX = "path_index";

	public static final String FILE_PARENT_PROPERTY = "parent";
	public static final String FILE_PARENT_INDEX = "parent_index";

	public final String CHUNK_STORE = "chunks";
	public final String CHUNK_UUID_INDEX = "uuid_index";

	public static final String CHUNK_UUID_PROPERTY = "uuid";
	public static final String CHUNK_INDEX_PROPERTY = "index";

	public static final String FILE_UUID_PROPERTY = "uuid";

	public static final String ROOT_UUID_PLACEHOLDER = "__ROOT__";

	public final int CHUNK_SIZE = 1024 * 8; // 8KB

	public final int DB_VERSION = 2;

	private final IDBDatabase db = IDBUtils.open(DB_NAME, DB_VERSION, IndexedDBHelper::upgrade);


	private void upgrade(IDBVersionChangeEvent evt) {
		IDBDatabase db = ((IDBOpenDBRequest) evt.getTarget()).getResult();

		// nuke the old stores
		for (String storeName : db.getObjectStoreNames()) {
			db.deleteObjectStore(storeName);
		}
		String[] storeNames = db.getObjectStoreNames();
		if (!ArrayUtils.contains(storeNames, FILE_STORE)) {
			IDBObjectStoreParameters fileParams = JSObjects.create();
			fileParams.keyPath(FILE_PATH_PROPERTY);
			IDBObjectStore store = db.createObjectStore(FILE_STORE, fileParams);

			// unique index on path
			IDBCreateStoreIndexOptions pathOptions = JSObjects.create();
			pathOptions.setUnique(true);
			createIndex(store, FILE_PATH_INDEX, FILE_PATH_PROPERTY, pathOptions);

			// non-unique index on parent UUID
			IDBCreateStoreIndexOptions parentOptions = JSObjects.create();
			parentOptions.setUnique(false);
			createIndex(store, FILE_PARENT_INDEX, FILE_PARENT_PROPERTY, parentOptions);

		}

		if (!ArrayUtils.contains(storeNames, CHUNK_STORE)) {
			IDBObjectStoreParameters chunkParams = JSObjects.create();
			chunkParams.keyPath(CHUNK_UUID_PROPERTY, CHUNK_INDEX_PROPERTY);
			IDBObjectStore store = db.createObjectStore(CHUNK_STORE, chunkParams);
			store.createIndex(CHUNK_UUID_INDEX, new String[]{CHUNK_UUID_PROPERTY, CHUNK_INDEX_PROPERTY});
		}
		// any other upgrades we'd need to do would go here.
	}

	private void log(String str) {
		//System.out.println(str);
	}

	public void resizeFile(String path, int size) {
		path = normalizePath(path, false);
		log("Resizing file: " + path);
		IndexedDBFile file = getFile(path).await();
		if (file.nullish()) {
			throw new IndexDBVirtualFSException("File does not exist");
		}

		if (file.isDirectory()) {
			throw new IndexDBVirtualFSException("Cannot resize a directory");
		}

		int currentSize = file.getSize().intValue();
		if (size == currentSize) {
			return;
		}
		file.setSize(JSNumber.valueOf(size));

		setFile(file).await();
	}

	public int getFileSize(String path) {
		path = normalizePath(path, false);
		log("Getting file size: " + path);
		IndexedDBFile file = getFile(path).await();
		int size = getFileSizeImpl(file);
		log("File size of " + path + ": " + size);
		return size;
	}

	private int getFileSizeImpl(IndexedDBFile file) {
		if (file.nullish()) {
			return 0;
		}

		if (file.isDirectory()) {
			return 0;
		}

		return file.getSize().intValue();
	}

	public void writeFile(String path, int position, byte[] buffer, int offset, int length) {
		path = normalizePath(path, false);
		log("Writing to file: " + path);
		IndexedDBFile file = getFile(path).await();
		if (file.nullish()) {
			String parentUUID = getParentUUID(path);
			file = IndexedDBFile.create(path, false, parentUUID);
		}

		if (file.isDirectory()) {
			throw new IndexDBVirtualFSException("Cannot write to a directory");
		}

		if (file.getSize().doubleValue() < position + length) {
			resizeFile(path, position + length);
		}

		if (file.isReadOnly()) {
			throw new IndexDBVirtualFSException("File is read only");
		}

		writeFileImpl(path, file, position, buffer, offset, length).await();
	}

	private JSPromise<Boolean> writeFileImpl(String path, IndexedDBFile file, int fileOffset,
											 byte[] buffer, int byteOffset, int byteLen) {

		return new JSPromise<>((resolve, reject) -> {
			int position = fileOffset;
			int length = byteLen;
			int offset = byteOffset;
			int lastPosition = position + length;

			IDBTransaction transaction = db.transaction(new String[]{CHUNK_STORE, FILE_STORE}, "readwrite");
			IDBObjectStore chunkStore = transaction.objectStore(CHUNK_STORE);
			IDBIndex index = chunkStore.index(CHUNK_UUID_INDEX);
			IDBObjectStore fileStore = transaction.objectStore(FILE_STORE);
			do {
				log("Writing chunk: " + path + ", position: " + position + ", offset: " + offset + ", length: " + length);

				int chunkIndex = position / CHUNK_SIZE;
				int chunkOffset = position % CHUNK_SIZE;
				int chunkLength = Math.min(CHUNK_SIZE - chunkOffset, length);

				JSObject key = makeKey(file.getUUID(), chunkIndex);
				IDBKeyRange range = IDBKeyRange.only(key);

				IDBGetRequest request = index.getKey(range);
				IDBGetRequest getChunkRequest = index.get(range);

				final int finalOffset = offset;

				getChunkRequest.setOnSuccess(() -> {
					request.setOnSuccess(() -> {
						JSObject dbKey = request.getResult();
						boolean isNewChunk = dbKey.nullish();
						IndexDBChunk chunk = (IndexDBChunk) getChunkRequest.getResult();
						if (chunk.nullish() || isNewChunk) {
							chunk = IndexDBChunk.create(file.getUUID(), chunkIndex);
						}

						byte[] data = chunk.getData();
						System.arraycopy(buffer, finalOffset, data, chunkOffset, chunkLength);
						chunk.setData(data);

						if (isNewChunk) {
							IDBRequest putRequest = chunkStore.add(chunk);
							putRequest.setOnError(() -> {
								log("Failed to add chunk: " + putRequest.getError());
								reject.accept(new IndexDBVirtualFSException("Failed to add chunk"));
							});
						} else {
							IDBRequest putRequest = chunkStore.put(chunk);
							putRequest.setOnError(() -> {
								log("Failed to put chunk: " + putRequest.getError());
								reject.accept(new IndexDBVirtualFSException("Failed to put chunk"));
							});
						}
					});
				});

				getChunkRequest.setOnError(() -> {
					log("Failed to get chunk: " + getChunkRequest.getError());
					reject.accept(new IndexDBVirtualFSException("Failed to get chunk"));
				});

				position += chunkLength;
				offset += chunkLength;
				length -= chunkLength;
			} while (length > 0);

			file.setLastModified(JSNumber.valueOf(System.currentTimeMillis()));

			if (lastPosition > file.getSize().intValue()) {
				file.setSize(JSNumber.valueOf(lastPosition));
			}

			fileStore.put(file);

			// Handle transaction completion
			transaction.setOnComplete(() -> {
				resolve.accept(true);
			});

			transaction.setOnError(() -> {
				log("Transaction failed: " + transaction.getError());
				resolve.accept(false);
			});
		});


	}

	public int readFile(String path, int position, byte[] buffer, int offset, int length) {
		path = normalizePath(path, false);
		log("Reading from file: '" + path + "', position: " + position + ", offset: " + offset + ", length: " + length);
		IndexedDBFile file = getFile(path).await();
		if (file.nullish()) {
			return 0;
		}

		int fileSize = getFileSizeImpl(file);
		if (position >= fileSize) {
			return 0;
		}

		if (position + length > fileSize) {
			length = fileSize - position;
		}

		int totalRead = 0;

		while (length > 0) {
			int chunkIndex = position / CHUNK_SIZE;
			int chunkOffset = position % CHUNK_SIZE;
			int chunkLength = Math.min(CHUNK_SIZE - chunkOffset, length);

			IndexDBChunk chunk = getChunk(file.getUUID(), chunkIndex).await();
			if (!chunk.nullish()) {
				byte[] data = chunk.getData();
				int availableInChunk = Math.max(0, data.length - chunkOffset);
				int copyLength = Math.min(chunkLength, availableInChunk);

				if (copyLength > 0) {
					System.arraycopy(data, chunkOffset, buffer, offset, copyLength);
					totalRead += copyLength;
					offset += copyLength;
				} else {
					// nothing in this chunk, treat as zeros
					Arrays.fill(buffer, offset, offset + chunkLength, (byte) 0);
					totalRead += chunkLength;
					offset += chunkLength;
				}
			} else {
				// missing chunk => implied zeros
				Arrays.fill(buffer, offset, offset + chunkLength, (byte) 0);
				totalRead += chunkLength;
				offset += chunkLength;
			}

			position += chunkLength;
			length -= chunkLength;
		}

		log("Read " + totalRead + " bytes");
		return totalRead;
	}

	public boolean setLastModified(String path, long lastModified) {
		path = normalizePath(path);
		log("Setting last modified: " + path + ", timestamp: " + lastModified);
		IndexedDBFile file = getFile(path).await();
		if (file.nullish()) {
			return false;
		}
		file.setLastModified(JSNumber.valueOf(lastModified));
		return setFile(file).await();
	}

	public boolean setReadOnly(String path, boolean readOnly) {
		path = normalizePath(path);
		log("Setting read only: " + path + ", " + readOnly);
		IndexedDBFile file = getFile(path).await();

		if (file.nullish()) {
			return false;
		}

		file.setReadOnly(readOnly);

		return setFile(file).await();
	}

	public static boolean isReadOnly(String path) {
		path = normalizePath(path);
		IndexedDBFile file = getFile(path).await();

		if (file.nullish()) {
			return false;
		}

		return file.isReadOnly();
	}

	public long getLastModified(String path) {
		path = normalizePath(path);
		IndexedDBFile file = getFile(path).await();
		if (file.nullish()) {
			return 0;
		}
		return (long) file.getLastModified().doubleValue();
	}

	public boolean moveFile(String oldPath, String newPath) {
		oldPath = normalizePath(oldPath);
		newPath = normalizePath(newPath);
		log("Moving file: " + oldPath + " to " + newPath);
		IndexedDBFile file = getFile(oldPath).await();
		if (file.nullish()) {
			return false;
		}

		IndexedDBFile newFile = getFile(newPath).await();
		if (!newFile.nullish()) {
			return false;
		}

		file.setPath(newPath);
		file.setParentUUID(getParentUUID(newPath));

		return setFile(file).await();
	}

	public boolean deleteFile(String path) {
		path = normalizePath(path);
		log("Deleting file: " + path);
		IndexedDBFile file = getFile(path).await();

		if (file.nullish()) {
			log("File does not exist: " + path);
			return false;
		}

		if (file.isDirectory()) {
			boolean isEmpty = isDirectoryEmpty(file.getUUID()).await();
			if (!isEmpty) {
				log("Directory is not empty: " + path);
				return false;
			}
		}

		return deleteFileImpl(file, path).await();
	}


	private JSPromise<Boolean> deleteFileImpl(IndexedDBFile file, String path) {
		return new JSPromise<>((resolve, reject) -> {
			IDBTransaction transaction = db.transaction(new String[]{CHUNK_STORE, FILE_STORE}, "readwrite");
			IDBObjectStore chunkStore = transaction.objectStore(CHUNK_STORE);
			IDBObjectStore fileStore = transaction.objectStore(FILE_STORE);
			IDBKeyRange range = IDBKeyRange.bound(
				JSArray.of(JSString.valueOf(file.getUUID()), JSNumber.valueOf(0)),
				JSArray.of(JSString.valueOf(file.getUUID()), JSNumber.valueOf(Integer.MAX_VALUE))
			);
			IDBCursorRequest chunkCursor = chunkStore.openCursor(range);

			chunkCursor.setOnSuccess(() -> {
				IDBCursor cursor = chunkCursor.getResult();
				if (cursor.nullish()) {
					// no more chunks to delete
					IDBRequest deleteRequest = fileStore.delete(JSString.valueOf(file.getPath()));
					deleteRequest.setOnError(() -> {
						log("Failed to delete file: " + deleteRequest.getError());
						resolve.accept(false);
					});
					return;
				}

				IndexDBChunk chunk = (IndexDBChunk) cursor.getValue();
				if (chunk.getFileUUID().equals(file.getUUID())) {
					IDBRequest deleteRequest = cursor.delete();
					deleteRequest.setOnError(() -> {
						log("Failed to delete chunk: " + deleteRequest.getError());
						resolve.accept(false);
					});
				}
				cursor.doContinue();
			});

			chunkCursor.setOnError(() -> {
				log("Failed to delete file: " + chunkCursor.getError());
				resolve.accept(false);
			});

			transaction.setOnComplete(() -> {
				resolve.accept(true);
			});

			transaction.setOnError(() -> {
				logger.error("Transaction failed: {}", transaction.getError());
				resolve.accept(false);
			});
		});


	}

	private String getParentUUID(String path) {
		String parentPath = normalizePath(path + "/..", true);

		if (parentPath.equals("/")) {
			// we're at root
			return ROOT_UUID_PLACEHOLDER;
		}


		log("Getting parent UUID for path: " + path + ", parent path: " + parentPath);
		IndexedDBFile parent = getFile(parentPath).await();
		if (parent.nullish() || !parent.isDirectory()) {
			throw new IndexDBVirtualFSException("Parent directory does not exist");
		}
		return parent.getUUID();
	}

	public JSPromise<JSBoolean> create(String pathToCreate, boolean isDirectory) throws IOException {
		if (isDirectory && !pathToCreate.endsWith("/")) {
			pathToCreate += "/";
		}

		String path = normalizePath(pathToCreate, isDirectory);
		String parentUUID = getParentUUID(path);

		return new JSPromise<>((resolve, reject) -> {
			log("Creating entry: " + path + ", isDirectory: " + isDirectory);
			JSString pathString = JSString.valueOf(path);
			IDBTransaction transaction = db.transaction(FILE_STORE, "readwrite");
			IDBObjectStore store = transaction.objectStore(FILE_STORE);
			IDBIndex index = store.index(FILE_PATH_INDEX);

			IDBGetRequest existsRequest = index.get(pathString);

			existsRequest.setOnSuccess(() -> {
				JSObject result = existsRequest.getResult();
				if (!result.nullish()) {
					resolve.accept(JSBoolean.valueOf(false));
					return;
				}

				IndexedDBFile entry = IndexedDBFile.create(path, isDirectory, parentUUID);

				IDBRequest addRequest = store.put(entry);

				addRequest.setOnSuccess(() -> {
					log("Created entry: " + path);
					resolve.accept(JSBoolean.valueOf(true));
				});

				addRequest.setOnError(() -> {
					log("Failed to create entry: " + addRequest.getError());
					reject.accept(new IOException("Failed to create entry"));
				});
			});

			existsRequest.setOnError(() -> {
				log("Failed to check if entry exists: " + existsRequest.getError());
				reject.accept(new IOException("Failed to check if entry exists"));
			});
		});
	}


	@SneakyThrows
	public boolean createDirectory(String path) {
		path = normalizePath(path, true);
		IndexedDBFile file = getFile(path).await();

		if (!file.nullish()) {
			return false;
		}

		return create(path, true).await().booleanValue();
	}


	public boolean createFile(String path) throws IOException {
		path = normalizePath(path, false);
		log("Creating file: " + path);
		// we can let directories be created without a trailing slash (as we'll add it)
		// but files must _not_ have a trailing slash
		if (path.endsWith("/")) {
			log("Attempted to create file with trailing slash: " + path);
			throw new IllegalArgumentException("Path must not end with a slash");
		}

		IndexedDBFile file = getFile(path).await();

		if (!file.nullish()) {
			return false;
		}

		return create(path, false).await().booleanValue();
	}

	private String findListUUID(String path) {

		if (path.equals("/")) {
			return ROOT_UUID_PLACEHOLDER;
		}

		String normalizedPath = normalizePath(path, true);
		IndexedDBFile dir = getFile(normalizedPath).await();

		if (dir.nullish() || !dir.isDirectory()) {
			return null;
		}

		return dir.getUUID();
	}

	public JSPromise<String[]> listDirectory(String reqPath) {
		String path = normalizePath(reqPath, true);
		String parentUUID = findListUUID(path);
		log("Listing directory: " + path + ", parent UUID: " + parentUUID);
		IDBTransaction tx = db.transaction(FILE_STORE, "readonly");
		IDBObjectStore store = tx.objectStore(FILE_STORE);
		IDBIndex parentIndex = store.index(FILE_PARENT_INDEX);
		IDBKeyRange range = IDBKeyRange.only(JSString.valueOf(parentUUID));
		IDBCursorRequest req = parentIndex.openCursor(range);

		return new JSPromise<>((resolve, reject) -> {
			List<String> results = new ArrayList<>();

			req.setOnSuccess(() -> {
				IDBCursor cursor = req.getResult();
				if (cursor.nullish()) {
					log("Completed listing directory: " + path + ", found " + results.size() + " entries");
					String[] arr = results.toArray(String[]::new);
					log("Directory entries: " + Arrays.toString(arr));
					resolve.accept(arr);
					return;
				}

				IndexedDBFile file = (IndexedDBFile) cursor.getValue();
				String filePath = file.getPath();
				String leaf = filePath.substring(path.length());
				log("Found entry: " + leaf);
				results.add(leaf);

				cursor.doContinue();
			});

			req.setOnError(() -> {
				log("Failed to list directory: " + req.getError());
				reject.accept(new IOException("Failed to list directory"));
			});
		});
	}

	public String normalizePath(String path) {
		return normalizePath(path, isDirectory(path));
	}

	public String normalizePath(String path, boolean isDirectory) {
		String[] parts = path.split("/");
		Stack<String> stack = new Stack<>();
		for (String part : parts) {
			if (part.isEmpty() || part.equals(".")) {
				continue;
			}
			if (part.equals("..")) {
				if (!stack.isEmpty()) {
					stack.pop();
				}
			} else {
				stack.push(part);
			}
		}
		StringBuilder normalizedPath = new StringBuilder("/");
		Iterator<String> iterator = stack.iterator();
		while (iterator.hasNext()) {
			normalizedPath.append(iterator.next());
			if (iterator.hasNext()) {
				normalizedPath.append("/");
			}
		}
		if (isDirectory && !normalizedPath.toString().endsWith("/")) {
			normalizedPath.append("/");
		}
		return normalizedPath.toString();
	}

	public boolean isDirectory(String path) {
		path = normalizePath(path, true);
		// fast path for root
		if (path.equals("/")) {
			return true;
		}
		if (!path.endsWith("/")) {
			path += "/";
		}
		IndexedDBFile file = getFile(path).await();
		return !file.nullish() && file.isDirectory();
	}

	public boolean isFile(String path) {
		path = normalizePath(path, false);
		IndexedDBFile file = getFile(path).await();
		return !file.nullish() && !file.isDirectory();
	}

	public static boolean exists(String path) {
		path = IndexedDBHelper.normalizePath(path);
		IndexedDBFile file = getFile(path).await();
		return !file.nullish();
	}

	private JSPromise<IndexedDBFile> getFile(String path) {
		IDBTransaction tx = db.transaction(FILE_STORE, "readonly");
		IDBObjectStore store = tx.objectStore(FILE_STORE);
		IDBIndex index = store.index(FILE_PATH_INDEX);

		IDBGetRequest req = index.get(JSString.valueOf(path));
		return new JSPromise<>((resolve, reject) -> {
			req.setOnSuccess(() -> {
				IndexedDBFile entry = (IndexedDBFile) req.getResult();
				resolve.accept(entry.nullish() ? null : entry);
			});
			req.setOnError(() -> {
				reject.accept(new IndexDBVirtualFSException(
					"Failed to get file: " + req.getError()
				));
			});
		});
	}

	private JSPromise<Boolean> setFile(IndexedDBFile file) {
		IDBTransaction transaction = db.transaction(FILE_STORE, "readwrite");
		IDBObjectStore store = transaction.objectStore(FILE_STORE);

		return new JSPromise<>(
			(resolve, reject) -> {
				IDBRequest req = store.put(file);
				req.setOnSuccess(() -> resolve.accept(true));
				req.setOnError(() -> reject.accept(new IndexDBVirtualFSException("Failed to set file")));
			}
		);


	}

	private JSPromise<Boolean> isDirectoryEmpty(String parentUUID) {
		return new JSPromise<>((resolve, reject) -> {
			IDBTransaction tx = db.transaction(FILE_STORE, "readonly");
			IDBObjectStore store = tx.objectStore(FILE_STORE);
			IDBIndex parentIndex = store.index(FILE_PARENT_INDEX);
			IDBKeyRange range = IDBKeyRange.only(JSString.valueOf(parentUUID));
			IDBCursorRequest req = parentIndex.openCursor(range);

			req.setOnSuccess(() -> {
				IDBCursor cursor = req.getResult();
				// If the first cursor result is null, there are no children
				boolean empty = cursor.nullish();
				resolve.accept(empty);
			});

			req.setOnError(() -> {
				log("Failed to check if directory is empty: " + req.getError());
				// be conservative: treat as not empty, or reject; here we reject
				reject.accept(new IndexDBVirtualFSException(
					"Failed to check if directory is empty: " + req.getError()
				));
			});
		});
	}


	private JSPromise<IndexDBChunk> getChunk(String uuid, int chunkIndex) {
		JSObject key = makeKey(uuid, chunkIndex);

		IDBTransaction transaction = db.transaction(CHUNK_STORE, "readonly");
		IDBObjectStore store = transaction.objectStore(CHUNK_STORE);
		IDBIndex index = store.index(CHUNK_UUID_INDEX);

		IDBKeyRange range = IDBKeyRange.only(key);

		IDBGetRequest getRequest = index.get(range);
		return new JSPromise<>(((resolve, reject) -> {
			getRequest.setOnSuccess(() -> {
				IndexDBChunk chunk = (IndexDBChunk) getRequest.getResult();
				resolve.accept(chunk.nullish() ? null : chunk);
			});

			getRequest.setOnError(() -> {
				logger.debug("Chunk read error (treated as null): {}", getRequest.getError());
				// we resolve still instead of rejecting, as missing chunks are valid
				// and we don't want to treat them as errors.
				resolve.accept(null);
			});
		}));
	}

	private static JSObject makeKey(String uuid, int index) {
		return JSArray.of(JSString.valueOf(uuid), JSNumber.valueOf(index));
	}

	@JSBody(params = {"store", "name", "keypath", "options"}, script = "store.createIndex(name, keypath, options);")
	private native void createIndex(IDBObjectStore store, String name, String keypath, IDBCreateStoreIndexOptions options);

	public interface IndexedDBFile extends JSObject {

		@JSProperty(FILE_UUID_PROPERTY)
		String getUUID();
		@JSProperty(FILE_UUID_PROPERTY)
		void setUUID(String uuid);

		@JSProperty(FILE_PATH_PROPERTY)
		String getPath();
		@JSProperty(FILE_PATH_PROPERTY)
		void setPath(String path);

		@JSProperty(FILE_PARENT_PROPERTY)
		String getParentUUID();
		@JSProperty(FILE_PARENT_PROPERTY)
		void setParentUUID(String parentUUID);

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

		static IndexedDBFile create(String path, boolean directory, String parentUUID) {
			IndexedDBFile file = JSObjects.create();
			file.setUUID(Crypto.current().randomUUID());
			file.setPath(path);
			file.setParentUUID(parentUUID);
			file.setSize(directory ? JSNumber.valueOf(-1) : JSNumber.valueOf(0));
			file.setReadOnly(false);
			file.setLastModified(JSNumber.valueOf(System.currentTimeMillis()));
			return file;
		}

		default boolean isDirectory() {
			return getSize().intValue() < 0;
		}
		default boolean isFile() {
			return !isDirectory();
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
