package me.mdbell.awtea.util;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class EventTestApplet extends Applet
	implements MouseListener, MouseMotionListener, KeyListener, MouseWheelListener {

	private int clickCount = 0;
	private int motionCount = 0;

	private int lastMouseX = -1;
	private int lastMouseY = -1;

	private long frameCount = 0;
	private long startTime = System.currentTimeMillis();

	public static final BufferedImage A_RED = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
	public static final BufferedImage A_GREEN = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
	public static final BufferedImage A_BLUE = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);

	public static final BufferedImage RED = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
	public static final BufferedImage GREEN = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
	public static final BufferedImage BLUE = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);

	public static final BufferedImage RED_BLUE_CHECKERED = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);

	static {
		fillImage(A_RED, new Color(255, 0, 0, 255));
		fillImage(A_GREEN, new Color(0, 255, 0, 255));
		fillImage(A_BLUE, new Color(0, 0, 255, 255));

		fillImage(RED, new Color(255, 0, 0));
		fillImage(GREEN, new Color(0, 255, 0));
		fillImage(BLUE, new Color(0, 0, 255));

		fillCheckeredImage(RED_BLUE_CHECKERED, new Color(255, 0, 0), new Color(0, 0, 255));
	}

	@Override
	public void init() {
		System.out.println("init()");

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);


		requestFocus(); // ensure we receive key events
	}

	private static void fillImage(BufferedImage img, Color color) {
		Graphics g = img.getGraphics();
		g.setColor(color);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.dispose();
	}

	private static void fillCheckeredImage(BufferedImage img, Color color1, Color color2) {
		Graphics g = img.getGraphics();
		for (int y = 0; y < img.getHeight(); y += 10) {
			for (int x = 0; x < img.getWidth(); x += 10) {
				if (((x / 10) + (y / 10)) % 2 == 0) {
					g.setColor(color1);
				} else {
					g.setColor(color2);
				}
				g.fillRect(x, y, 10, 10);
			}
		}
		g.dispose();
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.YELLOW);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.drawImage(A_RED, 10, 10, null);
		g.drawImage(A_GREEN, 40, 10, null);
		g.drawImage(A_BLUE, 70, 10, null);

		g.drawImage(RED, 10, 40, null);
		g.drawImage(GREEN, 40, 40, null);
		g.drawImage(BLUE, 70, 40, null);

		g.drawImage(RED_BLUE_CHECKERED, 10, 70, null);

		g.setColor(Color.BLACK);

		int mx = lastMouseX >= 0 ? lastMouseX : 0;
		int my = lastMouseY >= 0 ? lastMouseY : 0;

		g.drawLine(mx - 10, my - 10, mx + 10, my + 10);
		g.drawLine(mx - 10, my + 10, mx + 10, my - 10);

//
//		g.setColor(Color.BLACK);
//		g.drawString("Click count: " + clickCount, 10, 120);
//		g.drawString("Motion count: " + motionCount, 10, 140);
//		g.drawString("Last mouse: " + lastMouseX + "," + lastMouseY, 10, 160);
//
//		frameCount++;
//		long elapsed = System.currentTimeMillis() - startTime;
//		double fps = (frameCount * 1000.0) / elapsed;
//		g.drawString(String.format("Frames: %d  Time: %d ms  FPS: %.2f", frameCount, elapsed, fps), 10, 180);
	}

	// --------------------------------------------------------
	// Mouse events
	// --------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		System.out.println("mousePressed: " + e);
		clickCount++;
		repaint();
	}

	public void mouseReleased(MouseEvent e) {
		System.out.println("mouseReleased: " + e);
	}

	public void mouseClicked(MouseEvent e) {
		System.out.println("mouseClicked: " + e);
	}

	public void mouseEntered(MouseEvent e) {
		System.out.println("mouseEntered: " + e);
	}

	public void mouseExited(MouseEvent e) {
		System.out.println("mouseExited: " + e);
	}

	// --------------------------------------------------------
	// Mouse motion
	// --------------------------------------------------------
	public void mouseMoved(MouseEvent e) {
		motionCount++;
		lastMouseX = e.getX();
		lastMouseY = e.getY();
		System.out.println("mouseMoved: " + e.getX() + "," + e.getY());
		repaint(); // flood test: move around and watch coalescing
	}

	public void mouseDragged(MouseEvent e) {
		System.out.println("mouseDragged: " + e);
	}

	// --------------------------------------------------------
	// Key events
	// --------------------------------------------------------
	public void keyPressed(KeyEvent e) {
		System.out.println("keyPressed: " + e.getKeyChar());
		repaint();
	}

	public void keyReleased(KeyEvent e) {
		System.out.println("keyReleased: " + e.getKeyChar());
	}

	public void keyTyped(KeyEvent e) {
		System.out.println("keyTyped: " + e.getKeyChar());
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		System.out.println("mouseWheelMoved: " + e.getWheelRotation());
		repaint();
	}
}
