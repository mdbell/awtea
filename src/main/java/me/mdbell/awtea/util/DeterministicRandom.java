package me.mdbell.awtea.util;

import java.util.Random;

public class DeterministicRandom extends Random {
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

    public DeterministicRandom() {
        this(System.currentTimeMillis()); // Default seed based on time
    }

    public DeterministicRandom(long seed) {
        setSeed(seed);
    }

    public void setSeed(long seed) {
        // Prevent zero state (as Xorshift requires a non-zero state)
        state = (seed == 0) ? 0xdeadbeefL : seed;
    }

    private int nextBits(int bits) {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return (int) (state >>> (64 - bits));
    }

    @Override
    public int nextInt() {
        return nextBits(32);
    }

    @Override
    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Math.abs(nextInt()) % n;
    }

    @Override
    public long nextLong() {
        return ((long) nextBits(32) << 32) | (nextBits(32) & 0xFFFFFFFFL);
    }

    @Override
    public float nextFloat() {
        return nextBits(24) / ((float) (1 << 24));
    }

    @Override
    public double nextDouble() {
        return ((long) nextBits(26) << 27 | nextBits(27)) / (double) (1L << 53);
    }

    /**
     * Generate a random number with Gaussian distribution:
     * centered around 0 with a standard deviation of 1.0.
     */
    @Override
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
}
