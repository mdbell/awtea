# Class: `Thread` ![Coverage](https://img.shields.io/badge/coverage-51.9%25-yellow)

**Full Name:** `java.lang.Thread`

**Coverage:** 28 / 54 (51.9%)

```
[█████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░] 51.9%
```

## ✓ Implemented Methods

- `public boolean isInterrupted()`
- `public final boolean isDaemon()`
- `public final int getPriority()`
- `public final void join()`
- `public final void join(long)`
- `public final void join(long, int)`
- `public final void setDaemon(boolean)`
- `public final void setPriority(int)`
- `public java.lang.ClassLoader getContextClassLoader()`
- `public java.lang.StackTraceElement[] getStackTrace()`
- `public java.lang.Thread$UncaughtExceptionHandler getUncaughtExceptionHandler()`
- `public long getId()`
- `public static boolean holdsLock(java.lang.Object)`
- `public static boolean interrupted()`
- `public static int activeCount()`
- `public static java.lang.Thread currentThread()`
- `public static java.lang.Thread$UncaughtExceptionHandler getDefaultUncaughtExceptionHandler()`
- `public static void setDefaultUncaughtExceptionHandler(java.lang.Thread$UncaughtExceptionHandler)`
- `public static void sleep(long)`
- `public static void yield()`
- `public void interrupt()`
- `public void run()`
- `public void setUncaughtExceptionHandler(java.lang.Thread$UncaughtExceptionHandler)`
- `public void start()`

## ✗ Missing Methods

- `protected java.lang.Object clone()`
- `public final boolean isAlive()`
- `public final java.lang.String getName()`
- `public final java.lang.ThreadGroup getThreadGroup()`
- `public final void checkAccess()`
- `public final void resume()`
- `public final void setName(java.lang.String)`
- `public final void stop()`
- `public final void suspend()`
- `public int countStackFrames()`
- `public java.lang.String toString()`
- `public java.lang.Thread$State getState()`
- `public static int enumerate(java.lang.Thread[])`
- `public static java.util.Map getAllStackTraces()`
- `public static void dumpStack()`
- `public static void onSpinWait()`
- `public static void sleep(long, int)`
- `public void setContextClassLoader(java.lang.ClassLoader)`

## ✗ Missing Fields

- `public static final int MAX_PRIORITY`
- `public static final int MIN_PRIORITY`
- `public static final int NORM_PRIORITY`

## ✓ Implemented Constructors

- `public java.lang.Thread()`
- `public java.lang.Thread(java.lang.Runnable)`
- `public java.lang.Thread(java.lang.Runnable, java.lang.String)`
- `public java.lang.Thread(java.lang.String)`

## ✗ Missing Constructors

- `public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable)`
- `public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String)`
- `public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String, long)`
- `public java.lang.Thread(java.lang.ThreadGroup, java.lang.Runnable, java.lang.String, long, boolean)`
- `public java.lang.Thread(java.lang.ThreadGroup, java.lang.String)`


[← Back to Package](index.md)
