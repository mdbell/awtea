package me.mdbell.awtea.io.mount;

import org.teavm.runtime.fs.VirtualFile;
import org.teavm.runtime.fs.VirtualFileSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MountVirtualFileSystem implements VirtualFileSystem {

    static final String ROOT = "/";

    private final Map<String, VirtualFileSystem> mounts = new LinkedHashMap<>();
    VirtualFileSystem baseFileSystem;

    public void mount(String mountPath, VirtualFileSystem fileSystem) {
        if (fileSystem == null) {
            throw new IllegalArgumentException("Mounted filesystem cannot be null");
        }

        String normalizedPath = canonicalize(mountPath);
        if (ROOT.equals(normalizedPath)) {
            baseFileSystem = fileSystem;
        } else {
            mounts.put(normalizedPath, fileSystem);
        }
    }

    @Override
    public String getUserDir() {
        return ROOT;
    }

    @Override
    public VirtualFile getFile(String path) {
        String normalizedPath = canonicalize(path);
        if (ROOT.equals(normalizedPath)) {
            return new MountRootVirtualFile(this);
        }

        String directMount = findDirectMount(normalizedPath);
        if (directMount != null) {
            return new MountPointVirtualFile(directMount, mounts.get(directMount));
        }

        MountResolution resolution = resolve(normalizedPath);
        if (resolution != null) {
            return resolution.fileSystem.getFile(resolution.relativePath);
        }

        // Intermediate path: ancestor of a deeper mount point.
        if (isIntermediatePath(normalizedPath)) {
            return new SyntheticIntermediateDirectory(this, normalizedPath);
        }

        // Fall through to base filesystem if no named mount matches.
        if (baseFileSystem != null) {
            return baseFileSystem.getFile(normalizedPath);
        }

        return null;
    }

    @Override
    public boolean isWindows() {
        return false;
    }

    @Override
    public String canonicalize(String path) {
        if (path == null || path.isEmpty()) {
            return ROOT;
        }

        String normalized = path.replace('\\', '/').replaceAll("/+", "/");
        if (!normalized.startsWith(ROOT)) {
            normalized = ROOT + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith(ROOT)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Override
    public String[] getRoots() {
        return new String[]{ROOT};
    }

    private boolean isIntermediatePath(String normalizedPath) {
        String prefix = normalizedPath + ROOT;
        for (String mountPath : mounts.keySet()) {
            if (mountPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the next path segment after parentPath for all mounts that are
     * children of it.
     */
    String[] listChildMountSegments(String parentPath) {
        String prefix = parentPath.equals(ROOT) ? ROOT : parentPath + ROOT;
        List<String> segments = new ArrayList<>();
        for (String mountPath : mounts.keySet()) {
            if (!mountPath.startsWith(prefix) || mountPath.length() <= prefix.length()) {
                continue;
            }
            String remaining = mountPath.substring(prefix.length());
            int slash = remaining.indexOf('/');
            String segment = slash < 0 ? remaining : remaining.substring(0, slash);
            if (!segments.contains(segment)) {
                segments.add(segment);
            }
        }
        return segments.toArray(new String[0]);
    }

    static String lastSegment(String normalizedPath) {
        int slash = normalizedPath.lastIndexOf('/');
        return slash < 0 ? normalizedPath : normalizedPath.substring(slash + 1);
    }

    private String findDirectMount(String normalizedPath) {
        return mounts.containsKey(normalizedPath) ? normalizedPath : null;
    }

    private MountResolution resolve(String normalizedPath) {
        MountResolution bestMatch = null;
        for (Map.Entry<String, VirtualFileSystem> entry : mounts.entrySet()) {
            String mountPath = entry.getKey();
            if (!normalizedPath.equals(mountPath) && !normalizedPath.startsWith(mountPath + ROOT)) {
                continue;
            }

            if (bestMatch != null && mountPath.length() <= bestMatch.mountPath.length()) {
                continue;
            }

            String relativePath = normalizedPath.equals(mountPath)
                    ? entry.getValue().getUserDir()
                    : normalizedPath.substring(mountPath.length());
            if (relativePath == null || relativePath.isEmpty()) {
                relativePath = entry.getValue().getUserDir();
            }

            bestMatch = new MountResolution(mountPath, entry.getValue(), relativePath);
        }
        return bestMatch;
    }

    String[] listRootFiles() {
        List<String> names = new ArrayList<>();
        // Include base filesystem root listing first.
        if (baseFileSystem != null) {
            VirtualFile baseRoot = baseFileSystem.getFile(baseFileSystem.getUserDir());
            if (baseRoot != null) {
                String[] baseFiles = baseRoot.listFiles();
                if (baseFiles != null) {
                    for (String f : baseFiles) {
                        names.add(f);
                    }
                }
            }
        }
        // Append only the first path segment of each named mount (deduplicated).
        for (String segment : listChildMountSegments(ROOT)) {
            if (!names.contains(segment)) {
                names.add(segment);
            }
        }
        return names.toArray(new String[0]);
    }

}