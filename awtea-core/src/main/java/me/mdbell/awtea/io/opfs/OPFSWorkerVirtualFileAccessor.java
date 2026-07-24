package me.mdbell.awtea.io.opfs;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.jso.FileSystemFileHandle;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.runtime.fs.VirtualFileAccessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import me.mdbell.awtea.util.TypedArrays;

@ExtensionMethod({JSObjectsExtensions.class})
class OPFSWorkerVirtualFileAccessor implements VirtualFileAccessor {

    private static final String WORKER_SYNC_PROPERTY = "me.mdbell.awtea.io.opfs.worker_sync";

    private static String workerScriptUrl;
    // Singleton shared worker — created once, persistent onmessage dispatcher keyed
    // by request ID.
    private static JSObject sharedWorker;
    private static int nextRequestId = 1;
    // Java-side pending request registry — keyed by request ID, value is the
    // resolve callback.
    private static final Map<Integer, Consumer<WorkerMessage>> pendingRequests = new HashMap<>();

    private int position = 0;
    private final int handleId;

    private static final String WORKER_SCRIPT = "const handles = new Map();\n"
            + "let nextHandleId = 1;\n"
            + "self.onmessage = async (event) => {\n"
            + "  const msg = event.data || {};\n"
            + "  const id = msg.id;\n"
            + "  try {\n"
            + "    switch (msg.type) {\n"
            + "      case 'supportsSyncAccessHandle': {\n"
            + "        const supported = !!msg.fileHandle && typeof msg.fileHandle.createSyncAccessHandle === 'function';\n"
            + "        self.postMessage({ id, type: 'supportsSyncAccessHandleResult', supported });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'open': {\n"
            + "        const accessHandle = await msg.fileHandle.createSyncAccessHandle();\n"
            + "        const newId = nextHandleId++;\n"
            + "        handles.set(newId, accessHandle);\n"
            + "        self.postMessage({ id, type: 'openResult', handleId: newId });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'read': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (!accessHandle) throw new Error('Unknown handle: ' + msg.handleId);\n"
            + "        const length = msg.length | 0;\n"
            + "        const out = new Uint8Array(length);\n"
            + "        const bytes = accessHandle.read(out, { at: msg.position | 0 });\n"
            + "        const safeBytes = bytes > 0 ? bytes : 0;\n"
            + "        const data = out.buffer.slice(0, safeBytes);\n"
            + "        self.postMessage({ id, type: 'readResult', bytes, data }, [data]);\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'write': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (!accessHandle) throw new Error('Unknown handle: ' + msg.handleId);\n"
            + "        const bytes = accessHandle.write(new Uint8Array(msg.data), { at: msg.position | 0 });\n"
            + "        self.postMessage({ id, type: 'writeResult', bytes });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'size': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (!accessHandle) throw new Error('Unknown handle: ' + msg.handleId);\n"
            + "        self.postMessage({ id, type: 'sizeResult', size: accessHandle.getSize() | 0 });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'resize': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (!accessHandle) throw new Error('Unknown handle: ' + msg.handleId);\n"
            + "        accessHandle.truncate(msg.size | 0);\n"
            + "        self.postMessage({ id, type: 'ok' });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'flush': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (!accessHandle) throw new Error('Unknown handle: ' + msg.handleId);\n"
            + "        accessHandle.flush();\n"
            + "        self.postMessage({ id, type: 'ok' });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'close': {\n"
            + "        const accessHandle = handles.get(msg.handleId);\n"
            + "        if (accessHandle) {\n"
            + "          accessHandle.close();\n"
            + "          handles.delete(msg.handleId);\n"
            + "        }\n"
            + "        self.postMessage({ id, type: 'ok' });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'flushAll': {\n"
            + "        for (const accessHandle of handles.values()) {\n"
            + "          accessHandle.flush();\n"
            + "        }\n"
            + "        self.postMessage({ id, type: 'ok' });\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'shutdown': {\n"
            + "        for (const accessHandle of handles.values()) {\n"
            + "          try { accessHandle.flush(); } catch (e) {}\n"
            + "          try { accessHandle.close(); } catch (e) {}\n"
            + "        }\n"
            + "        handles.clear();\n"
            + "        self.postMessage({ id, type: 'ok' });\n"
            + "        close();\n"
            + "        break;\n"
            + "      }\n"
            + "      case 'keepalive': {\n"
            + "        self.postMessage({ id, type: 'keepaliveAck' });\n"
            + "        break;\n"
            + "      }\n"
            + "      default:\n"
            + "        throw new Error('Unknown request type: ' + msg.type);\n"
            + "    }\n"
            + "  } catch (err) {\n"
            + "    const message = (err && err.message) ? err.message : String(err);\n"
            + "    self.postMessage({ id, type: 'error', error: message });\n"
            + "  }\n"
            + "};\n";

