package me.mdbell.awtea.impl.idb;

public class IndexDBVirtualFSException extends RuntimeException {

    public IndexDBVirtualFSException(String message) {
		super(message);
    }

    public IndexDBVirtualFSException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexDBVirtualFSException(Throwable cause) {
        super(cause);
    }

    public IndexDBVirtualFSException() {
        super();
    }
}
