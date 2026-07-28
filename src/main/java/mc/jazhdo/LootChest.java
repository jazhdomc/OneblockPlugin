package mc.jazhdo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public class LootChest extends OneblockBlock {
    private final int minItems, maxItems;
    private final List<LootItem> items;

    public LootChest(int minItems, int maxItems, List<LootItem> items) {
        super(Types.LOOT);

        this.minItems = minItems;
        this.maxItems = maxItems;
        this.items = items;
    }

    private int randomBetween2Numbers(int min, int max) {
        return min + (int) (Math.random() * (max + 1 - min));
    }
    
    public List<ItemStack> getItems() {
        List<ItemStack> itemsList = new ArrayList<>();
        int randomTimes = randomBetween2Numbers(minItems, maxItems);
        for (int i = 0; i < randomTimes; i++) {
            LootItem randomItem = items.get((int) (Math.random() * items.size()));
            itemsList.add(new ItemStack(randomItem.material, randomBetween2Numbers(randomItem.min, randomItem.max), randomItem.data));
        }
        return itemsList;
    }
}
