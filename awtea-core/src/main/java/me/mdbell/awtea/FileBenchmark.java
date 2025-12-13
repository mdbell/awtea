package me.mdbell.awtea;

import org.teavm.runtime.fs.VirtualFileSystemProvider;
import me.mdbell.awtea.impl.idb.IndexedDBHelper;
import me.mdbell.awtea.impl.idb.IndexedDBVirtualFileSystem;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class FileBenchmark {
    private static final String FILE_PATH = "benchmark_test.dat";
    private static final int FILE_SIZE_MB = 100; // File size in MB
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    public static void main(String[] args) throws IOException {

        // comment this out to use the default file system (in-memory)
        VirtualFileSystemProvider.setInstance(new IndexedDBVirtualFileSystem());

        // in case file is left over from previous run
        if (IndexedDBHelper.isFile("/" + FILE_PATH) && !IndexedDBHelper.deleteFile("/" + FILE_PATH)) {
            System.out.println("Failed to delete existing file");
            return;
        }

        System.out.println("File I/O Benchmark (File size: " + FILE_SIZE_MB + "MB)");

        System.out.println("Verifying file contents...");
        verifyFileContents();
        System.out.println("Benchmarking write speed...");
        long writeTime = benchmarkWrite();
        System.out.printf("Write Speed: %.2f MB/s\n", (FILE_SIZE_MB * 1000.0) / writeTime);
        System.out.println("Benchmarking read speed...");
        long readTime = benchmarkRead();
        System.out.printf("Read Speed: %.2f MB/s\n", (FILE_SIZE_MB * 1000.0) / readTime);

        IndexedDBHelper.deleteFile(FILE_PATH);
    }

    private static void verifyFileContents() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        new Random().nextBytes(buffer); // Fill buffer with random data

        int expectedChecksum = 0;

        // write the file
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            for (int i = 0; i < ((FILE_SIZE_MB / 2) * 1024 * 1024) / BUFFER_SIZE; i++) {
                fos.write(buffer);
                for (byte b : buffer) {
                    expectedChecksum += b;
                }
            }
        }

        // read the file again to verify the checksum
        int actualChecksum = 0;

        try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    actualChecksum += buffer[i];
                }
            }
        }

        if (expectedChecksum != actualChecksum) {
            throw new IOException("Checksum mismatch");
        }
    }

    private static long benchmarkWrite() throws IOException {
        byte[] data = new byte[BUFFER_SIZE];
        new Random().nextBytes(data); // Fill buffer with random data

        long startTime = System.nanoTime();
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            for (int i = 0; i < (FILE_SIZE_MB * 1024 * 1024) / BUFFER_SIZE; i++) {
                fos.write(data);
            }
        }
        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }

    private static long benchmarkRead() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        int calculatedChecksum = 0;

        long startTime = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
            while (fis.read(buffer) != -1) {
            }
        }

        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }
}
