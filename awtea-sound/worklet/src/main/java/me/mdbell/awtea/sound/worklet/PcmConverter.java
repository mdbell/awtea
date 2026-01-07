package me.mdbell.awtea.sound.worklet;

/**
 * Utility class for converting raw PCM byte data to float samples.
 * Designed to be extensible for additional bit depths (24-bit, 32-bit).
 */
public class PcmConverter {

    /**
     * Convert PCM byte data to interleaved float samples in the range [-1.0, 1.0].
     *
     * @param pcmBytes       Raw PCM byte data (input)
     * @param output         Float array to store converted samples (output, interleaved)
     * @param frames         Number of audio frames to convert
     * @param channels       Number of audio channels
     * @param sampleSizeBits Sample size in bits (8, 16, 24, 32)
     * @param bigEndian      Whether the data is big-endian
     */
    public static void convertPcmToFloat(byte[] pcmBytes, float[] output,
                                         int frames, int channels,
                                         int sampleSizeBits, boolean bigEndian) {
        int sampleSizeBytes = sampleSizeBits / 8;
        int frameSizeBytes = sampleSizeBytes * channels;
        float scale = (float) (1.0 / Math.pow(2, sampleSizeBits - 1));

        int outputIndex = 0;
        for (int frame = 0; frame < frames; frame++) {
            int frameOffset = frame * frameSizeBytes;
            for (int ch = 0; ch < channels; ch++) {
                int sampleOffset = frameOffset + ch * sampleSizeBytes;
                int sample = getSample(pcmBytes, sampleOffset, sampleSizeBits, bigEndian);
                output[outputIndex++] = sample * scale;
            }
        }
    }

    /**
     * Reads a PCM sample from a byte array at a given offset.
     * Supports 8, 16, and 24-bit samples. Can be extended for 32-bit.
     *
     * @param buffer           The buffer to read from
     * @param offset           The offset to read from
     * @param sampleSizeInBits The size of the sample in bits
     * @param bigEndian        Whether the sample is big endian
     * @return The sample as a signed integer
     */
    private static int getSample(byte[] buffer, int offset, int sampleSizeInBits, boolean bigEndian) {
        int res = 0;
        
        // Switch on sample size to support different bit depths
        // Add cases for 32-bit or other formats here as needed
        switch (sampleSizeInBits) {
            case 8:
                // 8-bit PCM is typically unsigned, centered at 128
                res = (buffer[offset] & 0xFF) - 128;
                break;
                
            case 16:
                if (bigEndian) {
                    res = (buffer[offset] << 8) | (buffer[offset + 1] & 0xFF);
                } else {
                    res = (buffer[offset + 1] << 8) | (buffer[offset] & 0xFF);
                }
                break;
                
            case 24:
                if (bigEndian) {
                    res = (buffer[offset] << 16) | ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset + 2] & 0xFF);
                } else {
                    res = (buffer[offset + 2] << 16) | ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset] & 0xFF);
                }
                break;
                
            // To add 32-bit PCM support, add a case here:
            // case 32:
            //     if (bigEndian) {
            //         res = (buffer[offset] << 24) | ((buffer[offset + 1] & 0xFF) << 16) |
            //               ((buffer[offset + 2] & 0xFF) << 8) | (buffer[offset + 3] & 0xFF);
            //     } else {
            //         res = (buffer[offset + 3] << 24) | ((buffer[offset + 2] & 0xFF) << 16) |
            //               ((buffer[offset + 1] & 0xFF) << 8) | (buffer[offset] & 0xFF);
            //     }
            //     break;
                
            default:
                throw new IllegalArgumentException("Unsupported sample size: " + sampleSizeInBits + " bits");
        }
        
        return res;
    }
}
