package me.mdbell.awtea.util;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.jso.Glyph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;

@ExtensionMethod(value = {JsonExtensions.class}, suppressBaseMethods = false)
public class FontAtlasGenerator {

    public static final String CHARSET = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
    public static final boolean DEBUG = !Glyph.MINIFY;

    public static void generateFontAtlas(String fontName, int fontSize, int fontStyle, String outputDir) {
        try {
            Font font = new Font(fontName, fontStyle, fontSize);

            int charHeight = fontSize * 2; // Allow for descenders
            int cols = 10;
            int rows = (int) Math.ceil((double) CHARSET.length() / cols);

            BufferedImage atlas = new BufferedImage(cols * fontSize, rows * charHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = atlas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(font);
            g2d.setColor(Color.WHITE);

            FontMetrics fm = g2d.getFontMetrics();
            StringBuilder json = new StringBuilder();
            json.append(DEBUG ? "{\n" : "{");
            int x = 0, y = 0;

            for (int i = 0; i < CHARSET.length(); i++) {
                char ch = CHARSET.charAt(i);

                int width = fm.charWidth(ch);
                int ascent = fm.getAscent();
                int descent = fm.getDescent();
                int advance = fm.charWidth(ch);

                int charX = x * fontSize + 5; // some padding to prevent letters from touching
                int charY = y * charHeight + ascent;

                g2d.drawString(String.valueOf(ch), charX, charY);

                // we don't chain all the extension methods
                // as IntelliJ tends to blow up when analyzing the code
                JsonExtensions.beginRecord(json, CHARSET.charAt(i), DEBUG);
                JsonExtensions.appendValue(json, Glyph.X_PROPERTY, charX, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.Y_PROPERTY, y * charHeight, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.WIDTH_PROPERTY, width, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.HEIGHT_PROPERTY, charHeight, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.ASCENT_PROPERTY, ascent, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.DESCENT_PROPERTY, descent, DEBUG).separator(DEBUG);
                JsonExtensions.appendValue(json, Glyph.ADVANCE_PROPERTY, advance, DEBUG).finishObject(DEBUG);
                if (i < CHARSET.length() - 1) {
                    json.append(",");
                }
                json.append(DEBUG ? "\n" : "");

                x++;
                if (x >= cols) {
                    x = 0;
                    y++;
                }
            }

            json.append(DEBUG ? "}\n" : "}");
            g2d.dispose();

            String outputFileName = outputDir + fontName + "_" + fontSize + "_" + fontStyle;

            // Save atlas and metadata
            ImageIO.write(atlas, "png", new File(outputFileName + ".png"));
            try (FileWriter writer = new FileWriter(outputFileName + ".json")) {
                writer.write(json.toString());
            }

            System.out.println("Generated: " + outputFileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        generateFontAtlas("Helvetica", 13, Font.BOLD, "web/fonts/");
    }
}
