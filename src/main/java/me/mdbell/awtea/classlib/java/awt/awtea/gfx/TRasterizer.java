package me.mdbell.awtea.classlib.java.awt.awtea.gfx;

import java.util.List;

public interface TRasterizer {

	TRasterizer create();

	void reset();

	void onResize(int width, int height);

	void rasterizeCommands(List<TSurfaceCommand> cmds);
}
