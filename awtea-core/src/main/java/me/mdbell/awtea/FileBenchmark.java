package me.mdbell.awtea;

import me.mdbell.awtea.impl.idb.IndexedDBHelper;
import me.mdbell.awtea.io.opfs.OPFSVirtualFileSystem;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.runtime.fs.VirtualFileSystemProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class FileBenchmark {

    private static final Logger log = LoggerFactory.getLogger(FileBenchmark.class);

    private static final String FILE_PATH = "benchmark_test.dat";
    private static final int FILE_SIZE_MB = 100; // File size in MB
    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    public static void main(String[] args) throws IOException {

        // comment this out to use the default file system (in-memory)
        VirtualFileSystemProvider.setInstance(new OPFSVirtualFileSystem());
//        VirtualFileSystemProvider.setInstance(new IndexedDBVirtualFileSystem());
        // in case file is left over from previous run
        if (IndexedDBHelper.isFile("/" + FILE_PATH) && !IndexedDBHelper.deleteFile("/" + FILE_PATH)) {
            log.info("Failed to delete existing file");
            return;
        }

        log.info("File I/O Benchmark (File size: {}MB)", FILE_SIZE_MB);

        log.info("Verifying file contents...");
        verifyFileContents();
        log.info("Benchmarking write speed...");
        long writeTime = benchmarkWrite();
        log.info("Write Speed: {} MB/s", String.format("%.2f", (FILE_SIZE_MB * 1000.0) / writeTime));
        log.info("Benchmarking read speed...");
        long readTime = benchmarkRead();
        log.info("Read Speed: {} MB/s", String.format("%.2f", (FILE_SIZE_MB * 1000.0) / readTime));

        IndexedDBHelper.deleteFile(FILE_PATH);
    }

    private static void verifyFileContents() throws IOException {
        byte[] expected = new byte[BUFFER_SIZE];
        byte[] buffer = new byte[BUFFER_SIZE];
        new Random().nextBytes(expected); // Fill buffer with random data

        int expectedChecksum = 0;

        File f = new File(FILE_PATH);
        if (f.exists()) {
            log.info("Deleting existing file for verification...");
            f.delete();
        }

        // write the file
        System.out.println("Writing file for verification...");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            for (int i = 0; i < ((FILE_SIZE_MB / 2) * 1024 * 1024) / BUFFER_SIZE; i++) {
                fos.write(expected);
                for (byte b : expected) {
                    expectedChecksum += b;
                }
            }
        }

        // read the file again to verify the checksum
        int actualChecksum = 0;

        System.out.println("Reading file for verification...");
        try (FileInputStream fis = new FileInputStream(f)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    actualChecksum += buffer[i];
                    if (buffer[i % BUFFER_SIZE] != expected[i % BUFFER_SIZE]) {
                        throw new IOException("Data mismatch at byte " + i + ". Expected: " + expected[i % BUFFER_SIZE]
                                + ", Actual: " + buffer[i % BUFFER_SIZE]);
                    }
                }
                Arrays.fill(buffer, (byte) 0); // Clear buffer for next read
            }
        }

        if (expectedChecksum != actualChecksum) {
            throw new IOException("Checksum mismatch. Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
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

        long startTime = System.nanoTime();
        try (FileInputStream fis = new FileInputStream(FILE_PATH)) {
            while (fis.read(buffer) != -1) {
            }
        }

        return (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
    }
}
