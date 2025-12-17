/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/surface-operation.yaml
 * 
 * Operations that can be performed on a surface
 */

export enum SurfaceOperation {
  /** No operation */
  CMD_NO_OP = 0,
  /** Set foreground or background color */
  CMD_SET_COLOR = 1,
  /** Set affine transformation matrix */
  CMD_SET_TRANSFORM = 2,
  /** Set clipping rectangle */
  CMD_SET_CLIP_RECT = 3,
  /** Set compositing mode (Porter-Duff alpha blending) */
  CMD_SET_COMPOSITE = 4,
  /** Copy image to surface */
  CMD_BLIT_IMAGE = 5,
  /** Draw rectangle outline */
  CMD_DRAW_RECT = 6,
  /** Fill rectangle */
  CMD_FILL_RECT = 7,
  /** Clear rectangle */
  CMD_CLEAR_RECT = 8,
  /** Draw line */
  CMD_DRAW_LINE = 9,
  /** Draw a polygon */
  CMD_DRAW_POLYGON = 10,
  /** Fills a polygon */
  CMD_FILL_POLYGON = 11,
  /** Fills a rounded rectangle */
  CMD_FILL_ROUND_RECT = 12,
  /** Fills an arc */
  CMD_FILL_ARC = 13,
}
