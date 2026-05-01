// Worker bootstrap for gui-demo.
// Waits for the 'init' message from worker-init.js, stores the payload in
// self.pendingInit (read by TMainThreadBridge.init() via @JSBody), then starts
// the Java runtime.

import { main, setProperty } from './js/gui-demo.js';

self.onmessage = (evt) => {
    const msg = evt.data;
    if (!msg || msg.type !== 'init') return;

    self.pendingInit = msg;

    setProperty('me.mdbell.awtea.wasm.module_path', '/awtea-graphics/build/wasm/awt_raster.wasm');
    setProperty('me.mdbell.awtea.font.subpixel', 'true');
    setProperty('me.mdbell.awtea.font.supersample', '4');

    // 'worker-mode' is passed as the canvas ID; TApplet.createHeavyCanvas()
    // ignores it when running under TWorkerToolkit.
    main(['worker-mode', 'info']);
};
