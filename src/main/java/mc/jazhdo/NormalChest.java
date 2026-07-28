package mc.jazhdo;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class NormalChest extends OneblockBlock {
    private List<ItemStack> items;

    public NormalChest(List<ItemStack> items) {
        super(Types.CHEST);

        this.items = items;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
