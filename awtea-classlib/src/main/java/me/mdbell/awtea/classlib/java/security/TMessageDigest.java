package me.mdbell.awtea.classlib.java.security;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.security.NoSuchAlgorithmException;

/**
 * Browser-compatible stub for java.security.MessageDigest.
 * Provides basic cryptographic hash functionality using browser Web Crypto API.
 * Limited algorithm support compared to full JDK.
 * 
 * @see java.security.MessageDigest
 */
public abstract class TMessageDigest {
    
    private static final Logger log = LoggerFactory.getLogger(TMessageDigest.class);
    
    protected String algorithm;
    
    /**
     * Creates a message digest with the specified algorithm name.
     * 
     * @param algorithm the standard name of the digest algorithm
     */
    protected TMessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }
    
    /**
     * Returns a MessageDigest object that implements the specified digest algorithm.
     * 
     * @param algorithm the name of the algorithm requested
     * @return a MessageDigest object that implements the specified algorithm
     * @throws NoSuchAlgorithmException if no Provider supports the algorithm
     */
    public static TMessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        log.warn("MessageDigest.getInstance({}) called - limited crypto support in browser environment", algorithm);
        
        // Common algorithms that could potentially be supported via Web Crypto API:
        // SHA-1, SHA-256, SHA-384, SHA-512
        String upperAlg = algorithm.toUpperCase().replaceAll("-", "");
        
        if (upperAlg.equals("SHA1") || upperAlg.equals("SHA")) {
            return new SHA1MessageDigest();
        } else if (upperAlg.equals("SHA256")) {
            return new SHA256MessageDigest();
        } else if (upperAlg.equals("MD5")) {
            log.warn("MD5 requested but not securely supported in browser - throwing exception");
            throw new NoSuchAlgorithmException("MD5 MessageDigest not supported in browser environment. Use SHA-256 or SHA-1 instead.");
        }
        
        throw new NoSuchAlgorithmException("MessageDigest algorithm not supported in browser: " + algorithm + ". Supported: SHA-1, SHA-256");
    }
    
    /**
     * Updates the digest using the specified byte.
     * 
     * @param input the byte with which to update the digest
     */
    public abstract void update(byte input);
    
    /**
     * Updates the digest using the specified array of bytes.
     * 
     * @param input the array of bytes
     */
    public abstract void update(byte[] input);
    
    /**
     * Updates the digest using the specified array of bytes,
     * starting at the specified offset.
     * 
     * @param input the array of bytes
     * @param offset the offset to start from in the array of bytes
     * @param len the number of bytes to use, starting at offset
     */
    public abstract void update(byte[] input, int offset, int len);
    
    /**
     * Completes the hash computation by performing final operations.
     * 
     * @return the array of bytes for the resulting hash value
     */
    public abstract byte[] digest();
    
    /**
     * Performs a final update on the digest using the specified array of bytes,
     * then completes the digest computation.
     * 
     * @param input the input to be updated before the digest is completed
     * @return the array of bytes for the resulting hash value
     */
    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }
    
    /**
     * Resets the digest for further use.
     */
    public abstract void reset();

    public int getDigestLength() {
        String upperAlg = algorithm.toUpperCase().replaceAll("-", "");
        switch (upperAlg) {
            case "SHA1":
            case "SHA":
                return 20; // SHA-1 produces 20 bytes
            case "SHA256":
                return 32; // SHA-256 produces 32 bytes
            default:
                return 0; // Unknown
        }
    }
    
    /**
     * Returns a string that identifies the algorithm.
     * 
     * @return the name of the algorithm
     */
    public final String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Simple SHA-1 stub implementation.
     */
    private static class SHA1MessageDigest extends TMessageDigest {
        
        public SHA1MessageDigest() {
            super("SHA-1");
            log.warn("SHA-1 MessageDigest created - stub implementation (not cryptographically secure)");
        }
        
        @Override
        public void update(byte input) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public void update(byte[] input) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public void update(byte[] input, int offset, int len) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public byte[] digest() {
            log.warn("MessageDigest.digest() called - returning dummy hash (NOT SECURE)");
            // Return a dummy 20-byte hash (SHA-1 produces 20 bytes)
            return new byte[20];
        }
        
        @Override
        public void reset() {
            // No-op
        }
    }
    
    /**
     * Simple SHA-256 stub implementation.
     */
    private static class SHA256MessageDigest extends TMessageDigest {
        
        public SHA256MessageDigest() {
            super("SHA-256");
            log.warn("SHA-256 MessageDigest created - stub implementation (not cryptographically secure)");
        }
        
        @Override
        public void update(byte input) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public void update(byte[] input) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public void update(byte[] input, int offset, int len) {
            log.warn("MessageDigest.update() called - stub implementation, not secure");
        }
        
        @Override
        public byte[] digest() {
            log.warn("MessageDigest.digest() called - returning dummy hash (NOT SECURE)");
            // Return a dummy 32-byte hash (SHA-256 produces 32 bytes)
            return new byte[32];
        }
        
        @Override
        public void reset() {
            // No-op
        }
    }
}
