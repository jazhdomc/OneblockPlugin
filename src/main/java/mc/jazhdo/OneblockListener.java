package mc.jazhdo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class OneblockListener implements Listener {
    private final OneblockPlugin plugin;
    private final FileConfiguration config;
    private final Logger log;
    private Map<String, List<OneblockBlock>> phaseBlocks = null;

    public OneblockListener(OneblockPlugin plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        log = plugin.getLogger();
        generatePhaseBlocks();
    }

    public void generatePhaseBlocks() {
        // Set to null for any block mines before done
        this.phaseBlocks = null;

        // Make sure the configuration section of phase-blocks exists
        if (!config.contains("phase-blocks")) {
            log.log(Level.SEVERE, "Configuration section \"phase-blocks\" not found. Oneblock block replacement will not work.");
            return;
        }

        // Go through the phases and build each phase's block list
        Map<String, List<OneblockBlock>> tempPhaseBlocks = new HashMap<>();
        ConfigurationSection phaseBlocksConfig = config.getConfigurationSection("phase-blocks");
        for (String phase : phaseBlocksConfig.getKeys(false)) {
            // Get and verify phase's blocks
            List<String> blocks = phaseBlocksConfig.getStringList(phase);
            if (blocks.isEmpty()) log.log(Level.WARNING, "Error getting phase \"{0}\"'s blocks.", phase);

            // Build list of blocks
            List<OneblockBlock> blockList = new ArrayList<>();
            for (String block : blocks) {
                // If a chest is to be made
                if (block.toUpperCase().startsWith("CHEST")) {
                    // Replace block into a chest and get chest
                    List<ItemStack> chestItemsList = new ArrayList<>();

                    // Remove the first item because that's "CHEST"
                    String[] originalItems = block.split(",");
                    String[] chestItems = new String[originalItems.length - 1];
                    System.arraycopy(originalItems, 1, chestItems, 0, chestItems.length);

                    // Go through each item's string and create and add it
                    for (String chestItem : chestItems) {
                        // Get the three parts
                        String[] parts = chestItem.split(":");

                        // Get Material object from name
                        Material material = Material.getMaterial(parts[0]);

                        // In case material is incorrect
                        if (material == null) {
                            log.log(Level.WARNING, "Phase {0} chest material name {1} is invalid. Defaulting to grass.", new String[]{phase, parts[0]});
                            material = Material.GRASS;
                        }

                        // Create item
                        ItemStack item = new ItemStack(material, parts.length > 2 ? Integer.parseInt(parts[2]) : 1);

                        // Set item metadata
                        item.setDurability(parts.length > 1 ? Short.parseShort(parts[1]) : 0);

                        // Add item into chest
                        chestItemsList.add(item);
                    }

                    // Add item
                    blockList.add(new OneblockBlock(chestItemsList));
                } else {
                    // Get the two parts
                    String[] parts = block.split(":");

                    // Get Material object from name
                    Material material = Material.getMaterial(parts[0]);

                    // In case material is incorrect
                    if (material == null) {
                        log.log(Level.WARNING, "Phase {0} block material name {1} is invalid. Defaulting to grass.", new String[]{phase, parts[0]});
                        material = Material.GRASS;
                    }

                    // Add block
                    blockList.add(new OneblockBlock(material.getId(), (parts.length > 1) ? Byte.parseByte(parts[1]) : 0));
                }
            }

            // Add phase and its coresponding blocks to the big list
            tempPhaseBlocks.put(phase, blockList);
        }

        // Set all the changes to the real blocks list
        this.phaseBlocks = tempPhaseBlocks;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Figure out which island is 0 0 else teleport them to their island respawn point
        Player player = event.getPlayer();
        if (!config.contains("islands." + player.getName().toLowerCase())) {
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
            event.setRespawnLocation(new Location(Bukkit.getWorld(config.getString("oneblock-world")), config.getDouble(currentIsland.concat("x")), config.getDouble(currentIsland.concat("y")), config.getDouble(currentIsland.concat("z")), (float) config.getDouble("yaw"), (float) config.getDouble("pitch")));
        } else {
            String base = "islands.".concat(player.getName().toLowerCase()).concat(".home.");
            event.setRespawnLocation(new Location(Bukkit.getWorld(config.getString("oneblock-world")), config.getDouble(base.concat("x")), config.getDouble(base.concat("y")), config.getDouble(base.concat("z")), (float) config.getDouble("yaw"), (float) config.getDouble("pitch")));
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
        // if (inventoryTitle.equals(config.getString("phase-menu.title"))){
        //     // Cancel all clicks so no items leave the inventory
        //     event.setCancelled(true);

        //     // Make sure slot isn't a error slot
        //     int slot = event.getSlot();
        //     if (slot < 0) return;

        //     // Check for X slot
        //     if (slot == 0) player.closeInventory();

        //     // Make sure slot is not a border slot
        //     List<String> phases = config.getStringList("phases");
        //     int row = (int) Math.ceil((slot+1)/9);
        //     if (row == (int) (Math.ceil(phases.size() / 7) + 2) || row == 1 || slot % 9 == 1 || slot % 9 == 0) return;

        //     ItemStack currentItemStack = event.getCurrentItem();
        //     if (currentItemStack.hasItemMeta() && currentItemStack.getItemMeta().hasDisplayName()) {
        //         // Make sure slot is unlocked
        //         if (currentItemStack.getItemMeta().getDisplayName().equals("Locked phase")) return;

        //         // Set new phase
        //         config.set("islands." + player.getName().toLowerCase() + ".phase", currentItemStack.getItemMeta().getDisplayName().toLowerCase());
        //     }
        // } else 
        if (inventoryTitle.equals(config.getString("perm-menu.title.visitor")) || inventoryTitle.equals(config.getString("perm-menu.title.trusted"))) {
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
        if (loc.getWorld().getName().equals(config.getString("oneblock-world")) && loc.getBlockX() % distance == 0 && loc.getBlockZ() % distance == 0 && loc.getBlockY() == config.getInt("oneblock-y")) {
            // Cancel the block early to prevent too much lag
            event.setCancelled(true);

            // Update block count
            // TODO: Make this based on the island and not the player (make a new helper public func from the existing func)
            Player player = event.getPlayer();
            String base = "islands.".concat(player.getName().toLowerCase());
            int blockCount = config.getInt(base.concat(".blocks")) + 1;
            config.set(base.concat(".blocks"), blockCount);

            // Drop normal drops
            World brokenWorld = broken.getWorld();
            Location dropLocation = loc.clone().add(0.5, 1.2, 0.5);
            ItemStack tool = player.getInventory().getItemInMainHand();
            for (ItemStack drop : broken.getDrops(tool)) brokenWorld.dropItemNaturally(dropLocation, drop);
            int xp = event.getExpToDrop();
            if (xp > 0) brokenWorld.spawn(dropLocation, ExperienceOrb.class).setExperience(xp);

            // Simulate tool use
            ItemMeta meta = tool.getItemMeta();
            if (tool.getType().getMaxDurability() > 0 && meta != null && !meta.isUnbreakable()) {
                short newDurability = (short) ((short) tool.getDurability() + 1);
                if (newDurability >= tool.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.getWorld().playSound(dropLocation, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                } else tool.setDurability(newDurability);
            } 

            // Statistics
            try {
                player.incrementStatistic(Statistic.MINE_BLOCK, broken.getType(), 1);
            } catch (IllegalArgumentException e) {}

            // Exhaustion from mining
            player.setExhaustion(player.getExhaustion() + 0.005f);

            // Replace the broken block (use starter blocks if starter in section)
            List<String> starter = config.getStringList("starter");
            if (starter.size() > blockCount) {
                String[] parts = starter.get(blockCount).split(":");
                Material material = Material.getMaterial(parts[0]);
                if (material == null) {
                    log.log(Level.WARNING, "Starter block #{0} material name {1} is invalid. Defaulting to grass.", new String[]{Integer.toString(blockCount), parts[0]});
                    material = Material.GRASS;
                }
                broken.setType(material);
                broken.setData(Byte.parseByte((parts.length > 1) ? parts[1] : "0"));
            } else replaceBlock(broken, player);

            // Get total blocks needed to be out of the phase updates
            List<Integer> phaseLength = config.getIntegerList("phase-length");
            int total = 0;
            for (int i = 0; i < phaseLength.size(); i++) total += phaseLength.get(i);

            // Only update phase if phases have not yet been finished
            if (blockCount < total) {
                List<String> phases = new ArrayList<>(config.getConfigurationSection("phase-blocks").getKeys(false));
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
                if (!phase.equals(config.getString(base.concat(".phase")))) config.set(base.concat(".phase"), phase);
            }
        }
    }

    // @SuppressWarnings("deprecation")
    public void replaceBlock(Block replace, Player player) {
        // Get player location
        Location originalLoc = player.getLocation();
        int oneblockY = config.getInt("oneblock-y");
        if (originalLoc.getBlockY() == oneblockY) originalLoc.setY(oneblockY + 1);

        // Get phase
        // TODO: Make this based on the island and not the player (make a new helper public func from the existing func)
        String playerPhase = config.getString("islands." + player.getName().toLowerCase() + ".phase");
        if (playerPhase == null) {
            plugin.getCommands().sendInfo(player, ChatColor.RED + "Error fetching your phase / island. Make sure you have a island and contact staff for help. Defaulting to plains phase.");
            playerPhase = "plains";
        }

        // Replace block
        List<OneblockBlock> phaseList = phaseBlocks.get(playerPhase);
        OneblockBlock randomizedBlock = phaseList.get((int) (Math.random() * phaseList.size()));
        if (randomizedBlock.hasItems()) {
            replace.setType(Material.CHEST);
            Chest chest = (Chest) replace.getState();
            Inventory chestInventory = chest.getBlockInventory();
            for (ItemStack item : randomizedBlock.getItems()) chestInventory.addItem(item);
            chest.update(true, false);
        } else replace.setTypeIdAndData(randomizedBlock.getId(), randomizedBlock.getData(), true);
        
        // Teleport player to the right location
        player.teleport(originalLoc);
    }
}