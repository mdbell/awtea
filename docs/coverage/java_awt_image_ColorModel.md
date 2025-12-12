# Class: `ColorModel` ![Coverage](https://img.shields.io/badge/coverage-33.3%25-orange)

**Full Name:** `java.awt.image.ColorModel`

**Coverage:** 15 / 45 (33.3%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 33.3%
```

## ✓ Implemented Methods

- `public abstract int getAlpha(int)`
- `public abstract int getBlue(int)`
- `public abstract int getGreen(int)`
- `public abstract int getRed(int)`
- `public int getDataElement(int[], int)`
- `public int getNumColorComponents()`
- `public int getNumComponents()`
- `public int getPixelSize()`
- `public int getRGB(int)`
- `public int getTransparency()`
- `public int[] getComponents(int, int[], int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int)`
- `public java.lang.Object getDataElements(int, java.lang.Object)`
- `public static java.awt.image.ColorModel getRGBdefault()`

## ✗ Missing Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isCompatibleRaster(java.awt.image.Raster)`
- `public boolean isCompatibleSampleModel(java.awt.image.SampleModel)`
- `public final boolean hasAlpha()`
- `public final boolean isAlphaPremultiplied()`
- `public final int getTransferType()`
- `public final java.awt.color.ColorSpace getColorSpace()`
- `public float[] getNormalizedComponents(int[], int, float[], int)`
- `public float[] getNormalizedComponents(java.lang.Object, float[], int)`
- `public int getAlpha(java.lang.Object)`
- `public int getBlue(java.lang.Object)`
- `public int getComponentSize(int)`
- `public int getDataElement(float[], int)`
- `public int getGreen(java.lang.Object)`
- `public int getRGB(java.lang.Object)`
- `public int getRed(java.lang.Object)`
- `public int hashCode()`
- `public int[] getComponentSize()`
- `public int[] getComponents(java.lang.Object, int[], int)`
- `public int[] getUnnormalizedComponents(float[], int, int[], int)`
- `public java.awt.image.ColorModel coerceData(java.awt.image.WritableRaster, boolean)`
- `public java.awt.image.SampleModel createCompatibleSampleModel(int, int)`
- `public java.awt.image.WritableRaster getAlphaRaster(java.awt.image.WritableRaster)`
- `public java.lang.Object getDataElements(float[], int, java.lang.Object)`
- `public java.lang.Object getDataElements(int[], int, java.lang.Object)`
- `public java.lang.String toString()`
- `public void finalize()`

## ✓ Implemented Fields

- `protected int transferType`

## ✗ Missing Fields

- `protected int pixel_bits`

## ✗ Missing Constructors

- `protected java.awt.image.ColorModel(int, int[], java.awt.color.ColorSpace, boolean, boolean, int, int)`
- `public java.awt.image.ColorModel(int)`


[← Back to Package](java_awt_image.md)
