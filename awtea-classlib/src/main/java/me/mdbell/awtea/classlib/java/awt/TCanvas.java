package me.mdbell.awtea.classlib.java.awt;

/**
 * TCanvas: Lightweight Canvas Component for Custom Drawing
 *
 * <p>A lightweight canvas component designed for custom drawing within AWT containers.
 * This component does not manage its own DOM element or rendering surface; instead,
 * it renders into its parent container's surface as part of the normal AWT paint
 * event flow.</p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Lightweight - no DOM element, renders to parent's surface</li>
 *   <li>Participates in AWT component hierarchy and layout management</li>
 *   <li>Receives events through standard AWT event dispatch</li>
 *   <li>Suitable for custom drawing components within an application</li>
 *   <li>Efficient memory footprint - shares parent's rendering resources</li>
 * </ul>
 *
 * <p><b>When to Use TCanvas:</b></p>
 * <ul>
 *   <li>Custom drawing areas within panels, frames, or other containers</li>
 *   <li>Game elements or visualizations embedded in a larger UI</li>
 *   <li>Components that need AWT layout management (GridLayout, BorderLayout, etc.)</li>
 *   <li>Scenarios where you want minimal resource overhead</li>
 *   <li>When you need standard AWT component behavior (focus, events, bounds, etc.)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Create a lightweight canvas with custom drawing
 * TCanvas canvas = new TCanvas() {
 *     @Override
 *     public void paint(TGraphics g) {
 *         // Clear background
 *         g.setColor(Color.WHITE);
 *         g.fillRect(0, 0, getWidth(), getHeight());
 *         
 *         // Draw custom content
 *         g.setColor(Color.BLUE);
 *         g.fillOval(10, 10, 80, 80);
 *         
 *         g.setColor(Color.BLACK);
 *         g.drawString("Custom Drawing", 20, 50);
 *     }
 * };
 * 
 * // Add to a container
 * frame.add(canvas);
 * canvas.setBounds(10, 10, 200, 200);
 * 
 * // Trigger repainting when needed
 * canvas.repaint();
 * }</pre>
 *
 * <p><b>Event Handling:</b></p>
 * <pre>{@code
 * canvas.addMouseListener(new TMouseAdapter() {
 *     @Override
 *     public void mouseClicked(TMouseEvent e) {
 *         System.out.println("Canvas clicked at: " + e.getX() + ", " + e.getY());
 *         canvas.repaint();
 *     }
 * });
 * }</pre>
 *
 * <p><b>Comparison with THeavyCanvas:</b></p>
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>TCanvas (Lightweight)</th>
 *     <th>THeavyCanvas (Heavyweight)</th>
 *   </tr>
 *   <tr>
 *     <td>DOM Element</td>
 *     <td>❌ None</td>
 *     <td>✅ HTMLCanvasElement</td>
 *   </tr>
 *   <tr>
 *     <td>Rendering Surface</td>
 *     <td>Uses parent's surface</td>
 *     <td>Owns dedicated Surface</td>
 *   </tr>
 *   <tr>
 *     <td>Event Handling</td>
 *     <td>Through parent hierarchy</td>
 *     <td>Direct browser events</td>
 *   </tr>
 *   <tr>
 *     <td>Use Case</td>
 *     <td>Embedded components</td>
 *     <td>Top-level windows, high-performance</td>
 *   </tr>
 *   <tr>
 *     <td>Memory Overhead</td>
 *     <td>Low</td>
 *     <td>Higher (dedicated resources)</td>
 *   </tr>
 * </table>
 *
 * <p>For top-level windows, high-performance rendering, or direct hardware acceleration
 * needs, consider using {@link THeavyCanvas} instead.</p>
 *
 * @see THeavyCanvas The heavyweight canvas component with its own DOM element and surface
 * @see TComponent Base class for all AWT components
 * @see TGraphics Graphics context for drawing operations
 */
public class TCanvas extends TComponent {

    @Override
    public void paint(TGraphics g) {
        // Override this method to provide custom drawing
        // Default implementation does nothing
    }
}
