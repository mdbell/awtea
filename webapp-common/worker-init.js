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
                    const result = await loadImageData(msg.url);
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

async function loadImageData(url) {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = () => reject(new Error(`Failed to load image: ${url}`));
        img.src = url;
    });
    const cvs = new OffscreenCanvas(img.naturalWidth, img.naturalHeight);
    const ctx = cvs.getContext('2d');
    ctx.drawImage(img, 0, 0);
    const imageData = ctx.getImageData(0, 0, img.naturalWidth, img.naturalHeight);
    // slice() so the buffer is detached from the ImageData and safe to transfer
    return { width: img.naturalWidth, height: img.naturalHeight, buffer: imageData.data.buffer.slice(0) };
}
