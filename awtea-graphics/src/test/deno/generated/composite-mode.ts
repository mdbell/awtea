/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/composite-mode.yaml
 * 
 * Porter-Duff alpha compositing modes
 */

export enum CompositeMode {
  /** Clear destination (alpha = 0) */
  COMPOSITE_CLEAR = 1,
  /** Copy source to destination, replacing destination */
  COMPOSITE_SRC = 2,
  /** Source over destination (default) */
  COMPOSITE_SRC_OVER = 3,
  /** Destination over source */
  COMPOSITE_DST_OVER = 4,
  /** Source where destination is opaque */
  COMPOSITE_SRC_IN = 5,
  /** Destination where source is opaque */
  COMPOSITE_DST_IN = 6,
  /** Source where destination is transparent */
  COMPOSITE_SRC_OUT = 7,
  /** Destination where source is transparent */
  COMPOSITE_DST_OUT = 8,
  /** Leave destination unchanged */
  COMPOSITE_DST = 9,
  /** Source over destination, only where destination is opaque */
  COMPOSITE_SRC_ATOP = 10,
  /** Destination over source, only where source is opaque */
  COMPOSITE_DST_ATOP = 11,
  /** Source xor destination */
  COMPOSITE_XOR = 12,
}
