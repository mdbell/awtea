# Class: `FileSystemProvider` ![Coverage](https://img.shields.io/badge/coverage-65.5%25-yellow)

**Full Name:** `java.nio.file.spi.FileSystemProvider`

**Coverage:** 19 / 29 (65.5%)

```
[████████████████████████████████░░░░░░░░░░░░░░░░░░] 65.5%
```

## ✓ Implemented Methods

- `public abstract boolean isHidden(java.nio.file.Path)`
- `public abstract boolean isSameFile(java.nio.file.Path, java.nio.file.Path)`
- `public abstract java.lang.String getScheme()`
- `public abstract java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream$Filter)`
- `public abstract java.nio.file.FileSystem getFileSystem(java.net.URI)`
- `public abstract java.nio.file.FileSystem newFileSystem(java.net.URI, java.util.Map)`
- `public abstract java.nio.file.Path getPath(java.net.URI)`
- `public abstract java.nio.file.attribute.BasicFileAttributes readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public abstract void checkAccess(java.nio.file.Path, java.nio.file.AccessMode[])`
- `public abstract void copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public abstract void createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public abstract void delete(java.nio.file.Path)`
- `public abstract void move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public boolean deleteIfExists(java.nio.file.Path)`
- `public java.io.InputStream newInputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public java.io.OutputStream newOutputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public java.nio.file.Path readSymbolicLink(java.nio.file.Path)`
- `public static java.util.List installedProviders()`

## ✗ Missing Methods

- `public abstract java.nio.channels.SeekableByteChannel newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[])`
- `public abstract java.nio.file.FileStore getFileStore(java.nio.file.Path)`
- `public abstract java.nio.file.attribute.FileAttributeView getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public abstract java.util.Map readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])`
- `public abstract void setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[])`
- `public java.nio.channels.AsynchronousFileChannel newAsynchronousFileChannel(java.nio.file.Path, java.util.Set, java.util.concurrent.ExecutorService, java.nio.file.attribute.FileAttribute[])`
- `public java.nio.channels.FileChannel newFileChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[])`
- `public java.nio.file.FileSystem newFileSystem(java.nio.file.Path, java.util.Map)`
- `public void createLink(java.nio.file.Path, java.nio.file.Path)`
- `public void createSymbolicLink(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`

## ✓ Implemented Constructors

- `protected java.nio.file.spi.FileSystemProvider()`


[← Back to Package](index.md)
