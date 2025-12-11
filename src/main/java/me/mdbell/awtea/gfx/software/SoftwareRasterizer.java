package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.SurfaceCommand;

import java.util.List;

public class SoftwareRasterizer implements Rasterizer {

	private SoftwareSurface surface;

	SoftwareRasterizer(SoftwareSurface surface) {

	}

	private SoftwareRasterizer(SoftwareRasterizer other) {
		this.surface = other.surface;
	}

	@Override
	public Rasterizer create() {
		return new SoftwareRasterizer(this);
	}

	@Override
	public void reset() {

	}

	@Override
	public void onResize(int width, int height) {
		// unsupported, fixed-size surface
	}

	@Override
	public void rasterizeCommands(List<SurfaceCommand> cmds) {
		// currently a stub, in RS they blit directly to the underlying pixel buffer
	}
}
