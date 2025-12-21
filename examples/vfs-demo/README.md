# VFS Demo - IndexedDB VFS v2

This example demonstrates the IndexedDB Virtual File System v2 implementation with support for large files (100MB+).

## Features Demonstrated

- **Setting up VFS Provider**: How to configure `VirtualFileSystemProvider` to use IndexedDB VFS v2
- **Standard Java I/O**: Using `java.io.*` classes (FileInputStream, FileOutputStream, RandomAccessFile)
- **Basic File Operations**: Writing and reading text files
- **Random Access**: Seeking to specific positions in files
- **Large File Handling**: Creating and reading multi-megabyte files with lazy chunk loading
- **Performance Monitoring**: Using VFSStats to track performance metrics

## How the VFS Provider Works

```java
// Set up the VFS provider - enables java.io.* classes to use IndexedDB
IndexedDBVirtualFileSystem2 vfs = new IndexedDBVirtualFileSystem2();
VirtualFileSystemProvider.setInstance(vfs);

// Now standard Java I/O works with IndexedDB storage
try (FileOutputStream fos = new FileOutputStream("/home/myfile.txt")) {
    fos.write("Hello, World!".getBytes());
}
```

Once `VirtualFileSystemProvider.setInstance()` is called, all `java.io.*` file operations automatically use the configured VFS implementation.

## Running the Demo

### Using Gradle

```bash
# Build and run the demo (from repository root)
./gradlew :examples:vfs-demo:build

# The output will be in examples/vfs-demo/build/dist/
# Open examples/vfs-demo/build/dist/index.html in a browser
```

### Using the --no-daemon flag (recommended)

To avoid TeaVM plugin issues:

```bash
./gradlew --no-daemon :examples:vfs-demo:build
```

## What the Demo Does

1. **Basic File Operations**
   - Creates a text file with `FileOutputStream`
   - Reads it back with `FileInputStream`
   - Shows file size

2. **Random Access Operations**
   - Creates a structured binary file with records
   - Uses `RandomAccessFile` to seek to specific positions
   - Reads records in random order

3. **Large File Operations**
   - Creates a 5MB file (demonstrates 100MB+ capability)
   - Shows write/read performance
   - Demonstrates random access on large files
   - Uses lazy chunk loading (only modified chunks in memory)

4. **Performance Statistics**
   - Displays total reads/writes
   - Shows bytes read/written
   - Cache hit ratio
   - Database operations count

## Expected Output

The console will show:

```
=== IndexedDB VFS v2 Demo ===
VFS provider initialized

--- Basic File Operations ---
Writing to /home/demo.txt
File written successfully
Reading from /home/demo.txt
File contents:
Hello from IndexedDB VFS v2!
This file is stored in your browser's IndexedDB.
File size: 86 bytes

--- Random Access Operations ---
Creating structured file with random access...
Wrote 10 records to file
Reading records in random order...
Header: DATA_FILE_V1
Record 7: id=7, value=21.99113
Record 3: id=3, value=9.42477
Record 9: id=9, value=28.27431

--- Large File Operations ---
Creating 5MB file...
Written 0 MB
Written 1 MB
...
Write completed in XXXms (X.XX MB/s)
Reading 5MB file...
Read 5 MB in XXXms (X.XX MB/s)

--- VFS Performance Statistics ---
Total reads: XXX
Total writes: XXX
Bytes read: XXX (X MB)
Bytes written: XXX (X MB)
Cache hit ratio: XX.X%
...

=== Demo Complete ===
```

## Key Implementation Details

### VFS Provider Setup

The key line that enables java.io.* integration:

```java
VirtualFileSystemProvider.setInstance(new IndexedDBVirtualFileSystem2());
```

This tells TeaVM to use the IndexedDB VFS v2 for all file system operations.

### Large File Support

The VFS v2 uses lazy chunk loading (64KB chunks):
- Only modified chunks are kept in memory
- Unmodified chunks are read from Blob on-demand
- Supports files limited only by browser IndexedDB quota (typically several GB)

### Performance

The demo shows real-world performance metrics:
- Write speeds depend on browser and system
- Read speeds benefit from Blob slicing
- Random access is O(1) with chunk-based lazy loading

## Inspecting the Database

You can inspect the created files using browser DevTools:

1. Open DevTools (F12)
2. Go to Application tab (Chrome) or Storage tab (Firefox)
3. Find IndexedDB → vfs_v3 → files
4. You'll see entries for:
   - `/home` (directory)
   - `/home/demo.txt` (text file)
   - `/home/random_access.dat` (binary file)

Each file entry includes:
- Path, type, parent
- Size, timestamps
- Blob data (for files)

## See Also

- [IndexedDB VFS v2 Documentation](../../docs/INDEXEDDB_VFS_V2.md)
- [Package README](../../awtea-core/src/main/java/me/mdbell/awtea/impl/idb2/README.md)
