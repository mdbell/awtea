# Class: `SecureRandom` ![Coverage](https://img.shields.io/badge/coverage-39.1%25-orange)

**Full Name:** `java.security.SecureRandom`

**Coverage:** 9 / 23 (39.1%)

```
[███████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 39.1%
```

## ✓ Implemented Methods

- `public byte[] generateSeed(int)`
- `public java.lang.String getAlgorithm()`
- `public static byte[] getSeed(int)`
- `public static java.security.SecureRandom getInstance(java.lang.String)`
- `public void nextBytes(byte[])`
- `public void reseed()`
- `public void setSeed(byte[])`

## ✗ Missing Methods

- `protected final int next(int)`
- `public final java.security.Provider getProvider()`
- `public java.lang.String toString()`
- `public java.security.SecureRandomParameters getParameters()`
- `public static java.security.SecureRandom getInstance(java.lang.String, java.lang.String)`
- `public static java.security.SecureRandom getInstance(java.lang.String, java.security.Provider)`
- `public static java.security.SecureRandom getInstance(java.lang.String, java.security.SecureRandomParameters)`
- `public static java.security.SecureRandom getInstance(java.lang.String, java.security.SecureRandomParameters, java.lang.String)`
- `public static java.security.SecureRandom getInstance(java.lang.String, java.security.SecureRandomParameters, java.security.Provider)`
- `public static java.security.SecureRandom getInstanceStrong()`
- `public void nextBytes(byte[], java.security.SecureRandomParameters)`
- `public void reseed(java.security.SecureRandomParameters)`
- `public void setSeed(long)`

## ✓ Implemented Constructors

- `public java.security.SecureRandom()`
- `public java.security.SecureRandom(byte[])`

## ✗ Missing Constructors

- `protected java.security.SecureRandom(java.security.SecureRandomSpi, java.security.Provider)`


[← Back to Package](index.md)
