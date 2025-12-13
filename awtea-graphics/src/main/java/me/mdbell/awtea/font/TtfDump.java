package me.mdbell.awtea.font;

import me.mdbell.awtea.util.GlyphRasterizer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TtfDump {

	private static final Logger log = LoggerFactory.getLogger(TtfDump.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			log.error("Missing font");
			System.exit(1);
		}

		String fontFile = args[0];

		log.info("Reading: {}", fontFile);

		byte[] bytes = Files.readAllBytes(Paths.get(fontFile));

		TrueTypeFont font = TrueTypeFont.read(bytes);

		log.info("head: {}", font.getHeadTable());
		log.info("maxp: {}", font.getMaxpTable());
		log.info("====================");
		log.info("Glyph A: {}", font.glyphForCodePoint('a'));
		log.info("Glyph B: {}", font.glyphForCodePoint('B'));
		log.info("Glyph €: {}", font.glyphForCodePoint(0x20AC));

		// sanity checks for glyph parsing

		Glyph g = font.loadGlyphForCodePoint('A');
		if (g == null) {
			log.info("No glyph for 'A'");
			return;
		}

		log.info("Glyph 'A' contours={}", g.numberOfContours);
		log.info("Bounds: [" + g.xMin + "," + g.yMin + "] - [" + g.xMax + "," + g.yMax + "]");

		for (int c = 0; c < g.numberOfContours; c++) {
			int start = (c == 0) ? 0 : (g.endPtsOfContours[c - 1] + 1);
			int end = g.endPtsOfContours[c];

			log.info("Contour {} points {}..", c, start, end);
			for (int i = start; i <= end; i++) {
				log.info("  %3d: (%d,%d)%s",
					i, g.x[i], g.y[i], g.onCurve[i] ? " ON" : " OFF");
			}
		}

		// Glyph paths
		GlyphPath path = GlyphPathBuilder.buildPath(g);

		log.info("Path for 'A':");
		for (GlyphPath.Cmd cmd : path.getCommands()) {
			switch (cmd.type) {
				case MOVE_TO:
					log.info("MOVE_TO (%.1f, %.1f)", cmd.x1, cmd.y1);
					break;
				case LINE_TO:
					log.info("LINE_TO (%.1f, %.1f)", cmd.x1, cmd.y1);
					break;
				case QUAD_TO:
					log.info("QUAD_TO ctrl(%.1f, %.1f) end(%.1f, %.1f)",
						cmd.x2, cmd.y2, cmd.x1, cmd.y1);
					break;
				case CLOSE:
					log.info("CLOSE");
					break;
			}
		}

		int gidA = font.glyphForCodePoint('A');
		int gidB = font.glyphForCodePoint('B');
		int gidEuro = font.glyphForCodePoint(0x20AC);


		// metrics

		log.info("unitsPerEm = {}", font.getUnitsPerEm());
		log.info("ascent={} descent={} lineGap={}", font.getAscentUnits(),
			font.getDescentUnits(), font.getLineGapUnits());

		log.info("'A' gid={} adv={}", gidA, font.getAdvanceWidthUnits(gidA));
		log.info("'B' gid={} adv={}", gidB, font.getAdvanceWidthUnits(gidB));
		log.info("'€' gid={} adv={}", gidEuro, font.getAdvanceWidthUnits(gidEuro));

		float sizePx = 32f;
		float scale = sizePx / font.getUnitsPerEm();
		log.info("scale @ {}px = {}", sizePx, scale);

		log.info("'A' advance px ≈ {}", font.getAdvanceWidthUnits(gidA) * scale);
		log.info("'€' advance px ≈ {}", font.getAdvanceWidthUnits(gidEuro) * scale);
		log.info("line height px ≈ {}", (font.getAscentUnits() - font.getDescentUnits()
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

		log.info("{} - Full: {}", font.getFamily(), font.getFullName());
	}
}
