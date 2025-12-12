# Class: `ClassLoader` ![Coverage](https://img.shields.io/badge/coverage-11.6%25-red)

**Full Name:** `java.lang.ClassLoader`

**Coverage:** 5 / 43 (11.6%)

```
[█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 11.6%
```

## ✓ Implemented Methods

- `public java.io.InputStream getResourceAsStream(java.lang.String)`
- `public static java.io.InputStream getSystemResourceAsStream(java.lang.String)`
- `public static java.lang.ClassLoader getSystemClassLoader()`

## ✗ Missing Methods

- `protected final java.lang.Class defineClass(byte[], int, int)`
- `protected final java.lang.Class defineClass(java.lang.String, byte[], int, int)`
- `protected final java.lang.Class defineClass(java.lang.String, byte[], int, int, java.security.ProtectionDomain)`
- `protected final java.lang.Class defineClass(java.lang.String, java.nio.ByteBuffer, java.security.ProtectionDomain)`
- `protected final java.lang.Class findLoadedClass(java.lang.String)`
- `protected final java.lang.Class findSystemClass(java.lang.String)`
- `protected final void resolveClass(java.lang.Class)`
- `protected final void setSigners(java.lang.Class, java.lang.Object[])`
- `protected java.lang.Class findClass(java.lang.String)`
- `protected java.lang.Class findClass(java.lang.String, java.lang.String)`
- `protected java.lang.Class loadClass(java.lang.String, boolean)`
- `protected java.lang.Object getClassLoadingLock(java.lang.String)`
- `protected java.lang.Package definePackage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.net.URL)`
- `protected java.lang.Package getPackage(java.lang.String)`
- `protected java.lang.Package[] getPackages()`
- `protected java.lang.String findLibrary(java.lang.String)`
- `protected java.net.URL findResource(java.lang.String)`
- `protected java.net.URL findResource(java.lang.String, java.lang.String)`
- `protected java.util.Enumeration findResources(java.lang.String)`
- `protected static boolean registerAsParallelCapable()`
- `public final boolean isRegisteredAsParallelCapable()`
- `public final java.lang.ClassLoader getParent()`
- `public final java.lang.Module getUnnamedModule()`
- `public final java.lang.Package getDefinedPackage(java.lang.String)`
- `public final java.lang.Package[] getDefinedPackages()`
- `public java.lang.Class loadClass(java.lang.String)`
- `public java.lang.String getName()`
- `public java.net.URL getResource(java.lang.String)`
- `public java.util.Enumeration getResources(java.lang.String)`
- `public java.util.stream.Stream resources(java.lang.String)`
- `public static java.lang.ClassLoader getPlatformClassLoader()`
- `public static java.net.URL getSystemResource(java.lang.String)`
- `public static java.util.Enumeration getSystemResources(java.lang.String)`
- `public void clearAssertionStatus()`
- `public void setClassAssertionStatus(java.lang.String, boolean)`
- `public void setDefaultAssertionStatus(boolean)`
- `public void setPackageAssertionStatus(java.lang.String, boolean)`

## ✓ Implemented Constructors

- `protected java.lang.ClassLoader()`
- `protected java.lang.ClassLoader(java.lang.ClassLoader)`

## ✗ Missing Constructors

- `protected java.lang.ClassLoader(java.lang.String, java.lang.ClassLoader)`


[← Back to Package](index.md)
