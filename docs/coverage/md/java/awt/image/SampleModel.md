# Class: `SampleModel` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Full Name:** `java.awt.image.SampleModel`

**Coverage:** 11 / 44 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

## ✓ Implemented Methods

- `public abstract int getNumDataElements()`
- `public abstract int getSample(int, int, int, java.awt.image.DataBuffer)`
- `public abstract java.awt.image.DataBuffer createDataBuffer()`
- `public abstract java.awt.image.SampleModel createCompatibleSampleModel(int, int)`
- `public abstract java.lang.Object getDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public abstract void setDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public abstract void setSample(int, int, int, int, java.awt.image.DataBuffer)`

## ✗ Missing Methods

- `public abstract int getSampleSize(int)`
- `public abstract int[] getSampleSize()`
- `public abstract java.awt.image.SampleModel createSubsetSampleModel(int[])`
- `public double getSampleDouble(int, int, int, java.awt.image.DataBuffer)`
- `public double[] getPixel(int, int, double[], java.awt.image.DataBuffer)`
- `public double[] getPixels(int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public double[] getSamples(int, int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public final int getDataType()`
- `public final int getHeight()`
- `public final int getNumBands()`
- `public final int getWidth()`
- `public float getSampleFloat(int, int, int, java.awt.image.DataBuffer)`
- `public float[] getPixel(int, int, float[], java.awt.image.DataBuffer)`
- `public float[] getPixels(int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public float[] getSamples(int, int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public int getTransferType()`
- `public int[] getPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public int[] getPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public int[] getSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public java.lang.Object getDataElements(int, int, int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setDataElements(int, int, int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setPixel(int, int, double[], java.awt.image.DataBuffer)`
- `public void setPixel(int, int, float[], java.awt.image.DataBuffer)`
- `public void setPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public void setSample(int, int, int, double, java.awt.image.DataBuffer)`
- `public void setSample(int, int, int, float, java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`

## ✓ Implemented Fields

- `protected int dataType`
- `protected int height`
- `protected int numBands`
- `protected int width`

## ✗ Missing Constructors

- `public java.awt.image.SampleModel(int, int, int, int)`


[← Back to Package](index.md)
