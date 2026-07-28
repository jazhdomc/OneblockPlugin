package mc.jazhdo;

import org.bukkit.Material;

public class LootItem {
    public Material material;
    public short data;
    public int min, max;

    public LootItem(Material material, short data, int min, int max) {
        this.material = material;
        this.data = data;
        this.min = min;
        this.max = max;
    }
}
