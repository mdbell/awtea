package me.mdbell.awtea.util.jso;


import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;

public interface StorageManager extends JSObject {

    JSPromise<StorageEstimateResult> estimate();

    JSPromise<FileSystemDirectoryHandle> getDirectory();


    interface StorageEstimateResult extends JSObject {
        @JSProperty("quota")
        long getQuota();

        @JSProperty("usage")
        long getUsage();

        // also usageDetails, but it's non-standard
    }
}
