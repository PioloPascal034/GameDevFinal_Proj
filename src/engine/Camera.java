package engine;

import java.awt.geom.Rectangle2D;

/** Camera math shared by rendering and presentation/debug tooling. */
public final class Camera {
    private float x;
    private float y;
    private float zoom = 1f;

    public void follow(Rectangle2D.Float target, int viewportWidth, int viewportHeight,
                       int worldWidth, int worldHeight) {
        if (target == null) return;
        float visibleWidth = viewportWidth / zoom;
        float visibleHeight = viewportHeight / zoom;
        float desiredX = target.x + target.width / 2f - visibleWidth / 2f;
        float desiredY = target.y + target.height / 2f - visibleHeight / 2f;
        float maxX = Math.max(0f, worldWidth - visibleWidth);
        float maxY = Math.max(0f, worldHeight - visibleHeight);
        x = clamp(desiredX, 0f, maxX);
        y = clamp(desiredY, 0f, maxY);
    }

    public void setZoom(float zoom) { this.zoom = clamp(zoom, 1f, 1.5f); }
    public void toggleZoom() { setZoom(zoom > 1f ? 1f : 1.5f); }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZoom() { return zoom; }
    public int worldToScreenX(float worldX) { return Math.round((worldX - x) * zoom); }
    public int worldToScreenY(float worldY) { return Math.round((worldY - y) * zoom); }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
