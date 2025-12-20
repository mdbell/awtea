# IndexedDB Virtual File System v2

## Overview

The IndexedDB VFS v2 (`idb2` package) is a modern, high-performance virtual file system implementation for the browser using IndexedDB as the storage backend. It improves upon the original VFS implementation by using Blob storage instead of chunked storage and leveraging TeaVM's native `JSPromise.await()` support for cleaner async code.

## Key Features

- **Blob Storage**: Stores entire files as Blobs instead of chunks, providing better random access performance
- **JSPromise-based API**: Clean async operations using TeaVM's `JSPromise.await()` for synchronous-style code
- **Path Caching**: Normalized path caching for improved performance
- **Adaptive Caching**: Intelligent read/write caching with automatic flushing
- **Performance Monitoring**: Built-in statistics tracking for debugging and optimization
- **Batch Operations**: Support for batching multiple file operations in a single transaction

## Architecture

### Storage Schema

**Database**: `vfs_v3`  
**Version**: 3

**ObjectStore**: `files`
- **Primary Key**: `path` (string)
- **Indexes**:
  - `parent_index` on `parent` field (non-unique, for directory listings)

**Entry Schema**:
```typescript
{
  path: string,              // Primary key (full path)
  type: "file" | "dir",      // File type
  parent: string,            // Parent directory path
  size: number,              // File size in bytes
  created: number,           // Creation timestamp (ms)
  modified: number,          // Last modified timestamp (ms)
  accessed: number,          // Last accessed timestamp (ms)
  readonly: boolean,         // Read-only flag
  data?: Blob                // File contents (files only, stored as Blob)
}
```

### Package Structure

All classes are in the `me.mdbell.awtea.impl.idb2` package:

#### Core Classes

1. **`IndexedDBVirtualFileSystem2`** - Main VFS implementation
   - Implements TeaVM's `VirtualFileSystem` interface
   - Manages root and home directories
   - Provides path normalization and canonicalization

2. **`IndexedDBVirtualFile2`** - File/directory abstraction
   - Implements TeaVM's `VirtualFile` interface
   - Supports file and directory operations
   - Handles metadata queries (size, timestamps, permissions)

3. **`IndexedDBVirtualFileAccessor2`** - Blob-based file I/O
   - Implements TeaVM's `VirtualFileAccessor` interface
   - Stores entire file in memory for efficient random access
   - Flushes changes to IndexedDB as a single Blob
   - Supports read, write, seek, skip, resize operations

4. **`CachingVirtualFileAccessor2`** - Caching wrapper
   - Wraps `IndexedDBVirtualFileAccessor2` with intelligent caching
   - Write buffering (32KB buffer, auto-flush)
   - Read caching (32KB cache, handles partial hits)
   - Adaptive random/sequential access optimization

#### Helper Classes

5. **`IndexedDBHelper2`** - Core IndexedDB operations
   - Promise-based async methods with sync wrappers using `.await()`
   - File CRUD operations (`getFile`, `putFile`, `deleteFile`)
   - Directory operations (`listDirectory`, `isDirectoryEmpty`)
   - Blob conversion utilities
   - Statistics tracking

6. **`FileEntry`** - JSObject interface for file metadata
   - JavaScript-compatible file entry representation
   - Includes helper methods (`isFile()`, `isDirectory()`, `touch()`, `markModified()`)
   - Factory methods for creating file/directory entries
   - Nested `Blob` interface for file data

7. **`VFSPath`** - Path manipulation utilities
   - Path normalization with `.` and `..` resolution
   - Path caching (up to 1000 entries) for performance
   - Parent/child path resolution
   - Name extraction
   - Path validation

8. **`VFSStats`** - Performance monitoring
   - Tracks reads, writes, bytes transferred
   - Cache hit/miss ratios
   - Database operation counts
   - Error tracking
   - Uptime monitoring

9. **`VFSBatch`** - Batch operations
   - Queue multiple put/delete operations
   - Execute in a single IndexedDB transaction
   - Reduces transaction overhead for directory operations

