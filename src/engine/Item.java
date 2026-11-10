package engine;

import java.awt.image.BufferedImage;

/** Immutable item definition shared by world collectibles and inventory UI. */
public final class Item {
    public enum Category { MATERIAL, CONSUMABLE, QUEST, KEY }

    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final int stackLimit;
    private final BufferedImage sprite;

    public Item(String id, String name, String description, Category category,
                int stackLimit, BufferedImage sprite) {
        if (id == null || id.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("Item id and name are required");
        if (stackLimit < 1) throw new IllegalArgumentException("stackLimit must be positive");
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
        this.category = category == null ? Category.MATERIAL : category;
        this.stackLimit = stackLimit;
        this.sprite = sprite;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public int getStackLimit() { return stackLimit; }
    public BufferedImage getSprite() { return sprite; }
}
