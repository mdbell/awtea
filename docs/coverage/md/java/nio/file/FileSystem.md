# Class: `FileSystem` ![Coverage](https://img.shields.io/badge/coverage-53.8%25-yellow)

**Full Name:** `java.nio.file.FileSystem`

**Coverage:** 7 / 13 (53.8%)

```
[██████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░] 53.8%
```

## ✓ Implemented Methods

- `public abstract boolean isOpen()`
- `public abstract boolean isReadOnly()`
- `public abstract java.lang.Iterable getRootDirectories()`
- `public abstract java.lang.String getSeparator()`
- `public abstract java.nio.file.Path getPath(java.lang.String, java.lang.String[])`
- `public abstract java.nio.file.spi.FileSystemProvider provider()`
- `public abstract java.util.Set supportedFileAttributeViews()`

## ✗ Missing Methods

- `public abstract java.lang.Iterable getFileStores()`
- `public abstract java.nio.file.PathMatcher getPathMatcher(java.lang.String)`
- `public abstract java.nio.file.WatchService newWatchService()`
- `public abstract java.nio.file.attribute.UserPrincipalLookupService getUserPrincipalLookupService()`
- `public abstract void close()`

## ✗ Missing Constructors

- `protected java.nio.file.FileSystem()`


[← Back to Package](index.md)
