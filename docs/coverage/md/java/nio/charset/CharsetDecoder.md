# Class: `CharsetDecoder` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Full Name:** `java.nio.charset.CharsetDecoder`

**Coverage:** 23 / 23 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

## ✓ Implemented Methods

- `protected abstract java.nio.charset.CoderResult decodeLoop(java.nio.ByteBuffer, java.nio.CharBuffer)`
- `protected java.nio.charset.CoderResult implFlush(java.nio.CharBuffer)`
- `protected void implOnMalformedInput(java.nio.charset.CodingErrorAction)`
- `protected void implOnUnmappableCharacter(java.nio.charset.CodingErrorAction)`
- `protected void implReplaceWith(java.lang.String)`
- `protected void implReset()`
- `public boolean isAutoDetecting()`
- `public boolean isCharsetDetected()`
- `public final float averageCharsPerByte()`
- `public final float maxCharsPerByte()`
- `public final java.lang.String replacement()`
- `public final java.nio.CharBuffer decode(java.nio.ByteBuffer)`
- `public final java.nio.charset.Charset charset()`
- `public final java.nio.charset.CharsetDecoder onMalformedInput(java.nio.charset.CodingErrorAction)`
- `public final java.nio.charset.CharsetDecoder onUnmappableCharacter(java.nio.charset.CodingErrorAction)`
- `public final java.nio.charset.CharsetDecoder replaceWith(java.lang.String)`
- `public final java.nio.charset.CharsetDecoder reset()`
- `public final java.nio.charset.CoderResult decode(java.nio.ByteBuffer, java.nio.CharBuffer, boolean)`
- `public final java.nio.charset.CoderResult flush(java.nio.CharBuffer)`
- `public java.nio.charset.Charset detectedCharset()`
- `public java.nio.charset.CodingErrorAction malformedInputAction()`
- `public java.nio.charset.CodingErrorAction unmappableCharacterAction()`

## ✓ Implemented Constructors

- `protected java.nio.charset.CharsetDecoder(java.nio.charset.Charset, float, float)`


[← Back to Package](index.md)
