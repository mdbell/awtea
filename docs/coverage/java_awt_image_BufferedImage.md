# Class: `BufferedImage` ![Coverage](https://img.shields.io/badge/coverage-32.8%25-orange)

**Full Name:** `java.awt.image.BufferedImage`

**Coverage:** 21 / 64 (32.8%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 32.8%
```

## ✓ Implemented Methods

- `public boolean isAlphaPremultiplied()`
- `public int getHeight()`
- `public int getHeight(java.awt.image.ImageObserver)`
- `public int getRGB(int, int)`
- `public int getWidth()`
- `public int getWidth(java.awt.image.ImageObserver)`
- `public int[] getRGB(int, int, int, int, int[], int, int)`
- `public java.awt.Graphics getGraphics()`
- `public java.awt.image.ColorModel getColorModel()`
- `public java.awt.image.ImageProducer getSource()`
- `public java.awt.image.WritableRaster getRaster()`
- `public java.lang.Object getProperty(java.lang.String, java.awt.image.ImageObserver)`
- `public void setRGB(int, int, int)`
- `public void setRGB(int, int, int, int, int[], int, int)`

## ✗ Missing Methods

- `public boolean hasTileWriters()`
- `public boolean isTileWritable(int, int)`
- `public int getMinTileX()`
- `public int getMinTileY()`
- `public int getMinX()`
- `public int getMinY()`
- `public int getNumXTiles()`
- `public int getNumYTiles()`
- `public int getTileGridXOffset()`
- `public int getTileGridYOffset()`
- `public int getTileHeight()`
- `public int getTileWidth()`
- `public int getTransparency()`
- `public int getType()`
- `public java.awt.Graphics2D createGraphics()`
- `public java.awt.Point[] getWritableTileIndices()`
- `public java.awt.image.BufferedImage getSubimage(int, int, int, int)`
- `public java.awt.image.Raster getData()`
- `public java.awt.image.Raster getData(java.awt.Rectangle)`
- `public java.awt.image.Raster getTile(int, int)`
- `public java.awt.image.SampleModel getSampleModel()`
- `public java.awt.image.WritableRaster copyData(java.awt.image.WritableRaster)`
- `public java.awt.image.WritableRaster getAlphaRaster()`
- `public java.awt.image.WritableRaster getWritableTile(int, int)`
- `public java.lang.Object getProperty(java.lang.String)`
- `public java.lang.String toString()`
- `public java.lang.String[] getPropertyNames()`
- `public java.util.Vector getSources()`
- `public void addTileObserver(java.awt.image.TileObserver)`
- `public void coerceData(boolean)`
- `public void releaseWritableTile(int, int)`
- `public void removeTileObserver(java.awt.image.TileObserver)`
- `public void setData(java.awt.image.Raster)`

## ✓ Implemented Fields

- `public static final int TYPE_CUSTOM`
- `public static final int TYPE_INT_ARGB`
- `public static final int TYPE_INT_ARGB_PRE`
- `public static final int TYPE_INT_BGR`
- `public static final int TYPE_INT_RGB`

## ✗ Missing Fields

- `public static final int TYPE_3BYTE_BGR`
- `public static final int TYPE_4BYTE_ABGR`
- `public static final int TYPE_4BYTE_ABGR_PRE`
- `public static final int TYPE_BYTE_BINARY`
- `public static final int TYPE_BYTE_GRAY`
- `public static final int TYPE_BYTE_INDEXED`
- `public static final int TYPE_USHORT_555_RGB`
- `public static final int TYPE_USHORT_565_RGB`
- `public static final int TYPE_USHORT_GRAY`

## ✓ Implemented Constructors

- `public java.awt.image.BufferedImage(int, int, int)`
- `public java.awt.image.BufferedImage(java.awt.image.ColorModel, java.awt.image.WritableRaster, boolean, java.util.Hashtable)`

## ✗ Missing Constructors

- `public java.awt.image.BufferedImage(int, int, int, java.awt.image.IndexColorModel)`


[← Back to Package](java_awt_image.md)
