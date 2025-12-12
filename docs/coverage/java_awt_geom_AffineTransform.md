# Class: `AffineTransform` ![Coverage](https://img.shields.io/badge/coverage-98.7%25-green)

**Full Name:** `java.awt.geom.AffineTransform`

**Coverage:** 74 / 75 (98.7%)

```
[█████████████████████████████████████████████████░] 98.7%
```

## ✓ Implemented Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isIdentity()`
- `public double getDeterminant()`
- `public double getScaleX()`
- `public double getScaleY()`
- `public double getShearX()`
- `public double getShearY()`
- `public double getTranslateX()`
- `public double getTranslateY()`
- `public int getType()`
- `public int hashCode()`
- `public java.awt.geom.AffineTransform createInverse()`
- `public java.awt.geom.Point2D deltaTransform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.awt.geom.Point2D inverseTransform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.awt.geom.Point2D transform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.lang.Object clone()`
- `public java.lang.String toString()`
- `public static java.awt.geom.AffineTransform getQuadrantRotateInstance(int)`
- `public static java.awt.geom.AffineTransform getQuadrantRotateInstance(int, double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double, double, double)`
- `public static java.awt.geom.AffineTransform getScaleInstance(double, double)`
- `public static java.awt.geom.AffineTransform getShearInstance(double, double)`
- `public static java.awt.geom.AffineTransform getTranslateInstance(double, double)`
- `public void concatenate(java.awt.geom.AffineTransform)`
- `public void deltaTransform(double[], int, double[], int, int)`
- `public void getMatrix(double[])`
- `public void inverseTransform(double[], int, double[], int, int)`
- `public void invert()`
- `public void preConcatenate(java.awt.geom.AffineTransform)`
- `public void quadrantRotate(int)`
- `public void quadrantRotate(int, double, double)`
- `public void rotate(double)`
- `public void rotate(double, double)`
- `public void rotate(double, double, double)`
- `public void rotate(double, double, double, double)`
- `public void scale(double, double)`
- `public void setToIdentity()`
- `public void setToQuadrantRotation(int)`
- `public void setToQuadrantRotation(int, double, double)`
- `public void setToRotation(double)`
- `public void setToRotation(double, double)`
- `public void setToRotation(double, double, double)`
- `public void setToRotation(double, double, double, double)`
- `public void setToScale(double, double)`
- `public void setToShear(double, double)`
- `public void setToTranslation(double, double)`
- `public void setTransform(double, double, double, double, double, double)`
- `public void setTransform(java.awt.geom.AffineTransform)`
- `public void shear(double, double)`
- `public void transform(double[], int, double[], int, int)`
- `public void transform(double[], int, float[], int, int)`
- `public void transform(float[], int, double[], int, int)`
- `public void transform(float[], int, float[], int, int)`
- `public void transform(java.awt.geom.Point2D[], int, java.awt.geom.Point2D[], int, int)`
- `public void translate(double, double)`

## ✗ Missing Methods

- `public java.awt.Shape createTransformedShape(java.awt.Shape)`

## ✓ Implemented Fields

- `public static final int TYPE_FLIP`
- `public static final int TYPE_GENERAL_ROTATION`
- `public static final int TYPE_GENERAL_SCALE`
- `public static final int TYPE_GENERAL_TRANSFORM`
- `public static final int TYPE_IDENTITY`
- `public static final int TYPE_MASK_ROTATION`
- `public static final int TYPE_MASK_SCALE`
- `public static final int TYPE_QUADRANT_ROTATION`
- `public static final int TYPE_TRANSLATION`
- `public static final int TYPE_UNIFORM_SCALE`

## ✓ Implemented Constructors

- `public java.awt.geom.AffineTransform()`
- `public java.awt.geom.AffineTransform(double, double, double, double, double, double)`
- `public java.awt.geom.AffineTransform(double[])`
- `public java.awt.geom.AffineTransform(float, float, float, float, float, float)`
- `public java.awt.geom.AffineTransform(float[])`
- `public java.awt.geom.AffineTransform(java.awt.geom.AffineTransform)`


[← Back to Package](java_awt_geom.md)
