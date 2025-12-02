package me.mdbell.awtea;

import org.teavm.interop.PlatformMarker;

public class Helper {

    @PlatformMarker
    public static boolean isTeaVM() {
        return false;
    }

    public static byte[][][] create3DByteArrayNative(int len1, int len2, int len3) {
        return new byte[len1][len2][len3];
    }

    public static byte[][][] create3DByteArray(int len1, int len2, int len3) {
        byte[][][] arr = new byte[len1][][];
        for (int i = 0; i < len1; i++) {
            arr[i] = new byte[len2][];
            for (int j = 0; j < len2; j++) {
                arr[i][j] = new byte[len3];
                for (int k = 0; k < len3; k++) {
                    arr[i][j][k] = (byte) 0;
                }
            }
        }
        return arr;
    }

    public static int[][][] create3DIntArray(int len1, int len2, int len3) {
        int[][][] arr = new int[len1][][];
        for (int i = 0; i < len1; i++) {
            arr[i] = new int[len2][];
            for (int j = 0; j < len2; j++) {
                arr[i][j] = new int[len3];
                for (int k = 0; k < len3; k++) {
                    arr[i][j][k] = 0;
                }
            }
        }
        return arr;
    }
}
