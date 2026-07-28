package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.AllowUnmatched;
import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

@NoDetours
@DetourReceiver(target = RandomAccessFile.class)
public class RandomAccessFileDetour {

    private static final Logger log = LoggerFactory.getLogger(RandomAccessFileDetour.class);

    @DetourMethod("<init>")
    public static RandomAccessFile open(java.io.File file, String mode) throws Exception {
        if (mode.contains("w")) {
            // Ensure the file exists when opened in write mode
            if (!file.exists() && !file.createNewFile()) {
                log.error("Failed to create file: {}", file.getAbsolutePath());
                throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
            }
        }

        return new RandomAccessFile(file, mode);
    }

    /**
     * Guard, not a replacement: TeaVM has no {@link FileChannel}, so this
     * exists to turn a would-be silent misbehaviour into an immediate throw.
     * Binding nothing means nothing calls it, which is the outcome we want.
     */
    @AllowUnmatched("guard: nothing should call RandomAccessFile.getChannel() on this target")
    @DetourMethod()
    public static FileChannel getChannel(RandomAccessFile instance) {
        log.error("RandomAccessFile.getChannel() called but FileChannel is not supported in this environment.");
        throw new UnsupportedOperationException("RandomAccessFile.getChannel() is not supported in this environment.");
    }
}
