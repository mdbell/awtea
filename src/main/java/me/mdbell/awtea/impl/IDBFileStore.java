package me.mdbell.awtea.impl;

import jagex3.io.BufferedFile;
import jagex3.io.FileStream;
import me.mdbell.awtea.impl.idb.IDBStore;
import me.mdbell.awtea.impl.idb.IndexDBVirtualFSException;

public class IDBFileStore extends FileStream {

    private final int store;

    public IDBFileStore(int store, BufferedFile datFile, BufferedFile idx, int maxFileSize) {
        super(store, datFile, idx, maxFileSize);
        this.store = store; // technically we could get the value
        // from the super class, but then we'd have to make it protected
    }

    @Override
    public byte[] read(int file) {
        try {
            return IDBStore.get(this.store, file);
        } catch (IndexDBVirtualFSException e) {
            return null;
        }
    }

    @Override
	public boolean write(int file, byte[] data, int len, boolean useExisting) {
        try {
            return IDBStore.put(this.store, file, data);
        } catch (IndexDBVirtualFSException e) {
            return false;
        }
    }

}
