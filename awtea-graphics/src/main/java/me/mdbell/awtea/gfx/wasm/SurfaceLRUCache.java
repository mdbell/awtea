package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

@Monitored.AllMethods
class SurfaceLRUCache {

    private static final Logger log = LoggerFactory.getLogger(SurfaceLRUCache.class);

    private final int maxSize;
    private final Map<Surface, Node> map;
    private Node head; // most recently used
    private Node tail; // least recently used

    private final WasmAwtRasterizerExports exports;

    public SurfaceLRUCache(WasmSurfaceBackend backend, int maxSize) {
        this.exports = backend.exports;
        this.maxSize = maxSize;
        this.map = new HashMap<>();
    }

    public SurfaceCacheEntry create(Surface surface) {
        return new SurfaceCacheEntry(surface);
    }

    public SurfaceCacheEntry get(Surface surface) {
        removeDeadEntries(); // clean up dead entries first

        Node node = map.get(surface);
        if (node != null) {
            moveToHead(node);
            return node.value;
        }

        // Try allocating a new entry
        SurfaceCacheEntry entry;
        try {
            entry = new SurfaceCacheEntry(surface);
        } catch (IllegalStateException e) {
            // Allocation failed, free the oldest entry and retry once
            if (tail != null) {
                removeTail();
                try {
                    entry = new SurfaceCacheEntry(surface);
                } catch (IllegalStateException e2) {
                    // Still failed; give up
                    log.error("Failed to allocate SurfaceCacheEntry even after freeing oldest entry");
                    return null;
                }
            } else {
                // No entries to free; give up
                log.error("Failed to allocate SurfaceCacheEntry and cache is empty");
                return null;
            }
        }

        put(surface, entry);
        return entry;
    }


    public void put(Surface surface, SurfaceCacheEntry value) {
        removeDeadEntries();
        Node node = map.get(surface);
        if (node != null) {
            node.value.release(); // free old WASM memory
            node.value = value;
            moveToHead(node);
        } else {
            node = new Node(value);
            node.key = surface;
            map.put(surface, node);
            addToHead(node);
            if (map.size() > maxSize) {
                removeTail();
            }
        }
    }

    private void moveToHead(Node node) {
        if (node == head) return;
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node node) {
        node.prev = null;
        node.next = head;
        if (head != null) head.prev = node;
        head = node;
        if (tail == null) tail = node;
    }

    private void removeNode(Node node) {
        if (node.prev != null) node.prev.next = node.next;
        else head = node.next;
        if (node.next != null) node.next.prev = node.prev;
        else tail = node.prev;
    }

    private void removeTail() {
        if (tail == null) return;
        map.remove(tail.key);
        tail.value.release();
        removeNode(tail);
    }

    private static class Node {
        Surface key;
        SurfaceCacheEntry value;
        Node prev, next;

        Node(SurfaceCacheEntry value) {
            this.value = value;
        }
    }

    public void clear() {
        Node current = head;
        while (current != null) {
            current.value.release();
            current = current.next;
        }
        map.clear();
        head = tail = null;
    }

    private void removeDeadEntries() {
        Node current = head;
        while (current != null) {
            if (current.value.getSurface() == null) {
                Node toRemove = current;
                current = current.next;
                map.remove(toRemove.key);
                toRemove.value.release();
                removeNode(toRemove);
            } else {
                current = current.next;
            }
        }
    }

    @Monitored.AllMethods
    class SurfaceCacheEntry {
        final WeakReference<Surface> surfaceRef;
        int tempSurfaceId;  // Temporary surface ID for caching external surfaces
        int ptr;
        Uint8ClampedArray pixelsView;

        public SurfaceCacheEntry(Surface surface) {
            this.surfaceRef = new WeakReference<>(surface);
            int stride = surface.getWidth() * 4;
            
            // Find a free surface slot and allocate it
            this.tempSurfaceId = exports.findFreeSurfaceId();
            if (this.tempSurfaceId == -1) {
                throw new IllegalStateException("SurfaceCacheEntry: no free surface slots available");
            }
            
            // Reset the surface with the appropriate dimensions
            int rc = exports.resetSurface(this.tempSurfaceId, 0, surface.getWidth(),
                    surface.getHeight(), surface.getFormat());
            if (rc != 0) {
                throw new IllegalStateException("SurfaceCacheEntry: failed to reset surface: " + rc);
            }
            
            this.ptr = exports.getSurfacePixelsPtr(this.tempSurfaceId);
        }

        public void sync() {
            Surface surface = surfaceRef.get();
            if (surface == null) {
                release();
                return; // surface was GC'd
            }

            if (!surface.isDirty() && pixelsView != null) {
                return; // already synced
            }

            Uint8ClampedArray srcPixels = surface.getPixelData();
            if (pixelsView == null) {
                this.pixelsView = new Uint8ClampedArray(
                        exports.getMemory().getBuffer(),
                        ptr,
                        srcPixels.getByteLength()
                );
            }
            if (srcPixels.getBuffer() == pixelsView.getBuffer()) {
                log.error("SurfaceCacheEntry.sync: Surface is already using WASM memory buffer! We are copying unnecessarily.");
            }
            this.pixelsView.set(srcPixels);
        }

        public void release() {
            if (tempSurfaceId != -1) {
                exports.freeSurface(tempSurfaceId);
                ptr = 0;
                tempSurfaceId = -1;
            }
        }

        public Surface getSurface() {
            return surfaceRef.get();
        }
    }
}
