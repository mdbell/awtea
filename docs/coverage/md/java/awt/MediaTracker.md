# Class: `MediaTracker` ![Coverage](https://img.shields.io/badge/coverage-62.5%25-yellow)

**Full Name:** `java.awt.MediaTracker`

**Coverage:** 15 / 24 (62.5%)

```
[███████████████████████████████░░░░░░░░░░░░░░░░░░░] 62.5%
```

## ✓ Implemented Methods

- `public boolean checkAll()`
- `public boolean checkID(int)`
- `public boolean isErrorAny()`
- `public boolean isErrorID(int)`
- `public int statusAll(boolean)`
- `public int statusID(int, boolean)`
- `public void addImage(java.awt.Image, int)`
- `public void addImage(java.awt.Image, int, int, int)`
- `public void waitForAll()`
- `public void waitForID(int)`

## ✗ Missing Methods

- `public boolean checkAll(boolean)`
- `public boolean checkID(int, boolean)`
- `public boolean waitForAll(long)`
- `public boolean waitForID(int, long)`
- `public java.lang.Object[] getErrorsAny()`
- `public java.lang.Object[] getErrorsID(int)`
- `public void removeImage(java.awt.Image)`
- `public void removeImage(java.awt.Image, int)`
- `public void removeImage(java.awt.Image, int, int, int)`

## ✓ Implemented Fields

- `public static final int ABORTED`
- `public static final int COMPLETE`
- `public static final int ERRORED`
- `public static final int LOADING`

## ✓ Implemented Constructors

- `public java.awt.MediaTracker(java.awt.Component)`


[← Back to Package](index.md)