10. **`IndexedDBVFSException2`** - Exception type
    - Extends `IOException`
    - Used for all VFS v2 errors

## Usage

### Basic Usage

```java
// Create the file system
VirtualFileSystem vfs = new IndexedDBVirtualFileSystem2();

// Get a file reference
VirtualFile file = vfs.getFile("/home/myfile.txt");

// Create a new file
file.createFile("test.txt");

// Write to the file
VirtualFile testFile = vfs.getFile("/home/test.txt");
VirtualFileAccessor accessor = testFile.createAccessor(true, true, false);
accessor.write("Hello, World!".getBytes(), 0, 13);
accessor.flush();
accessor.close();

// Read from the file
accessor = testFile.createAccessor(true, false, false);
byte[] buffer = new byte[13];
int bytesRead = accessor.read(buffer, 0, 13);
accessor.close();
```

### Directory Operations

```java
VirtualFile dir = vfs.getFile("/home/mydir/");

// Create directory
dir.createDirectory("subdir");

// List directory contents
String[] files = dir.listFiles();

// Check if directory
boolean isDir = dir.isDirectory();
```

### Performance Monitoring

```java
IndexedDBVirtualFileSystem2 vfs = new IndexedDBVirtualFileSystem2();

// Perform operations...

// Get statistics
VFSStats stats = vfs.getStats();
System.out.println("Reads: " + stats.getReads());
System.out.println("Writes: " + stats.getWrites());
System.out.println("Cache hit ratio: " + stats.getCacheHitRatio());

// Log stats
vfs.logStats();
```

### Batch Operations

```java
// For bulk directory operations, use VFSBatch
VFSBatch batch = new VFSBatch();

FileEntry file1 = FileEntry.createFile("/home/file1.txt");
FileEntry file2 = FileEntry.createFile("/home/file2.txt");

batch.put(file1);
batch.put(file2);

// Execute in single transaction
IDBTransaction tx = db.transaction("files", "readwrite");
IDBObjectStore store = tx.objectStore("files");
batch.execute(store).await();
```

## Performance Characteristics

### Advantages over VFS v1

1. **Random Access**: Blob storage with lazy chunk loading eliminates chunk lookup overhead
2. **Write Performance**: Optimized chunk-based writes, single Blob reconstruction on flush
3. **Read Performance**: Lazy loading - only reads needed portions of file from Blob
4. **Code Clarity**: JSPromise.await() provides synchronous-style code without callbacks
5. **Metadata Efficiency**: Timestamps and size stored directly (no chunk iteration)
6. **Large File Support**: Supports files of any size with 64KB chunk-based lazy loading

### Memory Considerations

- **Lazy Loading Architecture**: Only modified chunks kept in memory (64KB per chunk)
- **Efficient for Large Files**: Supports 100MB+ files with minimal memory footprint
- **Read-on-Demand**: Unmodified chunks read from Blob only when accessed
- **Write Buffering**: Modified chunks kept in memory until flush
- **Caching Layer**: Adds 64KB overhead per open file (32KB read + 32KB write buffers)

### File Size Support

- **Small Files (< 1MB)**: Entire file may be in memory if all chunks accessed
- **Medium Files (1-10MB)**: Only accessed/modified chunks in memory
- **Large Files (10MB-100MB+)**: Minimal memory usage with lazy loading
- **Maximum**: Limited only by browser's IndexedDB quota (typically several GB)

### Caching Behavior

- **Write Buffer**: 32KB in caching layer, auto-flushes when full or on close
- **Read Cache**: 32KB in caching layer, adaptive prefetching
- **Chunk Size**: 64KB per chunk in base accessor
- **Path Cache**: 1000 normalized paths
- **Metadata Cache**: Per-accessor size and position caching

## Migration from VFS v1

The new VFS v2 uses a separate IndexedDB database (`vfs_v3`) to avoid conflicts with the existing VFS v1 (`vfs`). Both can coexist in the same application.

