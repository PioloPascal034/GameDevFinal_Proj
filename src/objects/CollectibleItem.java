package objects;

import engine.Item;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import main.Game;

/** A simple world pickup that can be collected by proximity/intersection. */
public final class CollectibleItem extends GameObject {
    private final Item item;
    private int quantity;
    private final BufferedImage sprite;

    public CollectibleItem(int x, int y, Item item, int quantity) {
        super(x, y, -1);
        if (item == null || quantity < 1) throw new IllegalArgumentException("Invalid collectible");
        this.item = item;
        this.quantity = quantity;
        this.sprite = item.getSprite();
        initHitbox(24, 24);
    }

    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }
    public void consume(int amount) { quantity = Math.max(0, quantity - Math.max(0, amount)); }

    public void draw(Graphics g, int xLvlOffset) {
        if (!active) return;
        int x = (int) hitbox.x - xLvlOffset;
        int y = (int) hitbox.y;
        if (sprite != null) g.drawImage(sprite, x, y, Game.TILES_SIZE, Game.TILES_SIZE, null);
        else {
            g.setColor(new Color(120, 220, 255));
            g.fillOval(x + 8, y + 8, Game.TILES_SIZE - 16, Game.TILES_SIZE - 16);
        }
    }
}
