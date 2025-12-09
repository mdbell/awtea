package me.mdbell.awtea.util;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

public class EventTestApplet extends Applet
	implements MouseListener, MouseMotionListener, KeyListener, MouseWheelListener {

	private int clickCount = 0;
	private int motionCount = 0;

	private int lastMouseX = -1;
	private int lastMouseY = -1;

	private long frameCount = 0;
	private long startTime = System.currentTimeMillis();

	@Override
	public void init() {
		System.out.println("init()");

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);

		requestFocus(); // ensure we receive key events
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(new Color(209, 19, 19));
		g.drawString("Clicks: " + clickCount, 20, 20);
		g.drawString("Moves:  " + motionCount, 20, 40);

		if (lastMouseX >= 0 && lastMouseY >= 0) {
			g.fillRect(lastMouseX - 3, lastMouseY - 3, 6, 6);
		}
		frameCount++;
		long currentTime = System.currentTimeMillis();
		if (currentTime - startTime >= 1000) {
			long fps = frameCount * 1000 / (currentTime - startTime);
			g.drawString("FPS: " + fps, 20, 60);
		}
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
