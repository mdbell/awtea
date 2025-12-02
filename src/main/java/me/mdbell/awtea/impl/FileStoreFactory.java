package me.mdbell.awtea.impl;

import jagex3.io.BufferedFile;
import jagex3.io.FileStream;
import me.mdbell.awtea.Helper;

public final class FileStoreFactory {

    private FileStoreFactory() {

    }

    public static FileStream createFileStore(int store, BufferedFile datFile, BufferedFile idx, int maxFileSize) {
        if (!Helper.isTeaVM()) {
            return new FileStream(store, datFile, idx, maxFileSize);
        }
        return new IDBFileStore(store, datFile, idx, maxFileSize);
    }
}
