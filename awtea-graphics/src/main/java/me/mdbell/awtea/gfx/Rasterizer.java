package me.mdbell.awtea.gfx;

import java.util.List;

public interface Rasterizer {

	Rasterizer create();

	void reset();

	void rasterizeCommands(List<SurfaceCommand> cmds);
}
