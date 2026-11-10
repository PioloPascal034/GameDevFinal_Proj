package game.items;

import engine.Item;
import utilz.LoadSave;

/** Game-owned item catalog; engine inventory code remains content-agnostic. */
public final class ItemDefinitions {
    public static final Item AURORA_SHARD = new Item("aurora_shard", "Aurora Shard",
            "A fragment of crystallized light.", Item.Category.QUEST, 10,
            LoadSave.GetSpriteAtlas(LoadSave.FRAGMENT));
    public static final Item HERB = new Item("moonleaf", "Moonleaf",
            "A resilient herb found near old platforms.", Item.Category.MATERIAL, 20,
            LoadSave.GetSpriteAtlas(LoadSave.GRASS_ATLAS));

    private ItemDefinitions() { }
}
