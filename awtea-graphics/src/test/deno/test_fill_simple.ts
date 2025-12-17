import { assertEquals } from "@std/assert";
import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Simple fill polygon test", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const surfaceId = rasterizer.allocateSurface(100, 100);
  const contextId = rasterizer.createContext(surfaceId);

  const red = WasmRasterizer.makeARGB(255, 255, 0, 0);

  // Simple triangle
  const xPoints = [25, 75, 50];
  const yPoints = [25, 25, 75];

  rasterizer.renderVariableLengthCommands(contextId, [
    (w) => WasmRasterizer.writeSetColorCommand(w, red),
    (w) => WasmRasterizer.writeFillPolygonCommand(w, xPoints, yPoints),
  ]);

  const pixels = rasterizer.copySurfacePixels(surfaceId);

  // Print some pixel values for debugging
  console.log("Pixel at (50, 45):", pixels[45 * 100 + 50].toString(16));
  console.log("Pixel at (50, 40):", pixels[40 * 100 + 50].toString(16));
  console.log("Pixel at (50, 30):", pixels[30 * 100 + 50].toString(16));
  
  // Count non-zero pixels
  let count = 0;
  for (let i = 0; i < pixels.length; i++) {
    if (pixels[i] !== 0) count++;
  }
  console.log("Non-zero pixels:", count);

  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
});
