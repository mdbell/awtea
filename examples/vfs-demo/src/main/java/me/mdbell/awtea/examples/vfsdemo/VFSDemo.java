package me.mdbell.awtea.examples.vfsdemo;

import me.mdbell.awtea.impl.idb2.IndexedDBVirtualFileSystem2;
import me.mdbell.awtea.impl.idb2.VFSStats;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

import java.io.*;
import java.util.Random;

/**
 * Demo of IndexedDB VFS v2 with support for large files (100MB+).
 * 
 * This example demonstrates:
 * - Setting up the VFS provider for use with java.io.* classes
 * - Writing and reading files using standard Java I/O
 * - Random access file operations
 * - Performance monitoring with VFSStats
 * - Large file handling with lazy chunk loading
 */
public class VFSDemo {
    
    private static final Logger log = LoggerFactory.getLogger(VFSDemo.class);
    
    public static void main(String[] args) {
        try {
            log.info("=== IndexedDB VFS v2 Demo ===");
            
            // Set up the VFS provider - this enables java.io.* classes to use IndexedDB
            IndexedDBVirtualFileSystem2 vfs = new IndexedDBVirtualFileSystem2();
            VirtualFileSystemProvider.setInstance(vfs);
            
            log.info("VFS provider initialized");
            
            // Demo 1: Basic file operations
            basicFileOperations();
            
            // Demo 2: Random access file operations
            randomAccessOperations();
            
            // Demo 3: Large file handling
            largeFileOperations();
            
            // Demo 4: Performance statistics
            showStatistics(vfs);
            
            log.info("=== Demo Complete ===");
            
        } catch (Exception e) {
            log.error("Demo failed", e);
        }
    }
    
    /**
     * Demonstrates basic file write and read operations using standard java.io classes
     */
    private static void basicFileOperations() throws IOException {
        log.info("\n--- Basic File Operations ---");
        
        String filename = "/home/demo.txt";
        String content = "Hello from IndexedDB VFS v2!\nThis file is stored in your browser's IndexedDB.";
        
        // Write to file
        log.info("Writing to {}", filename);
        try (FileOutputStream fos = new FileOutputStream(filename);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(content);
        }
        log.info("File written successfully");
        
        // Read from file
        log.info("Reading from {}", filename);
        try (FileInputStream fis = new FileInputStream(filename);
             InputStreamReader reader = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(reader)) {
            
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            log.info("File contents:\n{}", sb.toString().trim());
        }
        
        // Check file size
        File file = new File(filename);
        log.info("File size: {} bytes", file.length());
    }
    
    /**
     * Demonstrates random access file operations with seeks
     */
    private static void randomAccessOperations() throws IOException {
        log.info("\n--- Random Access Operations ---");
        
        String filename = "/home/random_access.dat";
        
        // Create a file with structured data
        log.info("Creating structured file with random access...");
        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
            // Write header
            raf.writeUTF("DATA_FILE_V1");
            
            // Write 10 records at known positions
            for (int i = 0; i < 10; i++) {
                long position = 100 + (i * 20); // Each record at 20-byte intervals
                raf.seek(position);
                raf.writeInt(i);
                raf.writeDouble(i * 3.14159);
            }
            
            log.info("Wrote 10 records to file");
        }
        
        // Read records in random order
        log.info("Reading records in random order...");
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            // Read header
            String header = raf.readUTF();
            log.info("Header: {}", header);
            
            // Read records 7, 3, 9 (random access)
            int[] indices = {7, 3, 9};
            for (int idx : indices) {
                long position = 100 + (idx * 20);
                raf.seek(position);
                int id = raf.readInt();
                double value = raf.readDouble();
                log.info("Record {}: id={}, value={}", idx, id, String.format("%.5f", value));
            }
        }
    }
    
    /**
     * Demonstrates large file handling with lazy chunk loading
     */
    private static void largeFileOperations() throws IOException {
        log.info("\n--- Large File Operations ---");
        
        String filename = "/home/large_file.dat";
        int fileSizeMB = 5; // Use 5MB for demo (can handle 100MB+)
        int bufferSize = 8192; // 8KB buffer
        
        log.info("Creating {}MB file...", fileSizeMB);
        
        // Write large file
        byte[] buffer = new byte[bufferSize];
        new Random(42).nextBytes(buffer); // Deterministic random data
        
        long startWrite = System.currentTimeMillis();
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            int chunks = (fileSizeMB * 1024 * 1024) / bufferSize;
            for (int i = 0; i < chunks; i++) {
                fos.write(buffer);
                
                // Log progress every 1MB
                if (i % (128) == 0) {
                    log.info("Written {} MB", (i * bufferSize) / (1024 * 1024));
                }
            }
        }
        long writeTime = System.currentTimeMillis() - startWrite;
        log.info("Write completed in {}ms ({} MB/s)", 
                writeTime, 
                String.format("%.2f", (fileSizeMB * 1000.0) / writeTime));
        
        // Read large file
        log.info("Reading {}MB file...", fileSizeMB);
        long startRead = System.currentTimeMillis();
        long bytesRead = 0;
        
        try (FileInputStream fis = new FileInputStream(filename)) {
            byte[] readBuffer = new byte[bufferSize];
            int read;
            while ((read = fis.read(readBuffer)) != -1) {
                bytesRead += read;
            }
        }
        long readTime = System.currentTimeMillis() - startRead;
        log.info("Read {} MB in {}ms ({} MB/s)", 
                bytesRead / (1024 * 1024),
                readTime,
                String.format("%.2f", (fileSizeMB * 1000.0) / readTime));
        
        // Demonstrate random access on large file
        log.info("Testing random access on large file...");
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            // Jump to middle of file
            long middlePos = (fileSizeMB * 1024 * 1024L) / 2;
            raf.seek(middlePos);
            byte[] sample = new byte[16];
            raf.read(sample);
            log.info("Read 16 bytes from position {}: first byte = {}", middlePos, sample[0]);
        }
        
        // Clean up
        File file = new File(filename);
        file.delete();
        log.info("Large file demo complete (file deleted)");
    }
    
    /**
     * Shows VFS performance statistics
     */
    private static void showStatistics(IndexedDBVirtualFileSystem2 vfs) {
        log.info("\n--- VFS Performance Statistics ---");
        
        VFSStats stats = vfs.getStats();
        log.info("Total reads: {}", stats.getReads());
        log.info("Total writes: {}", stats.getWrites());
        log.info("Bytes read: {} ({} MB)", 
                stats.getBytesRead(), 
                stats.getBytesRead() / (1024 * 1024));
        log.info("Bytes written: {} ({} MB)", 
                stats.getBytesWritten(),
                stats.getBytesWritten() / (1024 * 1024));
        log.info("Cache hits: {}", stats.getCacheHits());
        log.info("Cache misses: {}", stats.getCacheMisses());
        log.info("Cache hit ratio: {}%", 
                String.format("%.1f", stats.getCacheHitRatio() * 100));
        log.info("DB operations: {}", stats.getDbOperations());
        log.info("Errors: {}", stats.getErrors());
        log.info("Uptime: {}ms", stats.getUptime());
    }
}
