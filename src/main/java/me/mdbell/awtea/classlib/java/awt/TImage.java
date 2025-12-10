package me.mdbell.awtea.classlib.java.awt;

import lombok.Data;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;
import me.mdbell.awtea.impl.Debug;


@Data
public abstract class TImage {

	public static final Object UndefinedProperty = new Object();

	public static final int SCALE_DEFAULT = 1;

	public static final int SCALE_FAST = 2;

	public static final int SCALE_SMOOTH = 4;

	public static final int SCALE_REPLICATE = 8;

	public static final int SCALE_AREA_AVERAGING = 16;

	private float accelerationPriority = 0.5f;

	public abstract int getWidth(TImageObserver observer);

	public abstract int getHeight(TImageObserver observer);

	public abstract TImageProducer getSource();

	public abstract TGraphics getGraphics();

	public abstract Object getProperty(String name, TImageObserver observer);

	public TImage getScaledInstance(int width, int height, int hints) {
		throw Debug.unimplemented();
	}

}
