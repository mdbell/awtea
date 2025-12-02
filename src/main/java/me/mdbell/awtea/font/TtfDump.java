package me.mdbell.awtea.font;

import me.mdbell.awtea.util.GlyphRasterizer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TtfDump {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Missing font");
			System.exit(1);
		}

		String fontFile = args[0];

		System.out.println("Reading: " + fontFile);

		byte[] bytes = Files.readAllBytes(Paths.get(fontFile));

		TrueTypeFont font = TrueTypeFont.read(bytes);

		System.out.println("head: " + font.getHeadTable());
		System.out.println("maxp: " + font.getMaxpTable());
		System.out.println("====================");
		System.out.println("Glyph A: " + font.glyphForCodePoint('a'));
		System.out.println("Glyph B: " + font.glyphForCodePoint('B'));
		System.out.println("Glyph €:" + font.glyphForCodePoint(0x20AC));

		// sanity checks for glyph parsing

		Glyph g = font.loadGlyphForCodePoint('A');
		if (g == null) {
			System.out.println("No glyph for 'A'");
			return;
		}

		System.out.println("Glyph 'A' contours=" + g.numberOfContours);
		System.out.println("Bounds: [" + g.xMin + "," + g.yMin + "] - [" + g.xMax + "," + g.yMax + "]");

		for (int c = 0; c < g.numberOfContours; c++) {
			int start = (c == 0) ? 0 : (g.endPtsOfContours[c - 1] + 1);
			int end = g.endPtsOfContours[c];

			System.out.println("Contour " + c + " points " + start + ".." + end);
			for (int i = start; i <= end; i++) {
				System.out.printf("  %3d: (%d,%d)%s%n",
					i, g.x[i], g.y[i], g.onCurve[i] ? " ON" : " OFF");
			}
		}

		// Glyph paths
		GlyphPath path = GlyphPathBuilder.buildPath(g);

		System.out.println("Path for 'A':");
		for (GlyphPath.Cmd cmd : path.getCommands()) {
			switch (cmd.type) {
				case MOVE_TO:
					System.out.printf("MOVE_TO (%.1f, %.1f)%n", cmd.x1, cmd.y1);
					break;
				case LINE_TO:
					System.out.printf("LINE_TO (%.1f, %.1f)%n", cmd.x1, cmd.y1);
					break;
				case QUAD_TO:
					System.out.printf("QUAD_TO ctrl(%.1f, %.1f) end(%.1f, %.1f)%n",
						cmd.x2, cmd.y2, cmd.x1, cmd.y1);
					break;
				case CLOSE:
					System.out.println("CLOSE");
					break;
			}
		}

		int gidA = font.glyphForCodePoint('A');
		int gidB = font.glyphForCodePoint('B');
		int gidEuro = font.glyphForCodePoint(0x20AC);


		// metrics

		System.out.println("unitsPerEm = " + font.getUnitsPerEm());
		System.out.println("ascent=" + font.getAscentUnits()
			+ " descent=" + font.getDescentUnits()
			+ " lineGap=" + font.getLineGapUnits());

		System.out.println("'A' gid=" + gidA + " adv=" + font.getAdvanceWidthUnits(gidA));
		System.out.println("'B' gid=" + gidB + " adv=" + font.getAdvanceWidthUnits(gidB));
		System.out.println("'€' gid=" + gidEuro + " adv=" + font.getAdvanceWidthUnits(gidEuro));

		float sizePx = 32f;
		float scale = sizePx / font.getUnitsPerEm();
		System.out.println("scale @ " + sizePx + "px = " + scale);

		System.out.println("'A' advance px ≈ " + font.getAdvanceWidthUnits(gidA) * scale);
		System.out.println("'B' advance px ≈ " + font.getAdvanceWidthUnits(gidB) * scale);
		System.out.println("'€' advance px ≈ " + font.getAdvanceWidthUnits(gidEuro) * scale);
		System.out.println("line height px ≈ " + (font.getAscentUnits() - font.getDescentUnits()
			+ font.getLineGapUnits()) * scale);

		// simple rendering test

		BufferedImage img = new BufferedImage(500, 300, BufferedImage.TYPE_INT_ARGB);

		Graphics gfx = img.getGraphics();

		gfx.setColor(Color.BLACK);
		gfx.fillRect(0, 0, 500, 300);

		GlyphRasterizer.RasterTarget target = new GlyphRasterizer.RasterTarget() {
			@Override
			public int getWidth() {
				return img.getWidth();
			}

			@Override
			public int getHeight() {
				return img.getHeight();
			}

			@Override
			public void setRGB(int x, int y, int argb) {
				img.setRGB(x, y, argb);
			}

			@Override
			public int getRGB(int x, int y) {
				return img.getRGB(x, y);
			}
		};

		int[] ssLevels = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

		for(int i = 0; i < ssLevels.length; i++) {
			GlyphRasterizer.drawString(font, "This is supersample level:" + ssLevels[i], target, 12,
				5, 15 + (i * 15), 0xFFFFFFFF, ssLevels[i]);
		}

		ImageIO.write(img, "png", new File("./A.png"));

		System.out.println(font.getFamily() + " - Full:" + font.getFullName());
	}
}
