# Class: `AudioSystem` ![Coverage](https://img.shields.io/badge/coverage-9.4%25-red)

**Full Name:** `javax.sound.sampled.AudioSystem`

**Coverage:** 3 / 32 (9.4%)

```
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 9.4%
```

## ✓ Implemented Methods

- `public static javax.sound.sampled.Line getLine(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.SourceDataLine getSourceDataLine(javax.sound.sampled.AudioFormat)`

## ✗ Missing Methods

- `public static boolean isConversionSupported(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioFormat)`
- `public static boolean isConversionSupported(javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFormat)`
- `public static boolean isFileTypeSupported(javax.sound.sampled.AudioFileFormat$Type)`
- `public static boolean isFileTypeSupported(javax.sound.sampled.AudioFileFormat$Type, javax.sound.sampled.AudioInputStream)`
- `public static boolean isLineSupported(javax.sound.sampled.Line$Info)`
- `public static int write(javax.sound.sampled.AudioInputStream, javax.sound.sampled.AudioFileFormat$Type, java.io.File)`
- `public static int write(javax.sound.sampled.AudioInputStream, javax.sound.sampled.AudioFileFormat$Type, java.io.OutputStream)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.io.File)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.io.InputStream)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.net.URL)`
- `public static javax.sound.sampled.AudioFileFormat$Type[] getAudioFileTypes()`
- `public static javax.sound.sampled.AudioFileFormat$Type[] getAudioFileTypes(javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.AudioFormat$Encoding[] getTargetEncodings(javax.sound.sampled.AudioFormat$Encoding)`
- `public static javax.sound.sampled.AudioFormat$Encoding[] getTargetEncodings(javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.AudioFormat[] getTargetFormats(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.io.File)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.io.InputStream)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.net.URL)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.Clip getClip()`
- `public static javax.sound.sampled.Clip getClip(javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.Line$Info[] getSourceLineInfo(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.Line$Info[] getTargetLineInfo(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.Mixer getMixer(javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.Mixer$Info[] getMixerInfo()`
- `public static javax.sound.sampled.SourceDataLine getSourceDataLine(javax.sound.sampled.AudioFormat, javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.TargetDataLine getTargetDataLine(javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.TargetDataLine getTargetDataLine(javax.sound.sampled.AudioFormat, javax.sound.sampled.Mixer$Info)`

## ✓ Implemented Fields

- `public static final int NOT_SPECIFIED`


[← Back to Package](index.md)
