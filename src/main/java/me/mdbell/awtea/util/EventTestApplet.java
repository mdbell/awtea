package me.mdbell.awtea.util;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

public class EventTestApplet extends Applet
	implements MouseListener, MouseMotionListener, KeyListener {

	private int clickCount = 0;
	private int motionCount = 0;

	@Override
	public void init() {
		System.out.println("init()");
//		setBackground(Color.WHITE);

		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		requestFocus(); // ensure we receive key events
	}

	@Override
	public void paint(Graphics g) {
//		System.out.println("PAINT event");
		g.setColor(Color.BLACK);
		g.drawString("Clicks: " + clickCount, 20, 20);
		g.drawString("Moves:  " + motionCount, 20, 40);
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
}
