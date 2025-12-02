package me.mdbell.awtea.classlib.java.awt.geom;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

/**
 * @see java.awt.geom.AffineTransform
 */
@ToString
@EqualsAndHashCode
public class TAffineTransform implements Cloneable{

	private static final int TYPE_UNKNOWN = -1;

	private static final double EPS = 1e-10;

	public static final int TYPE_IDENTITY = 0;
	public static final int TYPE_TRANSLATION = 1;
	public static final int TYPE_UNIFORM_SCALE = 2;
	public static final int TYPE_GENERAL_SCALE = 4;

	public static final int TYPE_MASK_SCALE = (TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE);

	// this was added after the other types, thus it is out of order
	public static final int TYPE_FLIP = 64;

	public static final int TYPE_QUADRANT_ROTATION = 8;

	public static final int TYPE_GENERAL_ROTATION = 16;

	public static final int TYPE_MASK_ROTATION = (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION);

	public static final int TYPE_GENERAL_TRANSFORM = 32;

	double m00;
	double m10;
	double m01;
	double m11;
	double m02;
	double m12;

	private int type;

	public TAffineTransform() {
		// identity matrix
		m00 = 1.0;
		m11 = 1.0;
	}

	public TAffineTransform(TAffineTransform src) {
		this.m00 = src.m00;
		this.m10 = src.m10;
		this.m01 = src.m01;
		this.m11 = src.m11;
		this.m02 = src.m02;
		this.m12 = src.m12;
		this.type = src.type;
	}