    OPFSWorkerVirtualFileAccessor(FileSystemFileHandle fileHandle) throws IOException {
        ensureSharedWorker();
        WorkerMessage response = request("open", message -> message.setFileHandle(fileHandle));
        this.handleId = ((OpenResult) response).getHandleId();
    }

    /**
     * Creates the shared worker once and wires a single permanent onmessage
     * handler that dispatches responses to Java-side pending requests by ID.
     * The handler is set once and never replaced, so it cannot conflict with
     * any other worker or port (e.g. the audio worklet).
     */
    private static synchronized void ensureSharedWorker() {
        if (sharedWorker != null) {
            return;
        }
        sharedWorker = createWorker(getWorkerScriptUrl());
        setOnMessage(sharedWorker, evt -> {
            WorkerMessage msg = (WorkerMessage) evt.getData();
            if (msg.nullish()) {
                return;
            }
            Consumer<WorkerMessage> resolve = pendingRequests.remove(msg.getId());
            if (resolve != null) {
                resolve.accept(msg);
            }
        });
    }

    static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(WORKER_SYNC_PROPERTY, "true"));
    }

    static boolean isSupported(FileSystemFileHandle fileHandle) {
        if (!supportsWorkers() || fileHandle == null) {
            return false;
        }
        try {
            ensureSharedWorker();
            SupportsSyncAccessHandleResult response = (SupportsSyncAccessHandleResult) requestStatic(
                    "supportsSyncAccessHandle",
                    message -> message.setFileHandle(fileHandle));
            return response.isSupported();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int limit) throws IOException {
        if (limit <= 0) {
            return 0;
        }

        ReadResult response = (ReadResult) request("read", message -> {
            message.setHandleId(handleId);
            message.setPosition(position);
            message.setLength(limit);
        });

        int bytesRead = response.getBytes();
        if (bytesRead <= 0) {
            return -1;
        }

        byte[] data = TypedArrays.toJavaArray(new Int8Array(response.getData()));
        System.arraycopy(data, 0, buffer, offset, bytesRead);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public void write(byte[] buffer, int offset, int limit) throws IOException {
        if (limit <= 0) {
            return;
        }

        Int8Array source = TypedArrays.from(buffer);
        ArrayBuffer data = source.getBuffer().slice(source.getByteOffset() + offset,
                source.getByteOffset() + offset + limit);

        WriteResult response = (WriteResult) request("write", message -> {
            message.setHandleId(handleId);
            message.setPosition(position);
            message.setData(data);
        }, data);

        int bytesWritten = response.getBytes();
        if (bytesWritten > 0) {
            position += bytesWritten;
        }
    }

    @Override
    public int tell() {
        return position;
    }

    @Override
    public void seek(int target) {
        this.position = target;
    }

    @Override
    public void skip(int amount) {
        this.position += amount;
    }

    @Override
    public int size() throws IOException {
        SizeResult response = (SizeResult) request("size",
                message -> message.setHandleId(handleId));
        return response.getSize();
    }

    @Override
    public void resize(int size) throws IOException {
        request("resize", message -> {
            message.setHandleId(handleId);
            message.setSize(size);
        });
    }

    @Override
    public void close() throws IOException {
        request("close", message -> {
            message.setHandleId(handleId);
        });
        // Do NOT shutdown or terminate — the worker is shared and may have other open
        // handles.
    }

    @Override
    public void flush() throws IOException {
        request("flush", message -> message.setHandleId(handleId));
    }

    private WorkerMessage request(String type, MessageWriter messageWriter, JSObject... transfer) throws IOException {
        return requestStatic(type, messageWriter, transfer);
    }

    private static WorkerMessage requestStatic(String type, MessageWriter messageWriter, JSObject... transfer)
            throws IOException {
        WorkerMessage req = JSObjects.create();
        int requestId = nextRequestId++;
        req.setId(requestId);
        req.setType(type);

        if (messageWriter != null) {
            messageWriter.write(req);
        }

        WorkerMessage response;
        try {
            response = post(requestId, req, transfer).await();
        } catch (Throwable e) {
            throw new IOException("OPFS worker request failed: " + type, e);
        }

        if (response.nullish()) {
            throw new IOException("No response from OPFS worker for request: " + type);
        }

        if ("error".equals(response.getType())) {
            String error = ((ErrorResult) response).getError();
            throw new IOException("OPFS worker error for request '" + type + "': " + error);
        }

        return response;
    }

    /**
     * Registers a pending resolve callback in the Java-side map, then posts the
     * message to the shared worker. The persistent onmessage handler resolves the
     * promise by ID when the worker replies.
     */
    private static JSPromise<WorkerMessage> post(int requestId, WorkerMessage req, JSObject... transfer) {
        return new JSPromise<>((resolve, reject) -> {
            pendingRequests.put(requestId, resolve::accept);
            postToWorker(sharedWorker, req, transfer);
        });
    }

    private static synchronized String getWorkerScriptUrl() {
        if (workerScriptUrl == null) {
            workerScriptUrl = createWorkerUrl(WORKER_SCRIPT);
        }
        return workerScriptUrl;
    }

    @JSBody(params = "script", script = "const blob = new Blob([script], { type: 'text/javascript' }); return (URL || webkitURL).createObjectURL(blob);")
    private static native String createWorkerUrl(String script);

    @JSBody(params = "url", script = "return new Worker(url);")
    private static native JSObject createWorker(String url);

    @JSBody(params = {"worker", "handler"}, script = "worker.onmessage = handler;")
    private static native void setOnMessage(JSObject worker, EventListener<MessageEvent> handler);

    @JSBody(params = {"worker", "message",
            "transfer"}, script = "if (transfer && transfer.length) { worker.postMessage(message, transfer); }"
            + "else { worker.postMessage(message); }")
    private static native void postToWorker(JSObject worker, WorkerMessage message, JSObject... transfer);

    @JSBody(script = "return typeof Worker !== 'undefined' && typeof navigator !== 'undefined' && !!navigator.storage && !!navigator.storage.getDirectory;")
    private static native boolean supportsWorkers();

    private interface MessageWriter {
        void write(WorkerMessage message);
    }

    private interface WorkerMessage extends JSObject {
        @JSProperty("id")
        int getId();

        @JSProperty("id")
        void setId(int id);

        @JSProperty("type")
        String getType();

        @JSProperty("type")
        void setType(String type);

        @JSProperty("handleId")
        int getHandleId();

        @JSProperty("handleId")
        void setHandleId(int handleId);

        @JSProperty("size")
        int getSize();

        @JSProperty("size")
        void setSize(int size);

        @JSProperty("position")
        int getPosition();

        @JSProperty("position")
        void setPosition(int position);

        @JSProperty("length")
        int getLength();

        @JSProperty("length")
        void setLength(int length);

        @JSProperty("data")
        ArrayBuffer getData();

        @JSProperty("data")
        void setData(ArrayBuffer data);

        @JSProperty("fileHandle")
        FileSystemFileHandle getFileHandle();

        @JSProperty("fileHandle")
        void setFileHandle(FileSystemFileHandle fileHandle);
    }

    private interface OpenResult extends WorkerMessage {
        @JSProperty("handleId")
        int getHandleId();
    }

    private interface ReadResult extends WorkerMessage {
        @JSProperty("bytes")
        int getBytes();

        @JSProperty("data")
        ArrayBuffer getData();
    }

    private interface WriteResult extends WorkerMessage {
        @JSProperty("bytes")
        int getBytes();
    }

    private interface SizeResult extends WorkerMessage {
        @JSProperty("size")
        int getSize();
    }

    private interface ErrorResult extends WorkerMessage {
        @JSProperty("error")
        String getError();
    }

    private interface SupportsSyncAccessHandleResult extends WorkerMessage {
        @JSProperty("supported")
        boolean isSupported();
    }
}
