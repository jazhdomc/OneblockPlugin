package mc.jazhdo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.chat.TextComponent;

public class OneblockCommands implements CommandExecutor {
    private final OneblockPlugin plugin;
    private final OneblockListener listener;
    private final FileConfiguration config;
    private final Logger log;
    private final List<TpRequest> tpRequests = new ArrayList<>();

    public OneblockCommands(OneblockPlugin plugin, OneblockListener listener) {
        this.plugin = plugin;
        this.listener = listener;
        config = plugin.getConfig();
        log = plugin.getLogger();
    }

    @Override
    // @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Make sure the sender is player
        if (!(sender instanceof Player)) {
            if (sender != null) sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // Make sure a subcommand is specified
        Player player = (Player) sender;
        if (args.length == 0) {
            if (checkPerms(player, "help")) sendHelp(player);
            else sendInfo(player, ChatColor.RED + "Help denied.");
            return true;
        }

        // Do the right stuff based on the subcommand
        String base = "islands." + player.getName().toLowerCase() + ".";
        switch (args[0].toLowerCase()) {
            case "guide" -> {
                if (checkPerms(player, "guide")) sendMessage(player, config.getString("guide"));
            }
            case "help" -> {
                if (checkPerms(player, "help")) sendHelp(player);
                else sendInfo(player, ChatColor.RED + "Help denied.");
            }
            case "home" -> {
                if (checkPerms(player, "home")) {
                    double homeX, homeY, homeZ, yaw, pitch;

                    // Create oneblock island if nonexistant
                    if (!config.contains(base)) {
                        // Get next island and its coords
                        double islandSpacing = config.getDouble("island-spacing");
                        Location loc = indexToLocation(islandSpacing);
                        double x = loc.getX(), y = loc.getY(), z = loc.getZ();

                        // Set config values
                        ConfigurationSection section = config.createSection("islands." + player.getName().toLowerCase());
                        section.set("x", (int) x / islandSpacing);
                        section.set("z", (int) z / islandSpacing);
                        homeX = x + 0.5;
                        homeY = y + 1;
                        homeZ = z + 0.5;
                        yaw = config.getDouble("default-home-yaw");
                        pitch = config.getDouble("default-home-pitch");
                        section.set("home.y", homeY);
                        section.set("home.x", homeX);
                        section.set("home.z", homeZ);
                        section.set("home.yaw", yaw);
                        section.set("home.pitch", pitch);
                        section.set("spawn.x", homeX);
                        section.set("spawn.y", homeY);
                        section.set("spawn.z", homeZ);
                        section.set("spawn.yaw", yaw);
                        section.set("spawn.pitch", pitch);
                        section.set("blocks", 0);
                        section.set("tier-blocks", 0);
                        section.set("tier", listener.tiers.isEmpty() ? null : listener.tiers.get(0));
                        section.set("phase", listener.phases.get(0));
                        section.set("trusted", new ArrayList<>());
                        section.set("trusted-perms", List.of("player-move", "crafting-use"));
                        section.set("visitor-perms", List.of("player-move"));

                        // Save all the changes
                        plugin.saveConfig();

                        // Create oneblock
                        Location oneblock = new Location(listener.oneblockWorld, x, y, z);
                        Block oneblockBlock = oneblock.getBlock();
                        if (!listener.starterBlocks.isEmpty()) {
                            NormalBlock starterBlock = listener.starterBlocks.get(0);
                            if (starterBlock == null) {
                                log.log(Level.WARNING, "The first starter block is null. Defaulting to grass.");
                                oneblockBlock.setTypeIdAndData(Material.GRASS.getId(), (byte) 0, true);
                            } else oneblockBlock.setTypeIdAndData(starterBlock.getId(), starterBlock.getData(), true);
                        } else listener.replaceBlock(this, base, oneblockBlock, player);

                        // Create safety block
                        Location safetyBlock = new Location(listener.oneblockWorld, x, y - 1, z);
                        safetyBlock.getBlock().setType(Material.OBSIDIAN);
                    } else {
                        // Get home values if not setting them by creating a island
                        ConfigurationSection section = config.getConfigurationSection(base + "home");
                        homeX = section.getDouble("x");
                        homeY = section.getDouble("y");
                        homeZ = section.getDouble("z");
                        yaw = section.getDouble("yaw");
                        pitch = section.getDouble("pitch");
                    }

                    // Teleport player to their home
                    sendInfo(player, ChatColor.YELLOW + "Teleporting you to your oneblock home...");
                    player.teleport(new Location(listener.oneblockWorld, homeX, homeY, homeZ, (float) yaw, (float) pitch));
                }
            }
            case "list" -> {
                if (checkPerms(player, "mod")) {
                    ConfigurationSection list = config.getConfigurationSection("islands");
                    String listMsg = ChatColor.GOLD + "=== Oneblock Island List ===" + ChatColor.YELLOW;
                    for (String key : list.getKeys(false)) listMsg += "\n" + key + " - x: " + list.getString(key + ".x") + " - z: " + list.getString(key + ".z");
                    sendMessage(player, listMsg);
                }
            }
            case "playerlist" -> {
                if (checkPerms(player, "mod")) {
                    List<Player> playerList = listener.oneblockWorld.getPlayers();
                    String listMsg = ChatColor.GOLD + "=== Oneblock Player List ===" + ChatColor.YELLOW;
                    for (Player p : playerList) {
                        String playerName = p.getName();
                        ConfigurationSection section = config.getConfigurationSection("islands." + playerName.toLowerCase());
                        listMsg += "\n" + playerName + " - x: " + section.getString("x") + " z: " + section.getString("z");
                    }
                    sendMessage(player, listMsg);
                }
            }
            case "perm" -> {
                if (checkPerms(player, "perm") && hasIsland(player)) {
                    if (args.length > 1 && (args[1].toLowerCase().equals("visitor") || args[1].toLowerCase().equals("trusted"))) {
                        // Create inventory
                        Inventory permUI = Bukkit.createInventory(player, 27, config.getString("perm-menu.title." + args[1].toLowerCase()));
                        
                        // Set items with their blocks
                        List<String> materials = config.getStringList("perm-menu.blocks"), names = config.getStringList("perm-menu.names"), descriptions = config.getStringList("perm-menu.descriptions"), currentPerms = config.getStringList(base + args[1] + "-perms");
                        for (int i = 0; i < materials.size(); i++) {
                            // Create item
                            ItemStack item = new ItemStack(Material.getMaterial(materials.get(i).toUpperCase()));

                            // Set metadata (display name and description (lore))
                            ItemMeta meta = item.getItemMeta();
                            String displayName = names.get(i);
                            meta.setDisplayName(displayName);
                            meta.setLore(List.of(descriptions.get(i)));
                            if (currentPerms.contains(displayName)) meta.addEnchant(Enchantment.DURABILITY, 5, true);
                            item.setItemMeta(meta);

                            // Put item in the slot
                            permUI.setItem(i, item);
                        }

                        // Show player
                        player.openInventory(permUI);
                    } else sendInfo(player, ChatColor.RED + "<visitor|trusted> argument required to be \"visitor\" or \"trusted\". (/ob perm <visitor|trusted>)");
                }
            }
            case "phase" -> sendInfo(player, ChatColor.RED + "This command is a work in progress. Check back in a later version to see if it's been added.");
            case "count" -> {
                if (checkPerms(player, "phase")) {
                    base = listener.getIslandFromLocation(player.getLocation());
                    if (base == null) {
                        sendInfo(player, ChatColor.RED + "You have to be on a island to check the island's block count and phase.");
                        return true;
                    }
                    ConfigurationSection section = config.getConfigurationSection(base.substring(0, base.length() - 1));
                    String phase = section.getString("phase"), blockCount = section.getString("blocks");
                    int blockCountInt = Integer.parseInt(blockCount);
                    String tierMessage = "";
                    if (!listener.tiers.isEmpty()) {
                        String tier = section.getString("tier");
                        if (tier == null) tier = listener.tiers.get(0);
                        int tierIndex = listener.tiers.indexOf(tier);
                        int tierProgress = section.getInt("tier-blocks");
                        int tierLength = config.getInt("tiers." + tier + ".length");
                        tierMessage = "\nCurrent Tier (" + (tierIndex + 1) + "/" + listener.tiers.size() + "): " + tier
                            + "\nTier Blocks: " + tierProgress + "/" + tierLength;
                    }
                    int phaseIndex = listener.phases.indexOf(phase);
                    if (phaseIndex < 0) phaseIndex = 0;
                    List<Integer> phaseLength = config.getIntegerList("phase-length");
                    String untilNextPhase = "Phase complete";
                    if (phaseIndex + 1 < listener.phases.size()) {
                        int required = phaseLength.size() > phaseIndex + 1 ? phaseLength.get(phaseIndex + 1) : 0;
                        untilNextPhase = listener.phases.get(phaseIndex + 1) + " (" + Integer.toString(Math.max(0, required - blockCountInt)) + " blocks)";
                    }
                    sendInfo(player,
                        "Current Phase (" + Integer.toString(phaseIndex + 1) + "/" + Integer.toString(listener.phases.size()) + "): " + phase
                        + "\nBlock Count: " + blockCount
                        + "\nNext Phase: " + untilNextPhase
                        + tierMessage
                    );
                }
            }
            case "reload" -> {
                if (checkPerms(player, "mod")) {
                    plugin.reloadConfig();
                    listener.generatePhaseBlocks();
                }
            }
            case "reset" -> {
                if (checkPerms(player, "reset") && hasIsland(player)) {
                    // Make sure a confirm argument is present to make sure the player is ok with going through with this or else show them the warning
                    if (args.length > 1 && args[1].equals("confirm")) {
                        int islandSpacing = config.getInt("island-spacing"), range = islandSpacing / 2 - 1;
                        int[] oneblock = {config.getInt(base + "x") * islandSpacing, config.getInt(base + "z") * islandSpacing}, start = {oneblock[0] - range, 0, oneblock[1] - range}, end = {oneblock[0] + range, 255, oneblock[1] + range};
                        
                        // Teleport player to ontop their oneblock to make sure they are safe
                        Double defaultHomeYaw = config.getDouble("default-home-yaw"), defaultHomePitch = config.getDouble("default-home-pitch");
                        player.teleport(new Location(listener.oneblockWorld, oneblock[0], listener.oneblockY + 1, oneblock[1], defaultHomeYaw.floatValue(), defaultHomePitch.floatValue()));

                        // Set values
                        ConfigurationSection section = config.getConfigurationSection(base + "home");
                        section.set("x", oneblock[0]);
                        section.set("y", listener.oneblockY + 1);
                        section.set("z", oneblock[1]);
                        section.set("yaw", defaultHomeYaw);
                        section.set("pitch", defaultHomePitch);

                        // Remove all the blocks except the oneblock and the obsidian below
                        sendInfo(player, "Resetting your oneblock island... DO NOT RUN THE COMMAND AGAIN BEFORE IT FINISHES.");
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            int x, y, z;
                            for (x = start[0]; x <= end[0]; x++) for (y = start[1]; y <= end[1]; y++) for (z = start[2]; z <= end[2]; z++) if (x != 0 || z != 0 || (y != listener.oneblockY && y != listener.oneblockY - 1)) listener.oneblockWorld.getBlockAt(x, y, z).setType(Material.AIR);
                        });
                        
                        // Give player a indication of conclusion.
                        sendInfo(player, "Your oneblock island has been reset.");
                    } else sendInfo(player, config.getString("oneblock-reset-warning"));
                }
            }
            case "resethome" -> {
                if (checkPerms(player, "home") && hasIsland(player)) {
                    // Set values
                    ConfigurationSection section = config.getConfigurationSection(base + "home");
                    double islandSpacing = config.getInt("island-spacing");
                    section.set("x", (config.getDouble(base + "x") * islandSpacing) + 0.5);
                    section.set("y", listener.oneblockY + 1);
                    section.set("z", (config.getDouble(base + "z") * islandSpacing) + 0.5);
                    section.set("yaw", config.getDouble("default-home-yaw"));
                    section.set("pitch", config.getDouble("default-home-pitch"));

                    // Save config
                    plugin.saveConfig();
                    sendInfo(player, ChatColor.YELLOW + "Your home location has been reset.");
                }
            }
            case "sethome" -> setLocation(args, player, base, true);
            case "setspawn" -> setLocation(args, player, base, false);
            case "spawn" -> visit(player, player.getName());
            case "tp" -> {
                if (checkPerms(player, "mod")) {
                    // Check if the tp is to a island, else tp to a player
                    String[] parts = args[1].split(",");
                    if (parts.length > 1) {
                        // Get coords
                        double islandSpacing = config.getDouble("island-spacing"), x = Double.parseDouble(parts[0]) * islandSpacing, z = Double.parseDouble(parts[1]) * islandSpacing, y = listener.oneblockY + 1;

                        // Send teleporting message
                        sendInfo(player, ChatColor.YELLOW + "Teleporting to (" + Double.toString(x) + ", " + Double.toString(y) + ", "+ Double.toString(z) + ")");

                        // Teleport player to the target location
                        player.teleport(new Location(listener.oneblockWorld, x, y, z));
                    } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) sendInfo(player, ChatColor.RED + "Player " + args[1] + " is currently offline or does not exist.");
                        else {
                            sendInfo(player, ChatColor.YELLOW + "Teleporting to " + target.getName());
                            player.teleport(target.getLocation());
                        }
                    }
                }
            }
            case "tpa" -> {
                if (checkPerms(player, "tp-request")) {
                    if (args.length < 2) sendInfo(player, ChatColor.RED + "<player> argument required. (/ob tpa <player>)");
                    else {
                        for (TpRequest current : tpRequests) {
                            if (current.ifAccept(args[1], player.getName())) {
                                if (current.tpPlayer()) {
                                    sendInfo(player, ChatColor.YELLOW + "Teleporting " + args[1] + " to you.");
                                    sendInfo(Bukkit.getPlayer(args[1]), ChatColor.YELLOW + "Teleporting you to " + player.getName() + ".");
                                } else sendInfo(player, ChatColor.RED + "Unsuccessful teleportation. Player " + args[1] + " offline.");
                                return true;
                            }
                        }
                        sendInfo(player, ChatColor.RED + "Unsuccessful tpa. Request not found");
                    }
                }
            }
            case "tpr" -> {
                if (checkPerms(player, "tp-request")) {
                    // Make sure a player argument was given
                    if (args.length > 1) {
                        // Make sure the player teleporting to is online
                        Player to = Bukkit.getPlayer(args[1]);
                        if (to == null) sendInfo(player, ChatColor.RED + "Could not find player " + args[1] + ". Make sure they are online.");
                        else {
                            // Create new request
                            TpRequest request = new TpRequest(player.getName(), args[1]);
                            request.setTask(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                // Send expiration notices
                                String[] players = request.getPlayers();
                                Player player1 = Bukkit.getPlayer(players[0]), player2 = Bukkit.getPlayer(players[1]);
                                if (player1 != null) sendInfo(player1, ChatColor.YELLOW + "Your teleportation request to " + players[1] + " has expired.");
                                if (player2 != null) sendInfo(player2, ChatColor.YELLOW + players[0] + "'s teleportation request to you has expired.");

                                // Remove request
                                tpRequests.remove(request);
                            }, 1200L));
                            tpRequests.add(request);

                            // Notify both parties of the request
                            sendInfo(player, ChatColor.YELLOW + "Teleportation request sent. It will expire in 60 seconds.");
                            sendInfo(to, ChatColor.YELLOW + "You have recieved a teleportation request from" + player.getName() + ". It will expire in 60 seconds.");
                        }
                    } else sendInfo(player, ChatColor.RED + "<player> argument required (/ob tpr <player>)");
                }
            }
            case "trust" -> trust(args, player, base, true);
            case "trustlist" -> {
                if (checkPerms(player, "trust") && hasIsland(player)) {
                    List<String> trustList = config.getStringList(base + "trusted");
                    String trustListMsg = ChatColor.GOLD + config.getString("trusted-list-header") + ChatColor.YELLOW;
                    for (int i = 0; i < trustList.size(); i++) trustListMsg += "\n" + Integer.toString(i + 1) + ". " + trustList.get(i);
                    sendMessage(player, trustListMsg);
                }
            }
            case "untrust" -> trust(args, player, base, false);
            case "user" -> {
                if (checkPerms(player, "mod")) {
                    // Make sure player argument was given
                    if (args.length > 1) {
                        // Make sure player has a island
                        ConfigurationSection section = config.getConfigurationSection("islands." + args[1].toLowerCase());
                        if (section == null) sendInfo(player, ChatColor.RED + "Player " + args[1] + " does not have a island.");
                        else sendMessage(player,
                            config.getString("oneblock-user-header").replace("%p", args[1])
                            + "\nIsland: x: " + section.getString("x") + " y: " + section.getString("y")
                            + "\nHome: x: " + section.getString("home.x") + " y: " + section.getString("home.y") + " z: " + section.getString("home.z")
                            + "\nVisitor Spawn: x: " + section.getString("spawn.x") + " y: " + section.getString("spawn.y") + " z: " + section.getString("spawn.z")
                            + "\nBlocks: " + section.getString("blocks")
                            + "\nPhase: " + section.getString("phase")
                        );
                    } else sendInfo(player, ChatColor.RED + "<player> argument required. (/ob user <player>)");
                }
            }
            case "visit" -> {
                if (args.length < 2) sendInfo(player, ChatColor.RED + "<player> argument required. (/ob visit <player)");
                visit(player, args[1]);
            }
            default -> {
                sendInfo(player, ChatColor.RED + "Unknown subcommand.");
                if (checkPerms(player, "help")) sendHelp(player);
            }
        }

        return true;
    }

    public Boolean hasIsland(Player player) {
        if (config.contains("islands." + player.getName().toLowerCase())) return true;
        sendInfo(player, ChatColor.RED + "You need a island to perform this command. Use the command \"/ob home\" to create one.");
        return false;
    }

    private Boolean checkPerms(Player player, String perm) {
        if (player.hasPermission("oneblock." + perm)) return true;
        sendInfo(player, ChatColor.RED + "Permission denied. If this is incorrect, get help from a moderator or admin.");
        return false;
    }

    public void sendInfo(Player player, String msg) {
        // Send each line separately
        String[] msgParts = msg.split("\n");
        for (String part : msgParts) sendMessage(player, ChatColor.YELLOW + config.getString("msg-prefix") + ChatColor.WHITE + part);
    }
    
    private void sendMessage(Player player, String msg) {
        player.spigot().sendMessage(TextComponent.fromLegacyText(msg));
    }
    
    private void sendHelp(Player player) {
        // Prefill the help message with the header
        String helpMessage = ChatColor.GOLD + "=== Oneblock Help ===" + ChatColor.YELLOW;

        // Get the commands and messages as a list
        List<String> cmds = config.getStringList("help.cmds"), msgs = config.getStringList("help.msgs");

        // Dynamically build help message with custom strings
        for (int i = 0; i < cmds.size(); i++) helpMessage += "\n/oneblock " + cmds.get(i) + " - " + msgs.get(i);

        // If player has mod perms, build mod help section
        if (player.hasPermission("oneblock.mod")) {
            // Add header (first newline is for separating the two help sections)
            helpMessage += ChatColor.GOLD + "\n=== Oneblock Mod Help ===" + ChatColor.YELLOW;

            // Get the commands and messages as a list
            cmds = config.getStringList("mod-help.cmds");
            msgs = config.getStringList("mod-help.msgs");

            // Dynamically build mod-help section with custom strings
            for (int i = 0; i < cmds.size(); i++) helpMessage += "\n/oneblock " + cmds.get(i) + " - " + msgs.get(i);
        }

        // Send finished help message to player
        sendMessage(player, helpMessage);
    }

    private void setLocation(String[] args, Player player, String base, boolean home) {
        // Make sure perms exist
        if (checkPerms(player, home ? "home" : "visitor") && hasIsland(player)) {
            // Location variables
            double x, y, z;
            float yaw, pitch;

            // If its "x,y,z", "x,y,z,yaw,pitch", or just the current location
            String sethome = home ? "home" : "spawn";
            if (Math.abs(args.length - 5) == 1) {
                // Get x, y, and z
                x = Double.parseDouble(args[1]);
                y = Double.parseDouble(args[2]);
                z = Double.parseDouble(args[3]);

                // Get yaw and pitch if provided, else use defaults
                if (args.length == 6) {
                    yaw = Float.parseFloat(args[4]);
                    pitch = Float.parseFloat(args[5]);
                } else {
                    yaw = (float) config.getDouble("default-" + sethome + "-yaw");
                    pitch = (float) config.getDouble("default-" + sethome + "-pitch");
                }
            } else {
                // Get player loc data
                Location playerLoc = player.getLocation();
                x = playerLoc.getX();
                y = playerLoc.getY();
                z = playerLoc.getZ();
                yaw = playerLoc.getYaw();
                pitch = playerLoc.getPitch();
            }

            // Set values
            ConfigurationSection section = config.getConfigurationSection(base + sethome);
            section.set("x", x);
            section.set("y", y);
            section.set("z", z);
            section.set("yaw", yaw);
            section.set("pitch", pitch);

            // Save
            plugin.saveConfig();

            // Completion message
            sendInfo(player, (home ? "Home" : "Visitor Spawn") + " location has been set.");
        }
    }

    private void trust(String[] args, Player player, String base, boolean trust) {
        // Make sure trust perm exists
        if (checkPerms(player, "trust") && hasIsland(player)) {
            // Make sure the player argument exists
            if (args.length < 2) sendMessage(player, "\"player\" argument required");
            else {
                // If to change all the users or just one
                List<String> trusted = config.getStringList(base + "trusted");
                if (args[1].toLowerCase().equals("all")) {
                    if (trust) trusted.add("*");
                    else trusted = new ArrayList<>();
                } else {
                    if (!trusted.contains(args[1].toLowerCase()) && trust) trusted.add(args[1].toLowerCase());
                    else if (trusted.contains(args[1].toLowerCase()) && !trust) trusted.remove(args[1].toLowerCase());
                }
                
                // Set changed list
                config.set(base + "trusted", trusted);

                // Save
                plugin.saveConfig();

                // Send completion msg
                sendInfo(player, "Player " + args[1] + " has been " + (trust ? "added to" : "removed from") + " your trusted list.");
            }
        }
    }

    private void visit(Player player, String target) {
        // Make sure perms exist
        if (checkPerms(player, "visitor")) {
            // Make sure island exists
            if (config.contains("islands." + target.toLowerCase())) {
                // Tell player about the teleportation
                sendInfo(player, config.getString("oneblock-visit-msg").replace("%p", target));

                // Teleport player
                ConfigurationSection section = config.getConfigurationSection("islands." + target.toLowerCase() + ".spawn");
                player.teleport(new Location(
                    listener.oneblockWorld,
                    section.getDouble("x"),
                    section.getDouble("y"),
                    section.getDouble("z"),
                    (float) section.getDouble("yaw"),
                    (float) section.getDouble("pitch"))
                );
            } else sendInfo(player, "Island not found.");
        }
    }

    private Location indexToLocation(double spacing) {
        int index = config.getConfigurationSection("islands").getKeys(false).size(), x = 0, z = 0, step = 1, taken = 0, turns = 0, i = 0;

        // Directions: right, down, left, up
        int[] dx = {1, 0, -1, 0}, dz = {0, 1, 0, -1};

        while (i < index) {
            int dir = turns % 4;
            x += dx[dir];
            z += dz[dir];
            taken++;
            i++;

            if (taken == step) {
                taken = 0;
                turns++;
                // Increase step every 2 turns
                if (turns % 2 == 0) step++;
            }
        }

        return new Location(listener.oneblockWorld, x * spacing, listener.oneblockY, z * spacing);
    }
}