To migrate:

1. Read files from old VFS v1
2. Write to new VFS v2
3. Optionally delete old VFS v1 database

```java
// Example migration (pseudocode)
VirtualFileSystem oldVfs = new IndexedDBVirtualFileSystem();
VirtualFileSystem newVfs = new IndexedDBVirtualFileSystem2();

VirtualFile oldFile = oldVfs.getFile("/home/data.txt");
if (oldFile.isFile()) {
    VirtualFileAccessor oldAccessor = oldFile.createAccessor(true, false, false);
    byte[] data = new byte[oldFile.length()];
    oldAccessor.read(data, 0, data.length);
    oldAccessor.close();
    
    VirtualFile newFile = newVfs.getFile("/home/data.txt");
    newFile.createFile("data.txt");
    VirtualFileAccessor newAccessor = newFile.createAccessor(false, true, false);
    newAccessor.write(data, 0, data.length);
    newAccessor.flush();
    newAccessor.close();
}
```

## Limitations

1. **Browser Compatibility**: Requires IndexedDB and Blob API support (including Blob.slice())
2. **Concurrent Access**: No file locking mechanism (last write wins)
3. **Storage Quota**: Subject to browser IndexedDB storage limits (typically several GB)
4. **Integer Position**: File positions limited to 32-bit integers (~2GB max position)

## Future Enhancements

Potential improvements for future versions:

- Compressed Blob storage
- File locking and concurrent access control
- Background file sync/persistence
- Encryption support
- Transaction rollback support
- Configurable chunk size for different workloads

## Debugging

Enable debug logging to trace VFS operations:

```java
// VFS operations are logged at DEBUG level
// Stats are logged at INFO level via logStats()

IndexedDBVirtualFileSystem2 vfs = new IndexedDBVirtualFileSystem2();
vfs.logStats(); // Logs current statistics
```

## Technical Notes

### JSPromise.await() Usage

All IndexedDB operations return `JSPromise<T>` and provide sync wrappers using `.await()`:

```java
// Async version
JSPromise<FileEntry> promise = IndexedDBHelper2.getFile("/home/test.txt");
promise.then(entry -> { /* ... */ });

// Sync version (using .await())
FileEntry entry = IndexedDBHelper2.getFileSync("/home/test.txt");
```

The `.await()` method blocks the current execution context until the promise resolves, making the code appear synchronous while still being non-blocking in the browser.

### Blob Conversion

Byte arrays are converted to/from Blobs using native browser APIs:

```java
// byte[] -> Blob
Uint8Array arr = Uint8Array.create(data.length);
arr.set(data);
Blob blob = createBlob(arr); // Uses: new Blob([arr])

// Blob -> byte[]
ArrayBuffer buffer = blob.arrayBuffer().await();
byte[] data = convertArrayBufferToBytes(buffer); // Uses: new Uint8Array(buffer)
```

### Blob Slicing for Large Files

For large files, the VFS uses Blob's `slice()` method to read only needed portions:

```java
// Read a slice of a large Blob without loading entire file
Blob slice = blob.slice(startOffset, endOffset);
byte[] chunkData = readBlobData(slice).await();
```

This lazy-loading approach allows efficient handling of files larger than available memory.

### Lazy Loading Architecture

The accessor uses a chunk-based approach (64KB chunks):

1. **Read**: Chunks read on-demand from Blob using `slice()`
2. **Write**: Modified chunks kept in memory (HashMap)
3. **Flush**: Reconstructs full file from original Blob + modified chunks

This provides O(1) random access while maintaining low memory footprint for large files.

### Path Normalization

Paths are normalized to handle `.` and `..`:

```java
VFSPath.normalize("/home/user/../admin/./file.txt")
// Returns: "/home/admin/file.txt"
```

The normalization cache improves performance for frequently-accessed paths.

## See Also

- Original VFS implementation: `me.mdbell.awtea.impl.idb` package
- TeaVM documentation: https://teavm.org/
- IndexedDB API: https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API
