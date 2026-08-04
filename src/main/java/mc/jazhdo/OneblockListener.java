package mc.jazhdo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<NormalBlock> starterBlocks = new ArrayList<>();
    public World oneblockWorld;
    public int oneblockY;
    public List<String> phases = new ArrayList<>();

    public OneblockListener(OneblockPlugin plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        log = plugin.getLogger();
        generatePhaseBlocks();
    }

    /**
     * Converts a list into a readable string format
     * 
     * @param list The list to convert
     * @return The readable string
     */
    private String list2string(List<String> list) {
        String output = "[";
        for (int i = 0; i < list.size(); i++) {
            output += list.get(i);
            if (list.size() - 1 != i) output += ", ";
        }
        return output + "]";
    }

    /**
     * A wrapper for list2string(List<String> list) but with a ordinary string list instead of a List<String>
     * 
     * @param list The list to convert
     * @return The readable string 
     */
    private String list2string(String[] list) {
        return list2string(List.of(list));
    }

    /**
     * A function to parse and load all the phases' blocks before any player mines them to save time and requests
    */
    public void generatePhaseBlocks() {
        // Preload all the variables after each config reload
        phases = new ArrayList<>(config.getConfigurationSection("phase-blocks").getKeys(false));
        oneblockWorld = Bukkit.getWorld(config.getString("oneblock-world"));
        oneblockY = config.getInt("oneblock-y");
        
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
                // Find what type of block is to be made
                if (block.toUpperCase().startsWith("CHEST")) {
                    // Create list
                    List<ItemStack> chestItemsList = new ArrayList<>();

                    // Remove the first item because that's "CHEST"
                    String[] originalItems = block.split(","), chestItems = new String[originalItems.length - 1];
                    System.arraycopy(originalItems, 1, chestItems, 0, chestItems.length);

                    // Go through each item's string and create and add it
                    for (String chestItem : chestItems) {
                        // Get the three parts
                        String[] parts = chestItem.split(":");

                        // Get Material object from name
                        Material material = Material.getMaterial(parts[0]);

                        // Validate material name and status as a item
                        if (material == null) {
                            log.log(Level.WARNING, "Phase {0} chest Material.{1} is invalid. Defaulting to grass.", new String[]{phase, parts[0]});
                            material = Material.GRASS;
                        }
                        if (!material.isItem()) {
                            log.log(Level.WARNING, "Phase {0} chest Material.{1} is not a item. Defaulting to grass.", new String[]{phase, parts[0]});
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
                    blockList.add(new NormalChest(chestItemsList));
                } else if (block.toUpperCase().startsWith("LOOT")) {
                    String[] parts = block.split(","), itemsRangeString = parts[0].split(":")[1].split("-");
                    int itemsRangeMin, itemsRangeMax;
                    if (itemsRangeString.length < 2) {
                        log.log(Level.WARNING, "Phase {0} loot chest min/max items list ({1}) is too short. Defaulting to 2 to 4 items.", new String[]{phase, list2string(itemsRangeString)});
                        itemsRangeMin = 2;
                        itemsRangeMax = 4;
                    } else {
                        try {
                            itemsRangeMin = Integer.parseInt(itemsRangeString[0]);
                            itemsRangeMax = Integer.parseInt(itemsRangeString[1]);
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Phase {0} loot chest items range string list ({1}) has a invalid number (Error: {2}). Defaulting to 2 to 4 items.", new String[]{phase, list2string(itemsRangeString), e.getMessage()});
                            itemsRangeMin = 2;
                            itemsRangeMax = 4;
                        }
                    }
                    List<LootItem> items = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        // Get and validate parts
                        String[] subParts = parts[i].split(":");
                        if (subParts.length < 4) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} with data {2} is invalid for not having all four required parts: MATERIAL:DATA:RANGE:WEIGHT. Skipping item", new String[]{phase, Integer.toString(i), list2string(subParts)});
                            continue;
                        }

                        // Get and validate material
                        Material material = Material.getMaterial(subParts[0]);
                        if (material == null) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2} is invalid. Defaulting to grass.", new String[]{phase, Integer.toString(i), subParts[0]});
                            material = Material.GRASS;
                        }
                        if (!material.isItem()) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2} is not a item. Defaulting to grass.", new String[]{phase, Integer.toString(i), subParts[0]});
                            material = Material.GRASS;
                        }

                        // Get and validate range
                        String[] rangeString = subParts[2].split("-");
                        int rangeMin, rangeMax;
                        if (rangeString.length < 2) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2} range list is invalid ({3}). Defaulting to 2 to 4 items.", new String[]{phase, Integer.toString(i), subParts[0], list2string(rangeString)});
                            rangeMin = 2;
                            rangeMax = 4;
                        } else {
                            try {
                                rangeMin = Integer.parseInt(rangeString[0]);
                                rangeMax = Integer.parseInt(rangeString[1]);
                            } catch (NumberFormatException e) {
                                log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2} range string min {3} or max {4} is not a valid number (Error: {5}). Defaulting to 2 to 4 items.", new String[]{phase, Integer.toString(i), subParts[0], rangeString[0], rangeString[1], e.getMessage()});
                                rangeMin = 2;
                                rangeMax = 4;
                            }
                        }

                        // Get and validate data
                        short data;
                        try {
                            data = Short.parseShort(subParts[1]);
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2}'s data of \"{3}\" is not a number (Error: {4}). Defaulting to a data value of 0.", new String[]{phase, Integer.toString(i), subParts[0], subParts[1], e.getMessage()});
                            data = 0;
                        }

                        // Get and validate weight
                        int weight;
                        try {
                            weight = Integer.parseInt(subParts[3]);
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Phase {0} loot chest item {1} Material.{2}'s weight of \"{3}\" is not a number (Error: {4}). Defaulting to a weight value of 1.", new String[]{phase, Integer.toString(i), subParts[0], subParts[3], e.getMessage()});
                            weight = 1;
                        }

                        // Add the loot item
                        for (int j = 0; j < weight; j++) items.add(new LootItem(material, data, rangeMin, rangeMax));
                    }

                    // Add chest and its data
                    blockList.add(new LootChest(itemsRangeMin, itemsRangeMax, items));
                } else {
                    // Get the two parts
                    String[] parts = block.split(":");

                    // Get Material object from name
                    Material material = Material.getMaterial(parts[0]);

                    // Validate material name and status as a block
                    if (material == null) {
                        log.log(Level.WARNING, "Phase {0} block Material.{1} is invalid. Defaulting to grass.", new String[]{phase, parts[0]});
                        material = Material.GRASS;
                    }
                    if (!material.isBlock()) {
                        log.log(Level.WARNING, "Phase {0} block Material.{1} is not a block. Defaulting to grass.", new String[]{phase, parts[0]});
                        material = Material.GRASS;
                    }

                    // Add block
                    blockList.add(new NormalBlock(material.getId(), (parts.length > 1) ? Byte.parseByte(parts[1]) : 0));
                }
            }

            // Add phase and its coresponding blocks to the big list
            tempPhaseBlocks.put(phase, blockList);
        }

        // Set all the changes to the real blocks list
        this.phaseBlocks = tempPhaseBlocks;

        // Empty list
        starterBlocks = new ArrayList<>();

        // Parse blocks and add them to the starter list
        List<String> starter = config.getStringList("starter");
        for (String block : starter) {
            // Get the two parts of the block
            String[] parts = block.split(":");

            // Get and verify material
            Material material = Material.getMaterial(parts[0].toUpperCase());
            if (material == null) {
                log.log(Level.WARNING, "Starter block material Material.{0} does not exist. Defaulting to grass. ({1})", new String[]{parts[0], block});
                material = Material.GRASS;
            }

            // Get and verify data byte
            short data;
            if (parts.length < 2) data = 0;
            else {
                try {
                    data = Short.parseShort(parts[1]);
                } catch (NumberFormatException e) {
                    log.log(Level.WARNING, "Starter block {0}'s data value of {1} is not a valid number. Defaulting to 0.", new String[]{block, parts[1]});
                    data = 0;
                }
            }

            // Add block
            starterBlocks.add(new NormalBlock(material.getId(), (byte) data));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String playerNameLowercase = player.getName().toLowerCase(), base = "islands." + playerNameLowercase + ".home.";
        if (!config.contains("islands." + playerNameLowercase)) {
            base = getIslandFromLocation(player.getLocation());
            if (base == null) base = getIslandFromXAndZ(0.0, 0.0);
            if (base == null) return;
            base += "spawn.";
        }
        event.setRespawnLocation(new Location(oneblockWorld, config.getDouble(base + "x"), config.getDouble(base + "y"), config.getDouble(base + "z"), (float) config.getDouble(base + "yaw"), (float) config.getDouble(base + "pitch")));
    }

    /**
     * A wrapper function for getIslandFromXAndZ() that takes in a location parameter instead of two Double parameters
     * 
     * @param loc The location to check for
     * @return The base string for the island at that location or null if no island exists there
     */
    public String getIslandFromLocation(Location loc) {
        return getIslandFromXAndZ(loc.getX(), loc.getY());
    }

    /**
     * Gets a island from a x and a z value
     * 
     * @param x The x value
     * @param z The z value
     * @return The base string for the island at that x and z value, or null if no island exists there
     */
    public String getIslandFromXAndZ(Double x, Double z) {
        int islandSpacing = config.getInt("island-spacing"), islandSpacingHalf = islandSpacing/2;
        for (String island : config.getConfigurationSection("islands").getKeys(false)) {
            String base = "islands." + island + ".";
            int islandX = config.getInt(base + "x") * islandSpacing, islandZ = config.getInt(base + "z") * islandSpacing;
            if (x > islandX - islandSpacingHalf && z > islandZ - islandSpacingHalf && x <= islandX + islandSpacingHalf && z <= islandZ + islandSpacingHalf) return base;
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Make sure player has a island
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getCommands().hasIsland(player)) return;

        // Make sure slot is part of the GUI
        if (event.getClickedInventory() != event.getInventory()) return;

        // Find which GUI this is
        String inventoryTitle = event.getInventory().getTitle();
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
        if (loc.getWorld().getName().equals(config.getString("oneblock-world")) && loc.getBlockX() % distance == 0 && loc.getBlockZ() % distance == 0 && loc.getBlockY() == oneblockY) {
            // Cancel the block early to prevent too much lag
            event.setCancelled(true);

            // Get player location as a fix for being squished out a block
            Player player = event.getPlayer();
            Location originalLoc = player.getLocation();
            double islandSpacing = config.getDouble("island-spacing"), originalLocX = originalLoc.getX(), originalLocZ = originalLoc.getZ();
            if (((originalLocX >= 0 && originalLocX % islandSpacing < 1.3) || (originalLocX < 0 && originalLocX % islandSpacing > -0.3))
            && ((originalLocZ >= 0 && originalLocZ % islandSpacing < 1.3) || (originalLocZ < 0 && originalLocZ % islandSpacing > -0.3))
            && originalLoc.getBlockY() == oneblockY) originalLoc.setY(oneblockY + 1);

            // Get the island the player is on
            String base = getIslandFromLocation(originalLoc);
            if (base == null) return;

            // Update block count
            int blockCount = config.getInt(base + ".blocks") + 1;
            config.set(base + ".blocks", blockCount);

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
            OneblockCommands commands = plugin.getCommands();
            if (starterBlocks.size() > blockCount) {
                if (starterBlocks == null) broken.setTypeIdAndData(Material.GRASS.getId(), (byte) 0, true);
                else {
                    NormalBlock block = starterBlocks.get(blockCount);
                    broken.setTypeIdAndData(block.getId(), block.getData(), true);
                }
            } else replaceBlock(commands, base, broken, player);

            // Get total blocks needed to be out of the phase updates
            List<Integer> phaseLength = config.getIntegerList("phase-length");
            int total = 0;
            for (int i = 0; i < phaseLength.size(); i++) total += phaseLength.get(i);

            // Only update phase if phases have not yet been finished
            if (blockCount < total) {
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

                // Update phase if changed
                if (!phase.equals(config.getString(base + ".phase"))) {
                    config.set(base + ".phase", phase);
                    phase = capitalize(phase);
                    commands.sendInfo(player, ChatColor.GREEN + "New Phase: " + phase + ". Use the command \"/ob count\" to check block count.");
                    player.sendTitle(ChatColor.GOLD + "New Phase " + phase, ChatColor.YELLOW + "Use \"/ob count\" for more info.", 10, 100, 20);
                }
            }
        
            // Teleport player to the right location 
            Location newLocation = player.getLocation();
            originalLoc.setYaw(newLocation.getYaw());
            originalLoc.setPitch(newLocation.getPitch());
            player.teleport(originalLoc);
        }
    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    // @SuppressWarnings("deprecation")
    public void replaceBlock(OneblockCommands commands, String base, Block replace, Player player) {
        // Get phase
        String islandPhase = config.getString(base + ".phase");
        if (islandPhase == null) {
            commands.sendInfo(player, ChatColor.RED + "Error fetching the phase of the island you are on. Make sure you are on a island and contact staff for help. Defaulting to plains phase.");
            islandPhase = "plains";
        }

        // Replace block
        List<OneblockBlock> phaseList = phaseBlocks.get(islandPhase);
        OneblockBlock randomizedBlock = phaseList.get((int) (Math.random() * phaseList.size()));
        switch (randomizedBlock.getType()) {
            case CHEST -> {
                NormalChest chestBlock = (NormalChest) randomizedBlock;
                replace.setType(Material.CHEST);
                Chest chest = (Chest) replace.getState();
                Inventory chestInventory = chest.getBlockInventory();
                chestInventory.clear();
                for (ItemStack item : chestBlock.getItems()) chestInventory.addItem(item);
            }
            case LOOT -> {
                LootChest chestBlock = (LootChest) randomizedBlock;
                replace.setType(Material.CHEST);
                Chest chest = (Chest) replace.getState();
                Inventory chestInventory = chest.getBlockInventory();
                chestInventory.clear();
                for (ItemStack item : chestBlock.getItems()) chestInventory.addItem(item);
            }
            default -> {
                if (randomizedBlock instanceof NormalBlock block) replace.setTypeIdAndData(block.getId(), block.getData(), true);
                else log.log(Level.WARNING, "The randomized block's type ({0}) is not CHEST, BLOCK, or LOOT.", randomizedBlock.getType());
            }
        }
    }
}