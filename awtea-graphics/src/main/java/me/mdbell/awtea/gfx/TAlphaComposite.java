package me.mdbell.awtea.gfx;

/**
 * The TAlphaComposite class implements alpha blending rules for combining source and destination pixels.
 * This class corresponds to java.awt.AlphaComposite.
 *
 * <p>
 * The following compositing rules are supported:
 * <ul>
 * <li>CLEAR - Clear destination (alpha = 0)</li>
 * <li>SRC - Copy source to destination, replacing destination</li>
 * <li>DST - Leave destination unchanged</li>
 * <li>SRC_OVER - Source over destination (Porter-Duff Source Over)</li>
 * <li>DST_OVER - Destination over source</li>
 * <li>SRC_IN - Source where destination is opaque</li>
 * <li>DST_IN - Destination where source is opaque</li>
 * <li>SRC_OUT - Source where destination is transparent</li>
 * <li>DST_OUT - Destination where source is transparent</li>
 * <li>SRC_ATOP - Source over destination, only where destination is opaque</li>
 * <li>DST_ATOP - Destination over source, only where source is opaque</li>
 * <li>XOR - Source xor destination</li>
 * </ul>
 * </p>
 *
 * @see TComposite
 */
public final class TAlphaComposite implements TComposite {

	/**
	 * Porter-Duff Clear rule: Clear destination (alpha = 0).
	 * Ar = 0
	 */
	public static final int CLEAR = 1;

	/**
	 * Porter-Duff Source rule: Copy source to destination, replacing destination.
	 * Ar = As
	 */
	public static final int SRC = 2;

	/**
	 * Porter-Duff Destination rule: Leave destination unchanged.
	 * Ar = Ad
	 */
	public static final int DST = 9;

	/**
	 * Porter-Duff Source Over Destination rule: Source over destination (default blending).
	 * Ar = As + Ad*(1-As)
	 */
	public static final int SRC_OVER = 3;

	/**
	 * Porter-Duff Destination Over Source rule: Destination over source.
	 * Ar = Ad + As*(1-Ad)
	 */
	public static final int DST_OVER = 4;

	/**
	 * Porter-Duff Source In Destination rule: Source where destination is opaque.
	 * Ar = As * Ad
	 */
	public static final int SRC_IN = 5;

	/**
	 * Porter-Duff Destination In Source rule: Destination where source is opaque.
	 * Ar = Ad * As
	 */
	public static final int DST_IN = 6;

	/**
	 * Porter-Duff Source Out Destination rule: Source where destination is transparent.
	 * Ar = As * (1-Ad)
	 */
	public static final int SRC_OUT = 7;

	/**
	 * Porter-Duff Destination Out Source rule: Destination where source is transparent.
	 * Ar = Ad * (1-As)
	 */
	public static final int DST_OUT = 8;

	/**
	 * Porter-Duff Source Atop Destination rule.
	 * Ar = As*Ad + Ad*(1-As) = Ad
	 */
	public static final int SRC_ATOP = 10;

	/**
	 * Porter-Duff Destination Atop Source rule.
	 * Ar = Ad*As + As*(1-Ad) = As
	 */
	public static final int DST_ATOP = 11;

	/**
	 * Porter-Duff Xor rule: Source xor destination.
	 * Ar = As*(1-Ad) + Ad*(1-As)
	 */
	public static final int XOR = 12;

	/**
	 * TAlphaComposite instance for SRC_OVER with alpha = 1.0.
	 */
	public static final TAlphaComposite SrcOver = new TAlphaComposite(SRC_OVER, 1.0f);

	/**
	 * TAlphaComposite instance for SRC with alpha = 1.0.
	 */
	public static final TAlphaComposite Src = new TAlphaComposite(SRC, 1.0f);

	/**
	 * TAlphaComposite instance for CLEAR.
	 */
	public static final TAlphaComposite Clear = new TAlphaComposite(CLEAR, 1.0f);

	/**
	 * TAlphaComposite instance for DST.
	 */
	public static final TAlphaComposite Dst = new TAlphaComposite(DST, 1.0f);

	private final int rule;
	private final float alpha;

	private TAlphaComposite(int rule, float alpha) {
		if (rule < CLEAR || rule > XOR) {
			throw new IllegalArgumentException("Invalid composite rule: " + rule);
		}
		if (alpha < 0.0f || alpha > 1.0f) {
			throw new IllegalArgumentException("Alpha value out of range: " + alpha);
		}
		this.rule = rule;
		this.alpha = alpha;
	}

	/**
	 * Creates an TAlphaComposite object with the specified rule.
	 *
	 * @param rule the compositing rule
	 * @return an TAlphaComposite object with the specified rule and alpha = 1.0
	 * @throws IllegalArgumentException if rule is not a valid compositing rule
	 */
	public static TAlphaComposite getInstance(int rule) {
		return getInstance(rule, 1.0f);
	}

	/**
	 * Creates an TAlphaComposite object with the specified rule and alpha value.
	 *
	 * @param rule  the compositing rule
	 * @param alpha the alpha value (0.0 to 1.0)
	 * @return an TAlphaComposite object
	 * @throws IllegalArgumentException if rule is not valid or alpha is out of range
	 */
	public static TAlphaComposite getInstance(int rule, float alpha) {
		if (alpha == 1.0f) {
			switch (rule) {
				case SRC_OVER:
					return SrcOver;
				case SRC:
					return Src;
				case CLEAR:
					return Clear;
				case DST:
					return Dst;
			}
		}
		return new TAlphaComposite(rule, alpha);
	}

	/**
	 * Returns the compositing rule of this TAlphaComposite object.
	 *
	 * @return the compositing rule
	 */
	public int getRule() {
		return rule;
	}

	/**
	 * Returns the alpha value of this TAlphaComposite object.
	 *
	 * @return the alpha value
	 */
	public float getAlpha() {
		return alpha;
	}

	@Override
	public int hashCode() {
		return Float.floatToIntBits(alpha) * 31 + rule;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TAlphaComposite)) {
			return false;
		}
		TAlphaComposite other = (TAlphaComposite) obj;
		return rule == other.rule && alpha == other.alpha;
	}
}
