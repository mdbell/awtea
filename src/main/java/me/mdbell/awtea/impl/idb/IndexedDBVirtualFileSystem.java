package me.mdbell.awtea.impl.idb;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;


public class IndexedDBVirtualFileSystem implements VirtualFileSystem {
	public static final String ROOT_DIR = "/";
	public static final String HOME_DIR = "/home";

	public IndexedDBVirtualFileSystem() {
		if (!IndexedDBHelper.exists(HOME_DIR)) {
			IndexedDBHelper.createDirectory(HOME_DIR);
		}
	}

	@Override
	public String getUserDir() {
		return HOME_DIR;
	}

	@Override
	public VirtualFile getFile(String path) {
		return new IndexedDBVirtualFile(path);
	}

	@Override
	public boolean isWindows() {
		return false; // Assuming Unix-style paths
	}

	@Override
	public String canonicalize(String path) {
		return path.replaceAll("/+", "/"); // Normalize path
	}

	@Override
	public String[] getRoots() {
		return new String[]{ROOT_DIR};
	}
}
