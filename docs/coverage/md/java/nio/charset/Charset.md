# Class: `Charset` ![Coverage](https://img.shields.io/badge/coverage-68.2%25-yellow)

**Full Name:** `java.nio.charset.Charset`

**Coverage:** 15 / 22 (68.2%)

```
[██████████████████████████████████░░░░░░░░░░░░░░░░] 68.2%
```

## ✓ Implemented Methods

- `public abstract boolean contains(java.nio.charset.Charset)`
- `public abstract java.nio.charset.CharsetDecoder newDecoder()`
- `public abstract java.nio.charset.CharsetEncoder newEncoder()`
- `public boolean canEncode()`
- `public final int compareTo(java.nio.charset.Charset)`
- `public final java.lang.String name()`
- `public final java.nio.ByteBuffer encode(java.lang.String)`
- `public final java.nio.ByteBuffer encode(java.nio.CharBuffer)`
- `public final java.nio.CharBuffer decode(java.nio.ByteBuffer)`
- `public final java.util.Set aliases()`
- `public int compareTo(java.lang.Object)`
- `public java.lang.String displayName()`
- `public static java.nio.charset.Charset defaultCharset()`
- `public static java.nio.charset.Charset forName(java.lang.String)`

## ✗ Missing Methods

- `public final boolean equals(java.lang.Object)`
- `public final boolean isRegistered()`
- `public final int hashCode()`
- `public final java.lang.String toString()`
- `public java.lang.String displayName(java.util.Locale)`
- `public static boolean isSupported(java.lang.String)`
- `public static java.util.SortedMap availableCharsets()`

## ✓ Implemented Constructors

- `protected java.nio.charset.Charset(java.lang.String, java.lang.String[])`


[← Back to Package](index.md)
