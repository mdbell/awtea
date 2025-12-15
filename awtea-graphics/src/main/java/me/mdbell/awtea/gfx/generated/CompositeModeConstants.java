/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/composite-mode.yaml
 * 
 * Porter-Duff alpha compositing modes
 */
package me.mdbell.awtea.gfx.generated;

public interface CompositeModeConstants {
	/** Clear destination (alpha = 0) */
	int COMPOSITE_CLEAR = 1;
	/** Copy source to destination, replacing destination */
	int COMPOSITE_SRC = 2;
	/** Source over destination (default) */
	int COMPOSITE_SRC_OVER = 3;
	/** Destination over source */
	int COMPOSITE_DST_OVER = 4;
	/** Source where destination is opaque */
	int COMPOSITE_SRC_IN = 5;
	/** Destination where source is opaque */
	int COMPOSITE_DST_IN = 6;
	/** Source where destination is transparent */
	int COMPOSITE_SRC_OUT = 7;
	/** Destination where source is transparent */
	int COMPOSITE_DST_OUT = 8;
	/** Leave destination unchanged */
	int COMPOSITE_DST = 9;
	/** Source over destination, only where destination is opaque */
	int COMPOSITE_SRC_ATOP = 10;
	/** Destination over source, only where source is opaque */
	int COMPOSITE_DST_ATOP = 11;
	/** Source xor destination */
	int COMPOSITE_XOR = 12;
}
