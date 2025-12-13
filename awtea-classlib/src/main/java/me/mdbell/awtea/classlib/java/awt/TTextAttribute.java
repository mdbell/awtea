package me.mdbell.awtea.classlib.java.awt;

import java.text.AttributedCharacterIterator;

public class TTextAttribute extends AttributedCharacterIterator.Attribute {

	public static final TTextAttribute FAMILY = new TTextAttribute("family");
	public static final TTextAttribute WEIGHT = new TTextAttribute("weight");
	public static final TTextAttribute WIDTH = new TTextAttribute("width");
	public static final TTextAttribute POSTURE = new TTextAttribute("posture");
	public static final TTextAttribute SIZE = new TTextAttribute("size");
	public static final TTextAttribute TRANSFORM = new TTextAttribute("transform");
	public static final TTextAttribute SUPERSCRIPT = new TTextAttribute("superscript");
	public static final TTextAttribute FONT = new TTextAttribute("font");
	public static final TTextAttribute CHAR_REPLACEMENT = new TTextAttribute("char_replacement");
	public static final TTextAttribute FOREGROUND = new TTextAttribute("foreground");
	public static final TTextAttribute BACKGROUND = new TTextAttribute("background");
	public static final TTextAttribute UNDERLINE = new TTextAttribute("underline");
	public static final TTextAttribute STRIKETHROUGH = new TTextAttribute("strikethrough");
	public static final TTextAttribute RUN_DIRECTION = new TTextAttribute("run_direction");
	public static final TTextAttribute BIDI_EMBEDDING = new TTextAttribute("bidi_embedding");
	public static final TTextAttribute JUSTIFICATION = new TTextAttribute("justification");
	public static final TTextAttribute INPUT_METHOD_HIGHLIGHT = new TTextAttribute("input method highlight");
	public static final TTextAttribute INPUT_METHOD_UNDERLINE = new TTextAttribute("input method underline");
	public static final TTextAttribute SWAP_COLORS = new TTextAttribute("swap_colors");
	public static final TTextAttribute NUMERIC_SHAPING = new TTextAttribute("numeric_shaping");
	public static final TTextAttribute KERNING = new TTextAttribute("kerning");
	public static final TTextAttribute LIGATURES = new TTextAttribute("ligatures");
	public static final TTextAttribute TRACKING = new TTextAttribute("tracking");

	public static final Float WEIGHT_EXTRA_LIGHT = 0.5f;
	public static final Float WEIGHT_LIGHT = 0.75f;
	public static final Float WEIGHT_DEMILIGHT = 0.875f;
	public static final Float WEIGHT_REGULAR = 1.0f;
	public static final Float WEIGHT_SEMIBOLD = 1.25f;
	public static final Float WEIGHT_MEDIUM = 1.375f;
	public static final Float WEIGHT_DEMIBOLD = 1.75f;
	public static final Float WEIGHT_BOLD = 1.5f;
	public static final Float WEIGHT_HEAVY = 2.0f;
	public static final Float WEIGHT_EXTRABOLD = 2.25f;
	public static final Float WEIGHT_ULTRABOLD = 2.5f;

	public static final Float WIDTH_CONDENSED = 0.75f;
	public static final Float WIDTH_SEMI_CONDENSED = 0.875f;
	public static final Float WIDTH_REGULAR = 1.0f;
	public static final Float WIDTH_SEMI_EXTENDED = 1.25f;
	public static final Float WIDTH_EXTENDED = 1.5f;

	public static final Float POSTURE_REGULAR = 0.0f;
	public static final Float POSTURE_OBLIQUE = 0.20f;

	public static final Integer SUPERSCRIPT_SUPER = 1;
	public static final Integer SUPERSCRIPT_SUB = -1;

	public static final Integer UNDERLINE_ON = 0;

	public static final Boolean STRIKETHROUGH_ON = true;

	public static final Boolean RUN_DIRECTION_LTR = false;
	public static final Boolean RUN_DIRECTION_RTL = true;

	public static final Float JUSTIFICATION_FULL = 1.0f;
	public static final Float JUSTIFICATION_NONE = 0.0f;

	public static final Integer UNDERLINE_LOW_ONE_PIXEL = 1;
	public static final Integer UNDERLINE_LOW_TWO_PIXEL = 2;
	public static final Integer UNDERLINE_LOW_DOTTED = 3;
	public static final Integer UNDERLINE_LOW_GRAY = 4;
	public static final Integer UNDERLINE_LOW_DASHED = 5;

	public static final Boolean SWAP_COLORS_ON = true;

	public static final Integer KERNING_ON = 1;

	public static final Integer LIGATURES_ON = 1;

	public static final Float TRACKING_TIGHT = -0.04f;
	public static final Float TRACKING_LOOSE = 0.04f;

	/**
	 * Constructs an {@code Attribute} with the given name.
	 *
	 * @param name the name of {@code Attribute}
	 */
	protected TTextAttribute(String name) {
		super(name);
	}
}
