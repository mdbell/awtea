package me.mdbell.awtea.font;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class GlyphPath {

	public enum CmdType {
		MOVE_TO,
		LINE_TO,
		QUAD_TO,
		CLOSE
	}

	public static final class Cmd {
		public final CmdType type;
		public final float x1, y1;
		public final float x2, y2; // only used for QUAD_TO (control point)

		private Cmd(CmdType type, float x1, float y1, float x2, float y2) {
			this.type = type;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		public static Cmd moveTo(float x, float y) {
			return new Cmd(CmdType.MOVE_TO, x, y, 0, 0);
		}

		public static Cmd lineTo(float x, float y) {
			return new Cmd(CmdType.LINE_TO, x, y, 0, 0);
		}

		public static Cmd quadTo(float cx, float cy, float x, float y) {
			return new Cmd(CmdType.QUAD_TO, x, y, cx, cy);
		}

		public static Cmd close() {
			return new Cmd(CmdType.CLOSE, 0, 0, 0, 0);
		}
	}

	private final List<Cmd> commands = new ArrayList<>();

	public void moveTo(float x, float y)  { commands.add(Cmd.moveTo(x, y)); }
	public void lineTo(float x, float y)  { commands.add(Cmd.lineTo(x, y)); }
	public void quadTo(float cx, float cy, float x, float y) {
		commands.add(Cmd.quadTo(cx, cy, x, y));
	}
	public void closePath()              { commands.add(Cmd.close()); }

}
