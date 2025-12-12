# Class: `Raster` ![Coverage](https://img.shields.io/badge/coverage-23.3%25-red)

**Full Name:** `java.awt.image.Raster`

**Coverage:** 14 / 60 (23.3%)

```
[███████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 23.3%
```

## ✓ Implemented Methods

- `public int getSample(int, int, int)`
- `public int[] getPixel(int, int, int[])`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.image.Raster createChild(int, int, int, int, int, int, int[])`
- `public java.awt.image.Raster createTranslatedChild(int, int)`
- `public java.awt.image.SampleModel getSampleModel()`
- `public java.lang.Object getDataElements(int, int, java.lang.Object)`

## ✗ Missing Methods

- `public double getSampleDouble(int, int, int)`
- `public double[] getPixel(int, int, double[])`
- `public double[] getPixels(int, int, int, int, double[])`
- `public double[] getSamples(int, int, int, int, int, double[])`
- `public final int getHeight()`
- `public final int getMinX()`
- `public final int getMinY()`
- `public final int getNumBands()`
- `public final int getNumDataElements()`
- `public final int getSampleModelTranslateX()`
- `public final int getSampleModelTranslateY()`
- `public final int getTransferType()`
- `public final int getWidth()`
- `public float getSampleFloat(int, int, int)`
- `public float[] getPixel(int, int, float[])`
- `public float[] getPixels(int, int, int, int, float[])`
- `public float[] getSamples(int, int, int, int, int, float[])`
- `public int[] getPixels(int, int, int, int, int[])`
- `public int[] getSamples(int, int, int, int, int, int[])`
- `public java.awt.image.DataBuffer getDataBuffer()`
- `public java.awt.image.Raster getParent()`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster()`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int, int, int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(java.awt.Rectangle)`
- `public java.lang.Object getDataElements(int, int, int, int, java.lang.Object)`
- `public static java.awt.image.Raster createRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(int, int, int, int, int[], int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(java.awt.image.DataBuffer, int, int, int, int[], int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(int, int, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(java.awt.image.DataBuffer, int, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(int, int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(java.awt.image.DataBuffer, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(java.awt.image.DataBuffer, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createWritableRaster(java.awt.image.SampleModel, java.awt.Point)`
- `public static java.awt.image.WritableRaster createWritableRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`

## ✓ Implemented Fields

- `protected int height`
- `protected int minX`
- `protected int minY`
- `protected int sampleModelTranslateX`
- `protected int sampleModelTranslateY`
- `protected int width`
- `protected java.awt.image.SampleModel sampleModel`

## ✗ Missing Fields

- `protected int numBands`
- `protected int numDataElements`
- `protected java.awt.image.DataBuffer dataBuffer`
- `protected java.awt.image.Raster parent`

## ✗ Missing Constructors

- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.Point)`
- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`
- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Rectangle, java.awt.Point, java.awt.image.Raster)`


[← Back to Package](java_awt_image.md)
