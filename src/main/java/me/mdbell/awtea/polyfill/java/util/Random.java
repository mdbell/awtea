package me.mdbell.awtea.polyfill.java.util;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Not actually deprecated, but marked so to remind
 * us to use the base java.util.Random - and only use this class
 * internally
 *
 * @see java.util.Random
 */
@Deprecated
public class Random{
    /**
     * State for Xorshift64 PRNG
     */
    private long state;

    /**
     * A stored gaussian value for nextGaussian()
     */
    private double storedGaussian;

    /**
     * Whether storedGaussian value is valid
     */
    private boolean haveStoredGaussian;

    public Random() {
        this(System.currentTimeMillis()); // Default seed based on time
    }

    public Random(long seed) {
        setSeed(seed);
    }

    public void setSeed(long seed) {
        // Prevent zero state (as Xorshift requires a non-zero state)
        state = (seed == 0) ? 0xdeadbeefL : seed;
    }

    protected int next(int bits) {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return (int) (state >>> (64 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Math.abs(nextInt()) % n;
    }

    public boolean nextBoolean() {
        return (nextInt() & 1) != 0;
    }

    public void nextBytes(byte[] bytes) {
        int index = 0;
        while (index < bytes.length) {
            int rand = nextInt();
            for (int i = 0; i < 4 && index < bytes.length; i++) {
                bytes[index++] = (byte) (rand & 0xFF);
                rand >>= 8;
            }
        }
    }

    public long nextLong() {
        return ((long) next(32) << 32) | (next(32) & 0xFFFFFFFFL);
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return ((long) next(26) << 27 | next(27)) / (double) (1L << 53);
    }

    /**
     * Generate a random number with Gaussian distribution:
     * centered around 0 with a standard deviation of 1.0.
     */
    public double nextGaussian() {
        if (haveStoredGaussian) {
            haveStoredGaussian = false;
            return storedGaussian;
        }

        double u, v, s;
        do {
            u = 2.0 * nextDouble() - 1.0;
            v = 2.0 * nextDouble() - 1.0;
            s = u * u + v * v;
        } while (s >= 1 || s == 0);

        double multiplier = Math.sqrt(-2.0 * Math.log(s) / s);
        storedGaussian = v * multiplier;
        haveStoredGaussian = true;

        return u * multiplier;
    }

    public DoubleStream doubles() {
        return DoubleStream.generate(this::nextDouble);
    }

    public DoubleStream doubles(long streamSize) {
        return DoubleStream.generate(this::nextDouble).limit(streamSize);
    }

    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        return DoubleStream.generate(() -> randomNumberOrigin + nextDouble() * (randomNumberBound - randomNumberOrigin));
    }

    public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return DoubleStream.generate(() -> randomNumberOrigin + nextDouble() * (randomNumberBound - randomNumberOrigin)).limit(streamSize);
    }

    public IntStream ints() {
        return IntStream.generate(this::nextInt);
    }

    public IntStream ints(long streamSize) {
        return IntStream.generate(this::nextInt).limit(streamSize);
    }

    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        return IntStream.generate(() -> randomNumberOrigin + nextInt(randomNumberBound - randomNumberOrigin));
    }

    public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        return IntStream.generate(() -> randomNumberOrigin + nextInt(randomNumberBound - randomNumberOrigin)).limit(streamSize);
    }

    public LongStream longs() {
        return LongStream.generate(this::nextLong);
    }

    public LongStream longs(long streamSize) {
        return LongStream.generate(this::nextLong).limit(streamSize);
    }

    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        return LongStream.generate(() -> randomNumberOrigin + Math.abs(nextLong()) % (randomNumberBound - randomNumberOrigin));
    }

    public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return LongStream.generate(() -> randomNumberOrigin + Math.abs(nextLong()) % (randomNumberBound - randomNumberOrigin)).limit(streamSize);
    }
}
