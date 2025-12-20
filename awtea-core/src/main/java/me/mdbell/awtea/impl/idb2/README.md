# IndexedDB VFS v2 Package

This package (`me.mdbell.awtea.impl.idb2`) contains a modern IndexedDB-backed Virtual File System implementation using Blob storage and JSPromise.

## Quick Start

```java
// Create the file system
VirtualFileSystem vfs = new IndexedDBVirtualFileSystem2();

// Get/create a file
VirtualFile file = vfs.getFile("/home/example.txt");

// Write data
try (VirtualFileAccessor accessor = file.createAccessor(false, true, false)) {
    byte[] data = "Hello, World!".getBytes();
    accessor.write(data, 0, data.length);
    accessor.flush();
}

// Read data
try (VirtualFileAccessor accessor = file.createAccessor(true, false, false)) {
    byte[] buffer = new byte[file.length()];
    accessor.read(buffer, 0, buffer.length);
}
```

## Package Contents

### Core Classes (1,623 lines of code)

| Class | Lines | Purpose |
|-------|-------|---------|
| `IndexedDBHelper2.java` | 336 | Core IndexedDB operations with JSPromise |
| `IndexedDBVirtualFile2.java` | 235 | VirtualFile implementation |
| `CachingVirtualFileAccessor2.java` | 219 | Caching wrapper for performance |
| `IndexedDBVirtualFileAccessor2.java` | 182 | Blob-based file I/O |
| `VFSPath.java` | 173 | Path normalization utilities |
| `VFSBatch.java` | 121 | Batch operations |
| `FileEntry.java` | 151 | File metadata JSObject interface |
| `VFSStats.java` | 111 | Performance monitoring |
| `IndexedDBVirtualFileSystem2.java` | 89 | Main VFS implementation |
| `IndexedDBVFSException2.java` | 6 | Exception type |

## Key Features

✅ **Blob Storage** - Files stored as Blobs with lazy chunk loading  
✅ **Large File Support** - Handles 100MB+ files efficiently (64KB chunks)  
✅ **JSPromise API** - Clean async with `.await()` support  
✅ **Path Caching** - Up to 1000 normalized paths cached  
✅ **Adaptive Caching** - 64KB read/write buffers  
✅ **Statistics** - Performance monitoring built-in  
✅ **Batch Operations** - Single-transaction bulk ops  
✅ **Low Memory** - Only modified chunks kept in memory  

## Database Schema

- **Database**: `vfs_v3` (version 3)
- **ObjectStore**: `files` (keyPath: `path`)
- **Index**: `parent_index` (on `parent` field)

## Architecture

```
IndexedDBVirtualFileSystem2
    └── IndexedDBVirtualFile2 (VirtualFile)
            └── CachingVirtualFileAccessor2
                    └── IndexedDBVirtualFileAccessor2 (VirtualFileAccessor)
                            └── IndexedDBHelper2 (IndexedDB operations)
                                    └── FileEntry (metadata + Blob)
```

## Performance

- **Path normalization**: O(1) with caching
- **File read**: Lazy chunk loading from Blob (64KB chunks)
- **File write**: Modified chunks in memory, single Blob reconstruction on flush
- **Directory listing**: Indexed query on parent field
- **Large files**: Minimal memory footprint (only modified chunks loaded)

## Documentation

See `docs/INDEXEDDB_VFS_V2.md` for complete documentation including:
- Detailed architecture
- Usage examples
- Large file support details
- Migration guide from v1
- Technical implementation notes

## Comparison to VFS v1

| Feature | VFS v1 (idb) | VFS v2 (idb2) |
|---------|--------------|---------------|
| Storage | 8KB chunks | Single Blob + lazy loading |
| Database | `vfs` v2 | `vfs_v3` v3 |
| Async API | Callbacks | JSPromise |
| Random Access | Chunk lookup | Lazy chunk loading |
| Large Files | Limited | 100MB+ supported |
| Path Caching | No | Yes (1000 entries) |
| Statistics | No | Yes (VFSStats) |
| Batch Ops | No | Yes (VFSBatch) |

## License

Same as parent project.
