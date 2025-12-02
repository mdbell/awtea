class PCMProcessor extends AudioWorkletProcessor {
    constructor() {
        super();
        this.queue = []; // { data: Float32Array, frames, channels, offsetFrames }
        this.channels = 2;
        this.queuedFrames = 0;

        this.port.onmessage = (event) => {
            const msg = event.data;
            if (!msg || !msg.type) return;

            if (msg.type === "init") {
                this.channels = msg.channels || 2;
            } else if (msg.type === "pcm") {
                // msg.data is an ArrayBuffer (or transferable) of interleaved float32
                const data = new Float32Array(msg.data);
                const frames = msg.frames;
                const channels = msg.channels || this.channels;
                this.queue.push({
                    data,
                    frames,
                    channels,
                    offsetFrames: 0
                });
                this.queuedFrames += frames;
            }
        };
    }

    process(inputs, outputs, parameters) {
        const output = outputs[0]; // [channel][frame]
        const channels = output.length;
        const framesRequested = output[0].length;

        let framesFilled = 0;
        let framesConsumedTotal = 0;

        // Clear output first (in case of underrun)
        for (let ch = 0; ch < channels; ch++) {
            output[ch].fill(0);
        }

        while (framesFilled < framesRequested && this.queue.length > 0) {
            let chunk = this.queue[0];

            if (chunk.channels !== channels) {
                // shouldn't happen, but if it does just skip this chunk
                this.queuedFrames -= (chunk.frames - chunk.offsetFrames);
                this.queue.shift();
                continue;
            }

            const framesAvailable = chunk.frames - chunk.offsetFrames;
            if (framesAvailable <= 0) {
                this.queue.shift();
                continue;
            }

            const framesToCopy = Math.min(framesRequested - framesFilled, framesAvailable);
            const src = chunk.data;
            const cc  = channels;
            const startFrame = chunk.offsetFrames;

            for (let f = 0; f < framesToCopy; f++) {
                const srcBase = (startFrame + f) * cc;
                const dstIndex = framesFilled + f;
                for (let ch = 0; ch < cc; ch++) {
                    output[ch][dstIndex] = src[srcBase + ch];
                }
            }

            chunk.offsetFrames += framesToCopy;
            framesFilled       += framesToCopy;
            framesConsumedTotal += framesToCopy;

            if (chunk.offsetFrames >= chunk.frames) {
                this.queue.shift();
            }
        }

        this.queuedFrames -= framesConsumedTotal;
        if (this.queuedFrames < 0) {
            this.queuedFrames = 0;
        }

        // Let main thread know how many frames we consumed
        // (lets us backpressure - like making sync write functions work)
        if (framesConsumedTotal > 0) {
            this.port.postMessage({
                type: "consumed",
                frames: framesConsumedTotal
            });
        }

        return true; // keep processor alive
    }
}

registerProcessor("pcm-processor", PCMProcessor)
