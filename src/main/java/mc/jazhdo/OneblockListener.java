package mc.jazhdo;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OneblockListener implements Listener {
    private final OneblockPlugin plugin;
    private final FileConfiguration config;

    public OneblockListener(OneblockPlugin plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Figure out which island is 0 0 else teleport them to their island respawn point
        Player player = event.getPlayer();
        if (config.getString("islands.".concat(player.getName().toLowerCase())) == null) {
            Set<String> islands = config.getConfigurationSection("islands").getKeys(false);
            Double playerX = 0d;
            Double playerZ = 0d;
            Double islandSpacing = config.getDouble("island-spacing");
            Double islandSpacingHalf = islandSpacing/2;
            String currentIsland = null;
            for (String island : islands) {
                String base = "islands.".concat(island).concat(".");
                Double islandX = config.getDouble(base.concat("x")) * islandSpacing;
                Double islandZ = config.getDouble(base.concat("z")) * islandSpacing;
                if (playerX > islandX - islandSpacingHalf && playerZ > islandZ - islandSpacingHalf && playerX <= islandX + islandSpacingHalf && playerZ <= islandZ + islandSpacingHalf) {
                    currentIsland = base.concat("spawn.");
                    break;
                }
            }
            if (currentIsland == null) return;
            player.teleport(new Location(Bukkit.getWorld(config.getString("oneblock-world")), config.getDouble(currentIsland.concat("x")), config.getDouble(currentIsland.concat("y")), config.getDouble(currentIsland.concat("z")), (float) config.getDouble("yaw"), (float) config.getDouble("pitch")));
        } else {
            String base = "islands.".concat(player.getName().toLowerCase()).concat(".home.");
            player.teleport(new Location(Bukkit.getWorld(config.getString("oneblock-world")), config.getDouble(base.concat("x")), config.getDouble(base.concat("y")), config.getDouble(base.concat("z")), (float) config.getDouble("yaw"), (float) config.getDouble("pitch")));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Make sure player has a island
        Player player = (Player) event.getWhoClicked();
        if (config.getString("islands." + player.getName().toLowerCase() + ".x") == null) {
            plugin.getCommands().sendInfo(player, "You need a oneblock island to edit this.");
            return;
        }

        // Make sure slot is part of the GUI
        if (event.getClickedInventory() != event.getInventory()) return;

        // Find which GUI this is
        String inventoryTitle = event.getInventory().getTitle();
        if (inventoryTitle.equals(config.getString("phases-menu.title"))){
            // Cancel all clicks so no items leave the inventory
            event.setCancelled(true);

            // Make sure slot isn't a error slot
            int slot = event.getSlot();
            if (slot < 0) return;

            // Check for X slot
            if (slot == 0) player.closeInventory();

            // Make sure slot is not a border slot
            List<String> phases = config.getStringList("phases");
            int row = (int) Math.ceil((slot+1)/9);
            if (row == (int) (Math.ceil(phases.size() / 7) + 2) || row == 1 || slot % 9 == 1 || slot % 9 == 0) return;

            // Make sure slot is unlocked
            String[] locked = config.getString("phase-menu.locked").split(":");
            ItemStack lockedItem = new ItemStack(Material.getMaterial(locked[0]));
            lockedItem.setDurability(Short.parseShort(locked[1]));
            if (event.getCurrentItem() == lockedItem) return;

            // Set new phase
            config.set("islands." + player.getName().toLowerCase() + ".phase", event.getCurrentItem().getItemMeta().getDisplayName().toLowerCase());
        } else if (inventoryTitle.equals(config.getString("perm-menu.title.visitor")) || inventoryTitle.equals(config.getString("perm-menu.title.trusted"))) {
            // Make sure nothing leaves the inventory
            event.setCancelled(true);

            // Get slot
            int slot = event.getSlot();
            if (slot < 0) return;

            // Update display of enchantment and config perm
            ItemStack currentItem = event.getCurrentItem();
            boolean previouslyToggled = currentItem.getItemMeta().hasEnchant(Enchantment.DURABILITY);
            ItemMeta currentMeta = currentItem.getItemMeta();
            String configPart = "islands." + player.getName().toLowerCase() + "." + (inventoryTitle.equals(config.getString("perm-menu.title.visitor")) ? "visitor" : "trusted") + "-perms";
            List<String> permList = config.getStringList(configPart);
            if (previouslyToggled) {
                currentMeta.removeEnchant(Enchantment.DURABILITY);
                permList.remove(currentMeta.getDisplayName());
            } else {
                currentMeta.addEnchant(Enchantment.DURABILITY, slot, true);
                permList.add(currentMeta.getDisplayName());
            }

            // Set display and config
            currentItem.setItemMeta(currentMeta);
            config.set(configPart, permList);
        }

        // Save
        plugin.saveConfig();
    }

    @EventHandler
    // @SuppressWarnings("deprecation")
    public void onBlockBreak(BlockBreakEvent event) {
        // Verify that the block broken is a oneblock
        Block broken = event.getBlock();
        Location loc = broken.getLocation();
        int distance = config.getInt("island-spacing");
        if (!loc.getWorld().equals(Bukkit.getWorld(config.getString("oneblock-world"))) || loc.getBlockX() % distance != 0 || loc.getBlockY() != config.getInt("oneblock-y") || loc.getBlockZ() % distance != 0) return;

        // Update block count
        Player player = event.getPlayer();
        String base = "islands.".concat(player.getName().toLowerCase());
        int blockCount = config.getInt(base.concat(".blocks")) + 1;
        config.set(base.concat(".blocks"), blockCount);

        // Replace the broken block (use starter blocks if starter in section)
        List<String> starter = config.getStringList("starter");
        if (starter.size() > blockCount) {
            String[] parts = starter.get(blockCount).split(":");
            Material material = Material.getMaterial(parts[0]);
            if (material == null) plugin.getLogger().warning("Material name ".concat(parts[0]).concat(" is invalid."));
            broken.setType(material);
            if (parts.length > 1) broken.setData((parts.length > 1) ? Byte.parseByte(parts[1]) : 0);
        } else replaceBlock(broken, player);

        // Get total blocks needed to be out of the phase updates
        List<Integer> phaseLength = config.getIntegerList("phase-length");
        int total = 0;
        for (int i = 0; i < phaseLength.size(); i++) total += phaseLength.get(i);

        // Only update phase if phases have not yet been finished
        if (blockCount < total) {
            List<String> phases = config.getStringList("phases");
            String phase = phases.get(phases.size() - 1);

            // Find correct phase
            total = 0;
            for (int i = 0; i < phaseLength.size(); i++) {
                if (blockCount >= total && blockCount < total + phaseLength.get(i)) {
                    phase = phases.get(i);
                    break;
                }
                total += phaseLength.get(i);
            }
            if (!config.getString(base.concat(".phase")).equals(phase)) config.set(base.concat(".phase"), phase);
        }

        // Save changes
        plugin.saveConfig();
    }

    // @SuppressWarnings("deprecation")
    public void replaceBlock(Block replace, Player player) {
        // Get a random block
        List<String> blocks = config.getStringList("phase-blocks.".concat(plugin.getConfig().getString("islands.".concat(player.getName().toLowerCase()).concat(".phase"))));
        String block = blocks.get((int) (Math.random() * blocks.size()));

        // If a chest is to be made
        if (block.toLowerCase().startsWith("chest")) {
            // Replace block into a chest and get chest
            replace.setType(Material.CHEST);
            Chest chest = (Chest) replace.getState();

            // Loop through all the items and add each one (skip item 0 because that is the string "chest")
            String[] chestItems = block.split(",");
            for (int i = 1; i < chestItems.length; i++) {
                // Get the three part;s
                String[] parts = chestItems[i].split(":");

                // Get Material object from name
                Material material = Material.getMaterial(parts[0]);

                // In case material is incorrect
                if (material == null) plugin.getLogger().warning("Material name ".concat(parts[0]).concat(" is invalid."));

                // Create item
                ItemStack item = new ItemStack(material, parts.length > 2 ? Integer.parseInt(parts[2]) : 1);

                // Set item metadata
                item.setDurability(parts.length > 1 ? Short.parseShort(parts[1]) : 0);

                // Add item into chest
                chest.getBlockInventory().addItem(item);
            }
            chest.update();
        } else {
            // Get the two parts
            String[] parts = block.split(":");

            // Get Material object from name
            Material material = Material.getMaterial(parts[0]);

            // In case material is incorrect
            if (material == null) plugin.getLogger().warning("Material name ".concat(parts[0]).concat(" is invalid."));

            // Replace block
            replace.setType(material);

            // Set item metadata
            replace.setData((parts.length > 1) ? Byte.parseByte(parts[1]) : 0);
        }
    }
}