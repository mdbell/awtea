# Class: `URLConnection` ![Coverage](https://img.shields.io/badge/coverage-80.3%25-green)

**Full Name:** `java.net.URLConnection`

**Coverage:** 49 / 61 (80.3%)

```
[████████████████████████████████████████░░░░░░░░░░] 80.3%
```

## ✓ Implemented Methods

- `public abstract void connect()`
- `public boolean getAllowUserInteraction()`
- `public boolean getDefaultUseCaches()`
- `public boolean getDoInput()`
- `public boolean getDoOutput()`
- `public boolean getUseCaches()`
- `public int getConnectTimeout()`
- `public int getContentLength()`
- `public int getHeaderFieldInt(java.lang.String, int)`
- `public int getReadTimeout()`
- `public java.io.InputStream getInputStream()`
- `public java.io.OutputStream getOutputStream()`
- `public java.lang.String getContentEncoding()`
- `public java.lang.String getContentType()`
- `public java.lang.String getHeaderField(int)`
- `public java.lang.String getHeaderField(java.lang.String)`
- `public java.lang.String getHeaderFieldKey(int)`
- `public java.lang.String getRequestProperty(java.lang.String)`
- `public java.lang.String toString()`
- `public java.net.URL getURL()`
- `public java.util.Map getHeaderFields()`
- `public java.util.Map getRequestProperties()`
- `public long getDate()`
- `public long getExpiration()`
- `public long getHeaderFieldDate(java.lang.String, long)`
- `public long getIfModifiedSince()`
- `public long getLastModified()`
- `public static boolean getDefaultAllowUserInteraction()`
- `public static java.lang.String getDefaultRequestProperty(java.lang.String)`
- `public static void setDefaultAllowUserInteraction(boolean)`
- `public static void setDefaultRequestProperty(java.lang.String, java.lang.String)`
- `public void addRequestProperty(java.lang.String, java.lang.String)`
- `public void setAllowUserInteraction(boolean)`
- `public void setConnectTimeout(int)`
- `public void setDefaultUseCaches(boolean)`
- `public void setDoInput(boolean)`
- `public void setDoOutput(boolean)`
- `public void setIfModifiedSince(long)`
- `public void setReadTimeout(int)`
- `public void setRequestProperty(java.lang.String, java.lang.String)`
- `public void setUseCaches(boolean)`

## ✗ Missing Methods

- `public java.lang.Object getContent()`
- `public java.lang.Object getContent(java.lang.Class[])`
- `public java.security.Permission getPermission()`
- `public long getContentLengthLong()`
- `public long getHeaderFieldLong(java.lang.String, long)`
- `public static boolean getDefaultUseCaches(java.lang.String)`
- `public static java.lang.String guessContentTypeFromName(java.lang.String)`
- `public static java.lang.String guessContentTypeFromStream(java.io.InputStream)`
- `public static java.net.FileNameMap getFileNameMap()`
- `public static void setContentHandlerFactory(java.net.ContentHandlerFactory)`
- `public static void setDefaultUseCaches(java.lang.String, boolean)`
- `public static void setFileNameMap(java.net.FileNameMap)`

## ✓ Implemented Fields

- `protected boolean allowUserInteraction`
- `protected boolean connected`
- `protected boolean doInput`
- `protected boolean doOutput`
- `protected boolean useCaches`
- `protected java.net.URL url`
- `protected long ifModifiedSince`

## ✓ Implemented Constructors

- `protected java.net.URLConnection(java.net.URL)`


[← Back to Package](index.md)
