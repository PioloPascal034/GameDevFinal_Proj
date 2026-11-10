package engine;

import java.util.Arrays;

/** Fixed-size, stack-aware inventory with no negative quantities. */
public final class Inventory {
    public static final int DEFAULT_SLOT_COUNT = 20;
    private final InventorySlot[] slots;
    private int selectedIndex;

    public Inventory() { this(DEFAULT_SLOT_COUNT); }

    public Inventory(int slotCount) {
        if (slotCount < 1) throw new IllegalArgumentException("slotCount must be positive");
        slots = new InventorySlot[slotCount];
        for (int i = 0; i < slots.length; i++) slots[i] = new InventorySlot();
    }

    public int size() { return slots.length; }
    public InventorySlot getSlot(int index) { return index < 0 || index >= slots.length ? null : slots[index]; }
    public InventorySlot[] getSlots() { return Arrays.copyOf(slots, slots.length); }
    public int getSelectedIndex() { return selectedIndex; }
    public InventorySlot getSelectedSlot() { return slots[selectedIndex]; }

    /** Returns the quantity accepted; any overflow remains unadded. */
    public int add(Item item, int quantity) {
        if (item == null || quantity <= 0) return 0;
        int remaining = quantity;
        for (InventorySlot slot : slots)
            if (!slot.isEmpty() && slot.getItem().getId().equals(item.getId()))
                remaining -= slot.add(item, remaining);
        for (InventorySlot slot : slots) {
            if (remaining <= 0) break;
            if (slot.isEmpty()) remaining -= slot.add(item, remaining);
        }
        return quantity - remaining;
    }

    public int remove(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) return 0;
        int remaining = quantity;
        for (InventorySlot slot : slots)
            if (!slot.isEmpty() && itemId.equals(slot.getItem().getId()))
                remaining -= slot.remove(remaining);
        return quantity - remaining;
    }

    public int count(String itemId) {
        int total = 0;
        for (InventorySlot slot : slots)
            if (!slot.isEmpty() && itemId != null && itemId.equals(slot.getItem().getId())) total += slot.getQuantity();
        return total;
    }

    public boolean contains(String itemId) { return count(itemId) > 0; }
    public void select(int index) { if (index >= 0 && index < slots.length) selectedIndex = index; }
    public void moveSelection(int delta) { select(Math.max(0, Math.min(slots.length - 1, selectedIndex + delta))); }
}