	public TAffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;
		updateState();
	}

	public TAffineTransform(float[] matrix) {
		m00 = matrix[0];
		m10 = matrix[1];
		m01 = matrix[2];
		m11 = matrix[3];
		if (matrix.length > 5) {
			m02 = matrix[4];
			m12 = matrix[5];
		}
		updateState();
	}

	public TAffineTransform(double m00, double m10,
						   double m01, double m11,
						   double m02, double m12){
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;
		updateState();
	}

	public TAffineTransform(double[] matrix) {
		m00 = matrix[0];
		m10 = matrix[1];
		m01 = matrix[2];
		m11 = matrix[3];
		if (matrix.length > 5) {
			m02 = matrix[4];
			m12 = matrix[5];
		}
		updateState();
	}

	void updateState() {
		// Mark as dirty, will be recomputed lazily.
		type = TYPE_UNKNOWN;
	}

	private void calculateType() {
		int t = TYPE_IDENTITY;

		// Translation part
		boolean hasTx = !nearZero(m02);
		boolean hasTy = !nearZero(m12);
		if (hasTx || hasTy) {
			t |= TYPE_TRANSLATION;
		}

		// Linear part components
		double sx = m00;
		double sy = m11;
		double shx = m01;
		double shy = m10;

		boolean hasShear = !nearZero(shx) || !nearZero(shy);
		boolean hasScaleX = !nearOne(sx);
		boolean hasScaleY = !nearOne(sy);
		boolean hasScale = hasScaleX || hasScaleY;

		// Quick identity check
		if (!hasShear && !hasScale && !hasTx && !hasTy) {
			type = TYPE_IDENTITY;
			return;
		}

		double det = sx * sy - shx * shy;
		if (det < 0.0) {
			t |= TYPE_FLIP;
		}

		// No shear: pure scale + optional translation
		if (!hasShear) {
			if (!hasScale) {
				// pure translation (already set above)
				type = t;
				return;
			}

			double asx = Math.abs(sx);
			double asy = Math.abs(sy);

			if (nearZero(asx - asy)) {
				t |= TYPE_UNIFORM_SCALE;
			} else {
				t |= TYPE_GENERAL_SCALE;
			}

			type = t;
			return;
		}

		// If we have shear, we at least have some rotation / general transform.
		// Check if the 2x2 linear part looks like a pure rotation (orthonormal).
		double row0len2 = sx * sx + shx * shx;
		double row1len2 = shy * shy + sy * sy;
		double dot = sx * shx + shy * sy;

		boolean orthonormal =
			nearZero(row0len2 - 1.0) &&
				nearZero(row1len2 - 1.0) &&
				nearZero(dot);

		if (orthonormal && nearZero(det - 1.0)) {
			// Pure rotation (no scaling).
			// Quadrant rotation if coefficients are close to 0 or ±1
			boolean quadrantLike =
				(nearZero(sx) || nearZero(Math.abs(sx) - 1.0)) &&
					(nearZero(sy) || nearZero(Math.abs(sy) - 1.0)) &&
					(nearZero(shx) || nearZero(Math.abs(shx) - 1.0)) &&
					(nearZero(shy) || nearZero(Math.abs(shy) - 1.0));

			if (quadrantLike) {
				t |= TYPE_QUADRANT_ROTATION;
			} else {
				t |= TYPE_GENERAL_ROTATION;
			}
		} else {
			// Rotation + scale/shear mix -> fully general
			t |= TYPE_GENERAL_ROTATION | TYPE_GENERAL_TRANSFORM;
		}

		type = t;
	}

	public int getType() {
		if (type == TYPE_UNKNOWN) {
			calculateType();
		}
		return type;
	}

	public boolean isIdentity() {
		return getType() == TYPE_IDENTITY;
	}

	public double getDeterminant() {
		return m00 * m11 - m01 * m10;
	}

	public double getScaleX() {
		return m00;
	}

	public double getScaleY() {
		return m11;
	}

	public double getShearX() {
		return m01;
	}

	public double getShearY() {
		return m10;
	}

	public double getTranslateX() {
		return m02;
	}

	public double getTranslateY() {
		return m12;
	}

	public void getMatrix(double[] matrix) {
		matrix[0] = m00;
		matrix[1] = m10;
		matrix[2] = m01;
		matrix[3] = m11;
		matrix[4] = m02;
		matrix[5] = m12;
	}

	public TAffineTransform createInverse() throws TNoninvertibleTransformException {
		TAffineTransform t = new TAffineTransform(this);
		t.invert();
		return t;
	}

	public void translate(double tx, double ty) {
		m02 = m00 * tx + m01 * ty + m02;
		m12 = m10 * tx + m11 * ty + m12;
		updateState();
	}

	public void rotate(double theta) {
		double sin = Math.sin(theta);
		double cos = Math.cos(theta);

		// Save originals
		double o00 = m00;
		double o01 = m01;
		double o10 = m10;
		double o11 = m11;

		// this = this * R(theta)
		m00 =  cos * o00 + sin * o01;
		m01 = -sin * o00 + cos * o01;

		m10 =  cos * o10 + sin * o11;
		m11 = -sin * o10 + cos * o11;

		updateState();
	}

	public void rotate(double theta, double anchorx, double anchory) {
		translate(anchorx, anchory);
		rotate(theta);
		translate(-anchorx, -anchory);
	}

	public void rotate(double vecX, double vecY) {
		double hypot = Math.hypot(vecX, vecY);
		if (hypot == 0.0) {
			throw new IllegalArgumentException("Zero length vector");
		}
		double sin = vecY / hypot;
		double cos = vecX / hypot;

		// Save originals
		double o00 = m00;
		double o01 = m01;
		double o10 = m10;
		double o11 = m11;

		// this = this * R(vecX, vecY)
		m00 =  cos * o00 + sin * o01;
		m01 = -sin * o00 + cos * o01;

		m10 =  cos * o10 + sin * o11;
		m11 = -sin * o10 + cos * o11;

		updateState();
	}

	public void rotate(double vecX, double vecY, double anchorx, double anchory) {
		translate(anchorx, anchory);
		rotate(vecX, vecY);
		translate(-anchorx, -anchory);
	}

	public void quadrantRotate(int numQuadrants) {
		int nq = numQuadrants % 4;
		if (nq < 0) {
			nq += 4;
		}

		switch (nq) {
			case 0:
				// no-op
				return;
			case 1:
				// 90°: [0 1; -1 0]
				double m00_1 = m00, m01_1 = m01, m10_1 = m10, m11_1 = m11;
				m00 =  m01_1;
				m01 = -m00_1;
				m10 =  m11_1;
				m11 = -m10_1;
				break;
			case 2:
				// 180°: [-1 0; 0 -1]
				m00 = -m00;
				m01 = -m01;
				m10 = -m10;
				m11 = -m11;
				break;
			case 3:
				// 270°: [0 -1; 1 0]
				double m00_3 = m00, m01_3 = m01, m10_3 = m10, m11_3 = m11;
				m00 = -m01_3;
				m01 =  m00_3;
				m10 = -m11_3;
				m11 =  m10_3;
				break;
		}
		updateState();
	}

	public void quadrantRotate(int numQuadrants, double anchorx, double anchory) {
		int nq = numQuadrants % 4;
		if (nq < 0) {
			nq += 4;
		}
		if (nq == 0) {
			return;
		}

		translate(anchorx, anchory);
		quadrantRotate(nq);        // rotate about origin by multiples of 90°
		translate(-anchorx, -anchory);
	}

	public void shear(double shx, double shy) {
		// this = this * S(shx, shy)
		double o00 = m00;
		double o01 = m01;
		double o10 = m10;
		double o11 = m11;

		m00 = o00 + o01 * shy;        // m00' = m00 + m01*shy
		m01 = o00 * shx + o01;        // m01' = m00*shx + m01

		m10 = o10 + o11 * shy;        // m10' = m10 + m11*shy
		m11 = o10 * shx + o11;        // m11' = m10*shx + m11

		// m02, m12 unchanged
		updateState();
	}

	public void scale(double sx, double sy) {
		// this = this * S(sx, sy)
		m00 = m00 * sx;
		m01 = m01 * sy;
		m10 = m10 * sx;
		m11 = m11 * sy;
		// m02, m12 unchanged
		updateState();
	}

	public void invert() throws TNoninvertibleTransformException {
		double det = getDeterminant();
		if (nearZero(det)) {
			throw new TNoninvertibleTransformException("Determinant is zero");
		}
		double invDet = 1.0 / det;

		double nm00 =  m11 * invDet;
		double nm10 = -m10 * invDet;
		double nm01 = -m01 * invDet;
		double nm11 =  m00 * invDet;
		double nm02 = (m01 * m12 - m11 * m02) * invDet;
		double nm12 = (m10 * m02 - m00 * m12) * invDet;
		m00 = nm00;
		m10 = nm10;
		m01 = nm01;
		m11 = nm11;
		m02 = nm02;
		m12 = nm12;
		updateState();
	}

	public void concatenate(TAffineTransform other) {
		double nm00 = m00 * other.m00 + m01 * other.m10;
		double nm10 = m10 * other.m00 + m11 * other.m10;
		double nm01 = m00 * other.m01 + m01 * other.m11;
		double nm11 = m10 * other.m01 + m11 * other.m11;
		double nm02 = m00 * other.m02 + m01 * other.m12 + m02;
		double nm12 = m10 * other.m02 + m11 * other.m12 + m12;
		m00 = nm00;
		m10 = nm10;
		m01 = nm01;
		m11 = nm11;
		m02 = nm02;
		m12 = nm12;
		updateState();
	}

	public void preConcatenate(TAffineTransform other) {
		double nm00 = other.m00 * m00 + other.m01 * m10;
		double nm10 = other.m10 * m00 + other.m11 * m10;
		double nm01 = other.m00 * m01 + other.m01 * m11;
		double nm11 = other.m10 * m01 + other.m11 * m11;
		double nm02 = other.m00 * m02 + other.m01 * m12 + other.m02;
		double nm12 = other.m10 * m02 + other.m11 * m12 + other.m12;
		m00 = nm00;
		m10 = nm10;
		m01 = nm01;
		m11 = nm11;
		m02 = nm02;
		m12 = nm12;
		updateState();
	}

	public void setToTranslation(double tx, double ty) {
		m00 = 1.0;
		m10 = 0.0;
		m01 = 0.0;
		m11 = 1.0;
		m02 = tx;
		m12 = ty;
		if(tx != 0.0 || ty != 0.0) {
			type = TYPE_TRANSLATION;
		} else {
			type = TYPE_IDENTITY;
		}
	}

	public void setToQuadrantRotation(int numQuadrants) {
		int nq = numQuadrants % 4;
		if (nq < 0) {
			nq += 4;
		}

		switch (nq) {
			case 0:
				m00 = 1.0;
				m10 = 0.0;
				m01 = 0.0;
				m11 = 1.0;
				type = TYPE_IDENTITY;
				break;
			case 1:
				m00 = 0.0;
				m10 = 1.0;
				m01 = -1.0;
				m11 = 0.0;
				type = TYPE_QUADRANT_ROTATION;
				break;
			case 2:
				m00 = -1.0;
				m10 = 0.0;
				m01 = 0.0;
				m11 = -1.0;
				type = TYPE_QUADRANT_ROTATION;
				break;
			case 3:
				m00 = 0.0;
				m10 = -1.0;
				m01 = 1.0;
				m11 = 0.0;
				type = TYPE_QUADRANT_ROTATION;
				break;
		}
		m02 = 0.0;
		m12 = 0.0;
	}


	public void setToQuadrantRotation(int numQuadrants, double anchorx, double anchory) {
		setToQuadrantRotation(numQuadrants);
		double sin = m10;
		double oneMinusCos = 1.0 - m00;
		m02 = anchorx * oneMinusCos + anchory * sin;
		m12 = anchory * oneMinusCos - anchorx * sin;
		if (m02 != 0.0 || m12 != 0.0) {
			type |= TYPE_TRANSLATION;
		}
	}

	public void setToRotation(double theta) {
		double sin = Math.sin(theta);
		double cos;
		if (sin == 1.0 || sin == -1.0) {
			cos = 0.0;
			type = TYPE_QUADRANT_ROTATION;
		} else {
			cos = Math.cos(theta);
			if (cos == -1.0) {
				sin = 0.0;
				type = TYPE_QUADRANT_ROTATION;
			} else if (cos == 1.0) {
				sin = 0.0;
				type = TYPE_IDENTITY;
			} else {
				type = TYPE_GENERAL_ROTATION;
			}
		}
		m00 =  cos;
		m10 =  sin;
		m01 = -sin;
		m11 =  cos;
		m02 =  0.0;
		m12 =  0.0;
	}

	public void setToRotation(double vecX, double vecY) {
		double hypot = Math.hypot(vecX, vecY);
		if (hypot == 0.0) {
			throw new IllegalArgumentException("Zero length vector");
		}
		double sin = vecY / hypot;
		double cos = vecX / hypot;

		// Determine type
		if (nearZero(sin)) {
			if (cos > 0.0) {
				type = TYPE_IDENTITY;
			} else {
				sin = 0.0;
				cos = -1.0;
				type = TYPE_QUADRANT_ROTATION;
			}
		} else if (nearZero(cos)) {
			cos = 0.0;
			if (sin > 0.0) {
				type = TYPE_QUADRANT_ROTATION;
			} else {
				sin = -1.0;
				type = TYPE_QUADRANT_ROTATION;
			}
		} else {
			type = TYPE_GENERAL_ROTATION;
		}

		m00 =  cos;
		m10 =  sin;
		m01 = -sin;
		m11 =  cos;
		m02 =  0.0;
		m12 =  0.0;
	}

	public void setToRotation(double theta, double anchorx, double anchory) {
		setToRotation(theta);
		double sin = m10;
		double oneMinusCos = 1.0 - m00;
		m02 = anchorx * oneMinusCos + anchory * sin;
		m12 = anchory * oneMinusCos - anchorx * sin;
		if (m02 != 0.0 || m12 != 0.0) {
			type |= TYPE_TRANSLATION;
		}
	}

	public void setToRotation(double vecX, double vecY, double anchorx, double anchory) {
		setToRotation(vecX, vecY);
		double sin = m10;
		double oneMinusCos = 1.0 - m00;
		m02 = anchorx * oneMinusCos + anchory * sin;
		m12 = anchory * oneMinusCos - anchorx * sin;
		if (m02 != 0.0 || m12 != 0.0) {
			type |= TYPE_TRANSLATION;
		}
	}

	public void setToShear(double shx, double shy) {
		m00 = 1.0;
		m10 = shy;   // shear Y
		m01 = shx;   // shear X
		m11 = 1.0;
		m02 = 0.0;
		m12 = 0.0;

		if (shx != 0.0 || shy != 0.0) {
			// JDK effectively treats pure shear as a "general transform"
			type = TYPE_GENERAL_TRANSFORM;
		} else {
			type = TYPE_IDENTITY;
		}
	}

	public void setToScale(double sx, double sy) {
		m00 = sx;
		m10 = 0.0;
		m01 = 0.0;
		m11 = sy;
		m02 = 0.0;
		m12 = 0.0;
		if (sx == 1.0 && sy == 1.0) {
			type = TYPE_IDENTITY;
		} else if (sx == sy) {
			type = TYPE_UNIFORM_SCALE;
		} else {
			type = TYPE_GENERAL_SCALE;
		}
	}

	public void setToIdentity(){
		m00 = 1.0;
		m10 = 0.0;
		m01 = 0.0;
		m11 = 1.0;
		m02 = 0.0;
		m12 = 0.0;
		type = TYPE_IDENTITY;
	}

	public void setTransform(TAffineTransform Tx) {
		this.m00 = Tx.m00;
		this.m10 = Tx.m10;
		this.m01 = Tx.m01;
		this.m11 = Tx.m11;
		this.m02 = Tx.m02;
		this.m12 = Tx.m12;
		this.type = Tx.type;
	}

	public void setTransform(double m00, double m10,
							 double m01, double m11,
							 double m02, double m12){
		this.m00 = m00;
		this.m10 = m10;
		this.m01 = m01;
		this.m11 = m11;
		this.m02 = m02;
		this.m12 = m12;
		updateState();
	}

	private TPoint2D createOrReuse(TPoint2D dest, TPoint2D src) {
	    if (dest == null) {
			if(src instanceof TPoint2D.Double) {
				dest = new TPoint2D.Double();
			} else {
				dest = new TPoint2D.Float();
			}
		}
		return dest;
	}

	public void deltaTransform(double[] srcPts, int srcOff,
								   double[] dstPts, int dstOff,
								   int numPts) {
		while (--numPts >= 0) {
			double x = srcPts[srcOff++];
			double y = srcPts[srcOff++];

			dstPts[dstOff++] = m00 * x + m01 * y;
			dstPts[dstOff++] = m10 * x + m11 * y;
		}
	}

	public void deltaTransform(float[] srcPts, int srcOff,
								   float[] dstPts, int dstOff,
								   int numPts) {
		while (--numPts >= 0) {
			float x = srcPts[srcOff++];
			float y = srcPts[srcOff++];

			dstPts[dstOff++] = (float)(m00 * x + m01 * y);
			dstPts[dstOff++] = (float)(m10 * x + m11 * y);
		}
	}

	public TPoint2D deltaTransform(TPoint2D src, TPoint2D dest) {
		double x = src.getX();
		double y = src.getY();

		dest = createOrReuse(dest, src);

		dest.setLocation(m00 * x + m01 * y,
						  m10 * x + m11 * y);
		return dest;
	}

	public void transform(double[] srcPts, int srcOff,
							  double[] dstPts, int dstOff,
							  int numPts) {
		while (--numPts >= 0) {
			double x = srcPts[srcOff++];
			double y = srcPts[srcOff++];

			dstPts[dstOff++] = m00 * x + m01 * y + m02;
			dstPts[dstOff++] = m10 * x + m11 * y + m12;
		}
	}

	public void transform(float[] srcPts, int srcOff,
							  float[] dstPts, int dstOff,
							  int numPts) {
		while (--numPts >= 0) {
			float x = srcPts[srcOff++];
			float y = srcPts[srcOff++];

			dstPts[dstOff++] = (float)(m00 * x + m01 * y + m02);
			dstPts[dstOff++] = (float)(m10 * x + m11 * y + m12);
		}
	}

	public void transform(double[] srcPts, int srcOff,
							  float[] dstPts, int dstOff,
							  int numPts) {
		while (--numPts >= 0) {
			double x = srcPts[srcOff++];
			double y = srcPts[srcOff++];

			dstPts[dstOff++] = (float)(m00 * x + m01 * y + m02);
			dstPts[dstOff++] = (float)(m10 * x + m11 * y + m12);
		}
	}

	public void transform(float[] srcPts, int srcOff,
							  double[] dstPts, int dstOff,
							  int numPts) {
		while (--numPts >= 0) {
			float x = srcPts[srcOff++];
			float y = srcPts[srcOff++];

			dstPts[dstOff++] = m00 * x + m01 * y + m02;
			dstPts[dstOff++] = m10 * x + m11 * y + m12;
		}
	}

	public void transform(TPoint2D[] srcPts, int srcOff,
							  TPoint2D[] dstPts, int dstOff,
							  int numPts) {
		while (--numPts >= 0) {
			TPoint2D src = srcPts[srcOff++];
			TPoint2D dest = dstPts[dstOff++];

			double x = src.getX();
			double y = src.getY();

			dest = createOrReuse(dest, src);

			dest.setLocation(m00 * x + m01 * y + m02,
							  m10 * x + m11 * y + m12);
			dstPts[dstOff - 1] = dest;
		}
	}

	public TPoint2D transform(TPoint2D src, TPoint2D dest) {
		double x = src.getX();
		double y = src.getY();

		dest = createOrReuse(dest, src);

		dest.setLocation(m00 * x + m01 * y + m02,
						  m10 * x + m11 * y + m12);
		return dest;
	}

	public void inverseTransform(double[] srcPts, int srcOff,
								   double[] dstPts, int dstOff,
								   int numPts) throws TNoninvertibleTransformException {
		double det = getDeterminant();
		if (nearZero(det)) {
			throw new TNoninvertibleTransformException("Determinant is zero");
		}
		double invDet = 1.0 / det;

		while (--numPts >= 0) {
			double x = srcPts[srcOff++];
			double y = srcPts[srcOff++];

			dstPts[dstOff++] = (m11 * (x - m02) - m01 * (y - m12)) * invDet;
			dstPts[dstOff++] = (-m10 * (x - m02) + m00 * (y - m12)) * invDet;
		}
	}

	public TPoint2D inverseTransform(TPoint2D src, TPoint2D dest) throws TNoninvertibleTransformException {
		double det = getDeterminant();
		if (nearZero(det)) {
			throw new TNoninvertibleTransformException("Determinant is zero");
		}
		double invDet = 1.0 / det;

		double x = src.getX();
		double y = src.getY();

		dest = createOrReuse(dest, src);

		dest.setLocation(
			(m11 * (x - m02) - m01 * (y - m12)) * invDet,
			(-m10 * (x - m02) + m00 * (y - m12)) * invDet
		);
		return dest;
	}


	@SneakyThrows
	public Object clone() {
		return super.clone();
	}

	public static TAffineTransform getTranslateInstance(double tx, double ty) {
		TAffineTransform at = new TAffineTransform();
		at.setToTranslation(tx, ty);
		return at;
	}

	public static TAffineTransform getRotateInstance(double theta) {
		TAffineTransform at = new TAffineTransform();
		at.setToRotation(theta);
		return at;
	}

	public static TAffineTransform getRotateInstance(double theta, double anchorX, double anchorY) {
		TAffineTransform at = new TAffineTransform();
		at.setToRotation(theta, anchorX, anchorY);
		return at;
	}

	public static TAffineTransform getQuadrantRotateInstance(int numQuadrants) {
		TAffineTransform at = new TAffineTransform();
		at.setToQuadrantRotation(numQuadrants);
		return at;
	}

	public static TAffineTransform getQuadrantRotateInstance(int numQuadrants, double anchorX, double anchorY) {
		TAffineTransform at = new TAffineTransform();
		at.setToQuadrantRotation(numQuadrants, anchorX, anchorY);
		return at;
	}

	public static TAffineTransform getRotateInstance(double vecX, double vecY) {
		TAffineTransform at = new TAffineTransform();
		at.setToRotation(vecX, vecY);
		return at;
	}

	public static TAffineTransform getRotateInstance(double vecX, double vecY, double anchorX, double anchorY) {
		TAffineTransform at = new TAffineTransform();
		at.setToRotation(vecX, vecY, anchorX, anchorY);
		return at;
	}

	public static TAffineTransform getScaleInstance(double sx, double sy) {
		TAffineTransform at = new TAffineTransform();
		at.setToScale(sx, sy);
		return at;
	}

	public static TAffineTransform getShearInstance(double shx, double shy) {
		TAffineTransform at = new TAffineTransform();
		at.setToShear(shx, shy);
		return at;
	}

	private static boolean nearZero(double v) {
		return Math.abs(v) < EPS;
	}

	private static boolean nearOne(double v) {
		return Math.abs(v - 1.0) < EPS;
	}
}
