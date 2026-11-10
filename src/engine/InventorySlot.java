package engine;

/** Mutable stack entry held by an Inventory. */
public final class InventorySlot {
    private Item item;
    private int quantity;

    public boolean isEmpty() { return item == null || quantity <= 0; }
    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }

    int add(Item item, int amount) {
        if (amount <= 0 || item == null) return 0;
        if (isEmpty()) this.item = item;
        if (!this.item.getId().equals(item.getId())) return 0;
        int added = Math.min(amount, item.getStackLimit() - quantity);
        quantity += Math.max(0, added);
        return Math.max(0, added);
    }

    int remove(int amount) {
        if (amount <= 0 || isEmpty()) return 0;
        int removed = Math.min(amount, quantity);
        quantity -= removed;
        if (quantity == 0) item = null;
        return removed;
    }
}
