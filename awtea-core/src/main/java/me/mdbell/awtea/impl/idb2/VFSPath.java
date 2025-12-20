package me.mdbell.awtea.impl.idb2;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Path normalization and manipulation utility for VFS.
 * Caches normalized paths for performance.
 */
@UtilityClass
public class VFSPath {
    
    private static final Map<String, String> normalizedCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * Normalize a path by removing redundant slashes and handling . and ..
     * 
     * @param path the path to normalize
     * @param isDirectory whether this path represents a directory
     * @return normalized path
     */
    public static String normalize(String path, boolean isDirectory) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        // Check cache first
        String cacheKey = path + (isDirectory ? "/" : "");
        String cached = normalizedCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        String normalized = normalizeImpl(path, isDirectory);
        
        // Cache the result (with simple size limit)
        if (normalizedCache.size() < MAX_CACHE_SIZE) {
            normalizedCache.put(cacheKey, normalized);
        }
        
        return normalized;
    }
    
    /**
     * Normalize a path (auto-detect if directory based on trailing slash)
     */
    public static String normalize(String path) {
        boolean isDirectory = path != null && path.endsWith("/");
        return normalize(path, isDirectory);
    }
    
    private static String normalizeImpl(String path, boolean isDirectory) {
        // Validate for illegal characters
        if (path.contains("\0")) {
            throw new IllegalArgumentException("Path contains null character");
        }
        
        String[] parts = path.split("/");
        Stack<String> stack = new Stack<>();
        
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            } else {
                stack.push(part);
            }
        }
        
        StringBuilder normalizedPath = new StringBuilder("/");
        for (int i = 0; i < stack.size(); i++) {
            normalizedPath.append(stack.get(i));
            if (i < stack.size() - 1) {
                normalizedPath.append("/");
            }
        }
        
        if (isDirectory && !normalizedPath.toString().endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath.append("/");
        }
        
        return normalizedPath.toString();
    }
    
    /**
     * Get the parent directory path
     * 
     * @param path the path
     * @return parent directory path (always ends with /)
     */
    public static String getParent(String path) {
        if (path == null || path.equals("/")) {
            return "/";
        }
        
        String normalized = normalize(path, false);
        int lastSlash = normalized.lastIndexOf('/');
        
        if (lastSlash == 0) {
            return "/";
        }
        
        return normalized.substring(0, lastSlash + 1);
    }
    
    /**
     * Get the file/directory name (last component)
     * 
     * @param path the path
     * @return the name
     */
    public static String getName(String path) {
        if (path == null || path.equals("/")) {
            return "/";
        }
        
        String normalized = normalize(path, false);
        int lastSlash = normalized.lastIndexOf('/');
        
        if (lastSlash == -1) {
            return normalized;
        }
        
        return normalized.substring(lastSlash + 1);
    }
    
    /**
     * Resolve a child path relative to a parent
     * 
     * @param parent the parent directory
     * @param child the child name
     * @return resolved path
     */
    public static String resolve(String parent, String child) {
        if (child == null || child.isEmpty()) {
            return normalize(parent, true);
        }
        
        if (child.startsWith("/")) {
            return normalize(child);
        }
        
        String normalizedParent = normalize(parent, true);
        
        // Build the path
        StringBuilder sb = new StringBuilder(normalizedParent);
        if (!normalizedParent.endsWith("/")) {
            sb.append("/");
        }
        sb.append(child);
        
        return normalize(sb.toString());
    }
    
    /**
     * Check if a path is valid
     * 
     * @param path the path to check
     * @return true if valid
     */
    public static boolean isValid(String path) {
        return path != null && 
               !path.isEmpty() && 
               !path.contains("\0") &&
               !path.contains("//");
    }
    
    /**
     * Clear the normalization cache
     */
    public static void clearCache() {
        normalizedCache.clear();
    }
}
