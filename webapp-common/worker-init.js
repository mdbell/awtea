/**
 * Main-thread shim for awtea Web Worker mode.
 *
 * Transfers the canvas to an OffscreenCanvas, spawns the worker, forwards
 * browser events, and handles toolkit round-trip requests (getScreenSize,
 * setCursor, setFocus, loadImage, beep).
 *
 * Usage:
 *   import { initWorker } from './worker-init.js';
 *   initWorker(document.getElementById('my-canvas'), './worker-entry.js');
 */
// audioId → { context: AudioContext, node: AudioWorkletNode }
const audioContexts = new Map();
let nextAudioId = 1;

export function initWorker(canvas, workerUrl) {
    const dpr = window.devicePixelRatio || 1;
    const width = canvas.width;
    const height = canvas.height;

    canvas.setAttribute('tabindex', '0');
    canvas.style.outline = 'none';

    const offscreen = canvas.transferControlToOffscreen();
    const worker = new Worker(workerUrl, { type: 'module' });

    // ---- event forwarding ------------------------------------------------

    function forwardEvent(eventType, fields) {
        worker.postMessage({ type: 'event', eventType, ...fields });
    }

    function mouseFields(e) {
        const r = canvas.getBoundingClientRect();
        return {
            x: Math.round(e.clientX - r.left),
            y: Math.round(e.clientY - r.top),
            button: e.button,
            buttons: e.buttons,
            shiftKey: e.shiftKey,
            ctrlKey: e.ctrlKey,
            altKey: e.altKey,
            metaKey: e.metaKey,
        };
    }

    // Coalesce mousemove: one message per rAF tick
    let pendingMouseMove = null;
    function scheduleMouseFlush() {
        requestAnimationFrame(() => {
            if (pendingMouseMove) {
                worker.postMessage(pendingMouseMove);
                pendingMouseMove = null;
            }
        });
    }

    canvas.addEventListener('mousemove', e => {
        if (!pendingMouseMove) scheduleMouseFlush();
        pendingMouseMove = { type: 'event', eventType: 'mousemove', ...mouseFields(e) };
    });

    canvas.addEventListener('mousedown',  e => forwardEvent('mousedown',  mouseFields(e)));
    canvas.addEventListener('mouseup',    e => forwardEvent('mouseup',    mouseFields(e)));
    canvas.addEventListener('click',      e => forwardEvent('click',      mouseFields(e)));

    canvas.addEventListener('wheel', e => {
        e.preventDefault();
        const r = canvas.getBoundingClientRect();
        forwardEvent('wheel', {
            x: Math.round(e.clientX - r.left),
            y: Math.round(e.clientY - r.top),
            deltaY: e.deltaY,
            deltaMode: e.deltaMode,
        });
    }, { passive: false });

    // Suppress browser context menu so right-click reaches Java
    canvas.addEventListener('contextmenu', e => e.preventDefault());

    function keyFields(e) {
        return {
            code: e.code,
            key: e.key,
            shiftKey: e.shiftKey,
            ctrlKey: e.ctrlKey,
            altKey: e.altKey,
            metaKey: e.metaKey,
        };
    }

    canvas.addEventListener('keydown',  e => forwardEvent('keydown',  keyFields(e)));
    canvas.addEventListener('keyup',    e => forwardEvent('keyup',    keyFields(e)));
    canvas.addEventListener('keypress', e => forwardEvent('keypress', keyFields(e)));

    canvas.addEventListener('focus', () => forwardEvent('focus', {}));
    canvas.addEventListener('blur',  () => forwardEvent('blur',  {}));

    // ---- toolkit request handler -----------------------------------------

    worker.onmessage = async (evt) => {
        const msg = evt.data;
        if (!msg || !msg.type) return;

        switch (msg.type) {
            case 'getScreenSize':
                worker.postMessage({ id: msg.id, type: 'getScreenSize', width: screen.width, height: screen.height });
                break;

            case 'setCursor':
                canvas.style.cursor = msg.cursor || 'default';
                worker.postMessage({ id: msg.id, type: 'setCursor' });
                break;

            case 'setFocus':
                canvas.focus();
                // fire-and-forget (id === 0), no response needed
                break;

            case 'loadImage': {
                try {
                    const result = await loadImageData(msg.url, msg.bytes);
                    worker.postMessage(
                        { id: msg.id, type: 'loadImage', width: result.width, height: result.height, data: result.buffer },
                        [result.buffer]
                    );
                } catch (err) {
                    worker.postMessage({ id: msg.id, type: 'loadImage', error: err.message, width: 0, height: 0, data: new ArrayBuffer(0) });
                }
                break;
            }

            case 'beep':
                try {
                    const ctx = new AudioContext();
                    const osc = ctx.createOscillator();
                    osc.connect(ctx.destination);
                    osc.frequency.value = 440;
                    osc.start();
                    osc.stop(ctx.currentTime + 0.1);
                } catch (_) {}
                break;

            case 'audio.init': {
                const audioId = nextAudioId++;
                try {
                    const { sampleRate, channels, sampleSizeBits, bigEndian, maxFrames, script } = msg;
                    const ctx = new AudioContext({ sampleRate });
                    const scriptUrl = URL.createObjectURL(new Blob([script], { type: 'text/javascript' }));
                    try {
                        await ctx.audioWorklet.addModule(scriptUrl);
                    } finally {
                        URL.revokeObjectURL(scriptUrl);
                    }
                    const node = new AudioWorkletNode(ctx, 'pcm-processor', {
                        numberOfInputs: 0,
                        numberOfOutputs: 1,
                        outputChannelCount: [channels],
                    });
                    node.connect(ctx.destination);
                    node.port.postMessage({ type: 'init', channels, sampleRate, sampleSizeBits, bigEndian });
                    node.port.onmessage = (e) => {
                        const m = e.data;
                        if (m && m.type === 'consumed') {
                            worker.postMessage({ id: 0, type: 'audio.consumed', audioId, consumed: m.bytes });
                        }
                    };
                    audioContexts.set(audioId, { context: ctx, node });
                    worker.postMessage({ id: msg.id, type: 'audio.init', audioId });
                } catch (err) {
                    worker.postMessage({ id: msg.id, type: 'audio.init', audioId: -1 });
                }
                break;
            }

            case 'audio.pcm': {
                const audio = audioContexts.get(msg.audioId);
                if (audio) {
                    audio.node.port.postMessage(
                        { type: 'pcm', data: msg.bytes, frames: msg.frames },
                        [msg.bytes]
                    );
                }
                break;
            }

            case 'audio.keepalive': {
                const audio = audioContexts.get(msg.audioId);
                if (audio) audio.node.port.postMessage({ type: 'keepalive' });
                break;
            }

            case 'audio.close': {
                const audio = audioContexts.get(msg.audioId);
                if (audio) {
                    audio.node.port.postMessage({ type: 'shutdown' });
                    audio.node.disconnect();
                    audio.context.close();
                    audioContexts.delete(msg.audioId);
                }
                break;
            }
        }
    };

    // ---- resize observer -------------------------------------------------

    new ResizeObserver(entries => {
        for (const entry of entries) {
            const { width, height } = entry.contentRect;
            worker.postMessage({
                type: 'resize',
                width: Math.round(width),
                height: Math.round(height),
                dpr: window.devicePixelRatio || 1,
            });
        }
    }).observe(canvas);

    // ---- init ------------------------------------------------------------

    worker.postMessage({ type: 'init', offscreenCanvas: offscreen, width, height, dpr }, [offscreen]);

    return worker;
}

async function loadImageData(url, bytes) {
    let src = url;
    let createdUrl = false;
    if (bytes) {
        src = URL.createObjectURL(new Blob([bytes]));
        createdUrl = true;
    }
    try {
        const img = new Image();
        if (!createdUrl) img.crossOrigin = 'anonymous';
        await new Promise((resolve, reject) => {
            img.onload = resolve;
            img.onerror = () => reject(new Error(`Failed to load image: ${src}`));
            img.src = src;
        });
        const cvs = new OffscreenCanvas(img.naturalWidth, img.naturalHeight);
        const ctx = cvs.getContext('2d');
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, img.naturalWidth, img.naturalHeight);
        // slice() so the buffer is detached from the ImageData and safe to transfer
        return { width: img.naturalWidth, height: img.naturalHeight, buffer: imageData.data.buffer.slice(0) };
    } finally {
        if (createdUrl) URL.revokeObjectURL(src);
    }
}
