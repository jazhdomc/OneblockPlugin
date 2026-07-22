package mc.jazhdo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class OneblockBlock {
    private final int id;
    private final byte data;
    private final List<ItemStack> items;

    /**
     * A constructor for chests
     * 
     * @param items A list of the ItemStack for the chest
     */
    public OneblockBlock(List<ItemStack> items) {
        this.id = 54;
        this.data = 0;
        this.items = items;
    }

    /**
     * A constructor for normal blocks
     * 
     * @param id The block id
     * @param data The block's data value
     */
    public OneblockBlock(int id, byte data) {
        this.id = id;
        this.data = data;
        items = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public byte getData() {
        return data;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }
}
