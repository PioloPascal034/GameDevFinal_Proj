package ui;

import engine.Inventory;
import engine.InventorySlot;
import engine.Item;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import main.Game;

/** Screen-space inventory panel. It is drawn after the world transform. */
public final class InventoryOverlay {
    private static final int COLUMNS = 5;
    private static final int SLOT_SIZE = 92;
    private static final int GAP = 8;

    public void draw(Graphics g, Inventory inventory) {
        int panelWidth = COLUMNS * SLOT_SIZE + (COLUMNS + 1) * GAP + 360;
        int panelHeight = 4 * SLOT_SIZE + 5 * GAP + 56;
        int panelX = (Game.GAME_WIDTH - panelWidth) / 2;
        int panelY = (Game.GAME_HEIGHT - panelHeight) / 2;

        g.setColor(new Color(10, 18, 30, 235));
        g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);
        g.setColor(new Color(120, 190, 230));
        g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString("Inventory", panelX + 18, panelY + 30);

        for (int i = 0; i < inventory.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = panelX + GAP + col * (SLOT_SIZE + GAP);
            int y = panelY + 42 + row * (SLOT_SIZE + GAP);
            InventorySlot slot = inventory.getSlot(i);
            g.setColor(i == inventory.getSelectedIndex() ? new Color(245, 205, 85) : new Color(45, 65, 85));
            g.fillRect(x, y, SLOT_SIZE, SLOT_SIZE);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, SLOT_SIZE, SLOT_SIZE);
            if (!slot.isEmpty()) drawSlot(g, slot, x, y);
        }

        InventorySlot selected = inventory.getSelectedSlot();
        int infoX = panelX + COLUMNS * (SLOT_SIZE + GAP) + 25;
        int infoY = panelY + 82;
        g.setColor(Color.WHITE);
        if (selected.isEmpty()) {
            g.drawString("Select an item", infoX, infoY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("Arrow keys select slots", infoX, infoY + 28);
        } else {
            Item item = selected.getItem();
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString(item.getName(), infoX, infoY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.drawString("x" + selected.getQuantity() + "  " + item.getCategory(), infoX, infoY + 25);
            drawWrapped(g, item.getDescription(), infoX, infoY + 55, 260);
        }
        g.setColor(new Color(190, 210, 225));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("I: close   Arrow keys: select", panelX + 18, panelY + panelHeight - 18);
    }

    private void drawSlot(Graphics g, InventorySlot slot, int x, int y) {
        BufferedImage sprite = slot.getItem().getSprite();
        if (sprite != null) g.drawImage(sprite, x + 16, y + 12, 60, 60, null);
        else {
            g.setColor(new Color(120, 220, 255));
            g.fillOval(x + 25, y + 20, 42, 42);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString("x" + slot.getQuantity(), x + 8, y + SLOT_SIZE - 8);
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int width) {
        String[] words = text.split(" ");
        String line = "";
        int lineY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > width) {
                g.drawString(line, x, lineY);
                line = word;
                lineY += 20;
            } else line = candidate;
        }
        if (!line.isEmpty()) g.drawString(line, x, lineY);
    }
}
