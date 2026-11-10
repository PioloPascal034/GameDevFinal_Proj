package engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/** Screen-space developer overlay; it never receives world/camera transforms. */
public final class DebugOverlay {
    private boolean visible;
    private boolean hitboxes;

    public void toggle() { visible = !visible; }
    public boolean isVisible() { return visible; }
    public boolean isHitboxesVisible() { return hitboxes; }
    public void toggleHitboxes() { hitboxes = !hitboxes; }

    public void draw(Graphics g, int fps, Rectangle2D.Float player, float velocityY,
                      boolean grounded, float cameraX, float cameraY, float zoom) {
        if (!visible) return;
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(8, 8, 300, 112);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.drawString("Horizon Engine v0.1 Aurora", 16, 25);
        g.drawString("FPS: " + fps + "  | Player: " + fmt(player.x) + ", " + fmt(player.y), 16, 43);
        g.drawString("Velocity Y: " + fmt(velocityY) + "  Grounded: " + grounded, 16, 61);
        g.drawString("Camera: " + fmt(cameraX) + ", " + fmt(cameraY) + "  Zoom: " + fmt(zoom) + "x", 16, 79);
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        g.drawString("Memory: " + usedMb + " MB  |  F3 overlay", 16, 97);
    }

    private static String fmt(float value) { return String.format("%.1f", value); }
}
