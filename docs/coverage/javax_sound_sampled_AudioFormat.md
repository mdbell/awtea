# Class: `AudioFormat` ![Coverage](https://img.shields.io/badge/coverage-61.9%25-yellow)

**Full Name:** `javax.sound.sampled.AudioFormat`

**Coverage:** 13 / 21 (61.9%)

```
[██████████████████████████████░░░░░░░░░░░░░░░░░░░░] 61.9%
```

## ✓ Implemented Methods

- `public boolean isBigEndian()`
- `public boolean matches(javax.sound.sampled.AudioFormat)`
- `public float getFrameRate()`
- `public float getSampleRate()`
- `public int getChannels()`
- `public int getFrameSize()`
- `public int getSampleSizeInBits()`
- `public java.lang.String toString()`
- `public javax.sound.sampled.AudioFormat$Encoding getEncoding()`

## ✗ Missing Methods

- `public java.lang.Object getProperty(java.lang.String)`
- `public java.util.Map properties()`

## ✓ Implemented Fields

- `protected javax.sound.sampled.AudioFormat$Encoding encoding`

## ✗ Missing Fields

- `protected boolean bigEndian`
- `protected float frameRate`
- `protected float sampleRate`
- `protected int channels`
- `protected int frameSize`
- `protected int sampleSizeInBits`

## ✓ Implemented Constructors

- `public javax.sound.sampled.AudioFormat(float, int, int, boolean, boolean)`
- `public javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat$Encoding, float, int, int, int, float, boolean)`
- `public javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat$Encoding, float, int, int, int, float, boolean, java.util.Map)`


[← Back to Package](javax_sound_sampled.md)
