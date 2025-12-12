# Class: `Files` ![Coverage](https://img.shields.io/badge/coverage-1.4%25-red)

**Full Name:** `java.nio.file.Files`

**Coverage:** 1 / 70 (1.4%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 1.4%
```

## ✓ Implemented Methods

- `public static java.io.BufferedReader newBufferedReader(java.nio.file.Path)`

## ✗ Missing Methods

- `public static boolean deleteIfExists(java.nio.file.Path)`
- `public static boolean exists(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isDirectory(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isExecutable(java.nio.file.Path)`
- `public static boolean isHidden(java.nio.file.Path)`
- `public static boolean isReadable(java.nio.file.Path)`
- `public static boolean isRegularFile(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isSameFile(java.nio.file.Path, java.nio.file.Path)`
- `public static boolean isSymbolicLink(java.nio.file.Path)`
- `public static boolean isWritable(java.nio.file.Path)`
- `public static boolean notExists(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static byte[] readAllBytes(java.nio.file.Path)`
- `public static java.io.BufferedReader newBufferedReader(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.io.BufferedWriter newBufferedWriter(java.nio.file.Path, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.io.BufferedWriter newBufferedWriter(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.io.InputStream newInputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.io.OutputStream newOutputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.lang.Object getAttribute(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])`
- `public static java.lang.String probeContentType(java.nio.file.Path)`
- `public static java.lang.String readString(java.nio.file.Path)`
- `public static java.lang.String readString(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.nio.channels.SeekableByteChannel newByteChannel(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.nio.channels.SeekableByteChannel newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path)`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path, java.lang.String)`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream$Filter)`
- `public static java.nio.file.FileStore getFileStore(java.nio.file.Path)`
- `public static java.nio.file.Path copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static java.nio.file.Path createDirectories(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createFile(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createLink(java.nio.file.Path, java.nio.file.Path)`
- `public static java.nio.file.Path createSymbolicLink(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempDirectory(java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempDirectory(java.nio.file.Path, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempFile(java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempFile(java.nio.file.Path, java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static java.nio.file.Path readSymbolicLink(java.nio.file.Path)`
- `public static java.nio.file.Path setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[])`
- `public static java.nio.file.Path setLastModifiedTime(java.nio.file.Path, java.nio.file.attribute.FileTime)`
- `public static java.nio.file.Path setOwner(java.nio.file.Path, java.nio.file.attribute.UserPrincipal)`
- `public static java.nio.file.Path setPosixFilePermissions(java.nio.file.Path, java.util.Set)`
- `public static java.nio.file.Path walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)`
- `public static java.nio.file.Path walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)`
- `public static java.nio.file.Path write(java.nio.file.Path, byte[], java.nio.file.OpenOption[])`
- `public static java.nio.file.Path write(java.nio.file.Path, java.lang.Iterable, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path write(java.nio.file.Path, java.lang.Iterable, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path writeString(java.nio.file.Path, java.lang.CharSequence, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path writeString(java.nio.file.Path, java.lang.CharSequence, java.nio.file.OpenOption[])`
- `public static java.nio.file.attribute.BasicFileAttributes readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.FileAttributeView getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.FileTime getLastModifiedTime(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.UserPrincipal getOwner(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.util.List readAllLines(java.nio.file.Path)`
- `public static java.util.List readAllLines(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.util.Map readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])`
- `public static java.util.Set getPosixFilePermissions(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.util.stream.Stream find(java.nio.file.Path, int, java.util.function.BiPredicate, java.nio.file.FileVisitOption[])`
- `public static java.util.stream.Stream lines(java.nio.file.Path)`
- `public static java.util.stream.Stream lines(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.util.stream.Stream list(java.nio.file.Path)`
- `public static java.util.stream.Stream walk(java.nio.file.Path, int, java.nio.file.FileVisitOption[])`
- `public static java.util.stream.Stream walk(java.nio.file.Path, java.nio.file.FileVisitOption[])`
- `public static long copy(java.io.InputStream, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static long copy(java.nio.file.Path, java.io.OutputStream)`
- `public static long mismatch(java.nio.file.Path, java.nio.file.Path)`
- `public static long size(java.nio.file.Path)`
- `public static void delete(java.nio.file.Path)`


[← Back to Package](java_nio_file.md)
