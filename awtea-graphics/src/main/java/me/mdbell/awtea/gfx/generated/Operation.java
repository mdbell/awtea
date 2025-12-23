/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/surface-operation.yaml
 * 
 * Operations that can be performed on a surface
 */
package me.mdbell.awtea.gfx.generated;

public enum Operation {
	/** No operation */
	NO_OP(0),
	/** Set foreground or background color */
	SET_COLOR(1),
	/** Set affine transformation matrix */
	SET_TRANSFORM(2),
	/** Set clipping rectangle */
	SET_CLIP_RECT(3),
	/** Set compositing mode (Porter-Duff alpha blending) */
	SET_COMPOSITE(4),
	/** Copy image to surface */
	BLIT_IMAGE(5),
	/** Draw rectangle outline */
	DRAW_RECT(6),
	/** Fill rectangle */
	FILL_RECT(7),
	/** Clear rectangle */
	CLEAR_RECT(8),
	/** Draw line */
	DRAW_LINE(9),
	/** Draw a polygon */
	DRAW_POLYGON(10),
	/** Fills a polygon */
	FILL_POLYGON(11),
	/** Fills an oval */
	FILL_OVAL(12),
	/** Fills a rounded rectangle */
	FILL_ROUND_RECT(13),
	/** Fills an arc */
	FILL_ARC(14),
	/** Draw oval outline */
	DRAW_OVAL(15),
	/** Draw arc outline */
	DRAW_ARC(16),
	/** Draw rounded rectangle outline */
	DRAW_ROUND_RECT(17),
	/** Draw connected line segments */
	DRAW_POLYLINE(18),
	/** Copy area of surface with offset */
	COPY_AREA(19),
	/** Execute custom rendering callback (e.g. for custom shaders) */
	RENDER_CALLBACK(20);

	private final int value;

	Operation(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
