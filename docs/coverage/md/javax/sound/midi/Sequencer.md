# Class: `Sequencer` ![Coverage](https://img.shields.io/badge/coverage-11.4%25-red)

**Full Name:** `javax.sound.midi.Sequencer`

**Coverage:** 5 / 44 (11.4%)

```
[█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 11.4%
```

## ✓ Implemented Methods

- `public abstract void setLoopCount(int)`
- `public abstract void setSequence(javax.sound.midi.Sequence)`
- `public abstract void start()`
- `public abstract void stop()`

## ✗ Missing Methods

- `public abstract boolean addMetaEventListener(javax.sound.midi.MetaEventListener)`
- `public abstract boolean getTrackMute(int)`
- `public abstract boolean getTrackSolo(int)`
- `public abstract boolean isRecording()`
- `public abstract boolean isRunning()`
- `public abstract float getTempoFactor()`
- `public abstract float getTempoInBPM()`
- `public abstract float getTempoInMPQ()`
- `public abstract int getLoopCount()`
- `public abstract int[] addControllerEventListener(javax.sound.midi.ControllerEventListener, int[])`
- `public abstract int[] removeControllerEventListener(javax.sound.midi.ControllerEventListener, int[])`
- `public abstract javax.sound.midi.Sequence getSequence()`
- `public abstract javax.sound.midi.Sequencer$SyncMode getMasterSyncMode()`
- `public abstract javax.sound.midi.Sequencer$SyncMode getSlaveSyncMode()`
- `public abstract javax.sound.midi.Sequencer$SyncMode[] getMasterSyncModes()`
- `public abstract javax.sound.midi.Sequencer$SyncMode[] getSlaveSyncModes()`
- `public abstract long getLoopEndPoint()`
- `public abstract long getLoopStartPoint()`
- `public abstract long getMicrosecondLength()`
- `public abstract long getMicrosecondPosition()`
- `public abstract long getTickLength()`
- `public abstract long getTickPosition()`
- `public abstract void recordDisable(javax.sound.midi.Track)`
- `public abstract void recordEnable(javax.sound.midi.Track, int)`
- `public abstract void removeMetaEventListener(javax.sound.midi.MetaEventListener)`
- `public abstract void setLoopEndPoint(long)`
- `public abstract void setLoopStartPoint(long)`
- `public abstract void setMasterSyncMode(javax.sound.midi.Sequencer$SyncMode)`
- `public abstract void setMicrosecondPosition(long)`
- `public abstract void setSequence(java.io.InputStream)`
- `public abstract void setSlaveSyncMode(javax.sound.midi.Sequencer$SyncMode)`
- `public abstract void setTempoFactor(float)`
- `public abstract void setTempoInBPM(float)`
- `public abstract void setTempoInMPQ(float)`
- `public abstract void setTickPosition(long)`
- `public abstract void setTrackMute(int, boolean)`
- `public abstract void setTrackSolo(int, boolean)`
- `public abstract void startRecording()`
- `public abstract void stopRecording()`

## ✓ Implemented Fields

- `public static final int LOOP_CONTINUOUSLY`


[← Back to Package](index.md)
