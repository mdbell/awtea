# Class: `CharsetEncoder` ![Coverage](https://img.shields.io/badge/coverage-95.8%25-green)

**Full Name:** `java.nio.charset.CharsetEncoder`

**Coverage:** 23 / 24 (95.8%)

```
[███████████████████████████████████████████████░░░] 95.8%
```

## ✓ Implemented Methods

- `protected abstract java.nio.charset.CoderResult encodeLoop(java.nio.CharBuffer, java.nio.ByteBuffer)`
- `protected java.nio.charset.CoderResult implFlush(java.nio.ByteBuffer)`
- `protected void implOnMalformedInput(java.nio.charset.CodingErrorAction)`
- `protected void implOnUnmappableCharacter(java.nio.charset.CodingErrorAction)`
- `protected void implReplaceWith(byte[])`
- `protected void implReset()`
- `public boolean canEncode(char)`
- `public boolean canEncode(java.lang.CharSequence)`
- `public final byte[] replacement()`
- `public final float averageBytesPerChar()`
- `public final float maxBytesPerChar()`
- `public final java.nio.ByteBuffer encode(java.nio.CharBuffer)`
- `public final java.nio.charset.Charset charset()`
- `public final java.nio.charset.CharsetEncoder onMalformedInput(java.nio.charset.CodingErrorAction)`
- `public final java.nio.charset.CharsetEncoder onUnmappableCharacter(java.nio.charset.CodingErrorAction)`
- `public final java.nio.charset.CharsetEncoder replaceWith(byte[])`
- `public final java.nio.charset.CharsetEncoder reset()`
- `public final java.nio.charset.CoderResult encode(java.nio.CharBuffer, java.nio.ByteBuffer, boolean)`
- `public final java.nio.charset.CoderResult flush(java.nio.ByteBuffer)`
- `public java.nio.charset.CodingErrorAction malformedInputAction()`
- `public java.nio.charset.CodingErrorAction unmappableCharacterAction()`

## ✗ Missing Methods

- `public boolean isLegalReplacement(byte[])`

## ✓ Implemented Constructors

- `protected java.nio.charset.CharsetEncoder(java.nio.charset.Charset, float, float)`
- `protected java.nio.charset.CharsetEncoder(java.nio.charset.Charset, float, float, byte[])`


[← Back to Package](index.md)
