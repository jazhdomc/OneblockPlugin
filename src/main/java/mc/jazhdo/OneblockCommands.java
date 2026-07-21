package mc.jazhdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

    public OneblockCommands(OneblockPlugin plugin, OneblockListener listener) {
        this.plugin = plugin;
        this.listener = listener;
        config = plugin.getConfig();
    }

    private List<TpRequest> tpRequests;

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
            else sendInfo(player, "Help denied.");
            return true;
        }

        // Do the right stuff based on the subcommand
        Logger logger = plugin.getLogger();
        String base = "islands.".concat(player.getName().toLowerCase()).concat(".");
        switch (args[0].toLowerCase()) {
            case "gamemode" -> {
                if (checkPerms(player, "gamemode")) {
                    // Check if the "player" argument is added
                    if (args.length > 1) player = Bukkit.getPlayer(args[1]);

                    // Set player to spectator if in survival & is a mod, else default to survival
                    if (player.getGameMode().equals(GameMode.SURVIVAL) && player.hasPermission("gamemode")) player.setGameMode(GameMode.SPECTATOR);
                    else player.setGameMode(GameMode.SURVIVAL);
                }
            }
            case "guide" -> {
                if (checkPerms(player, "guide")) sendMessage(player, config.getString("guide"));
            }
            case "help" -> {
                if (checkPerms(player, "help")) sendHelp(player);
                else sendInfo(player, "Help denied.");
            }
            case "home" -> {
                if (checkPerms(player, "home")) {
                    World oneblockWorld = Bukkit.getWorld(config.getString("oneblock-world"));
                    double homeX, homeY, homeZ, yaw, pitch;

                    // Create oneblock island if nonexistant
                    if (!config.contains(base)) {
                        // Get next island & increase counter
                        int index = config.getInt("next-island-index");
                        double islandSpacing = config.getDouble("island-spacing");
                        Location loc = indexToLocation(index, islandSpacing);
                        config.set("next-island-index", index + 1);

                        // Get coords
                        double x = loc.getX(), y = loc.getY(), z = loc.getZ();

                        // Set config values
                        config.set(base.concat("x"), x/islandSpacing);
                        config.set(base.concat("z"), z/islandSpacing);
                        homeX = x;
                        homeY = y + 1;
                        homeZ = z;
                        yaw = config.getDouble("default-home-yaw");
                        pitch = config.getDouble("default-home-pitch");
                        config.set(base.concat("home.x"), homeX);
                        config.set(base.concat("home.y"), homeY);
                        config.set(base.concat("home.z"), homeZ);
                        config.set(base.concat("home.yaw"), yaw);
                        config.set(base.concat("home.pitch"), pitch);
                        config.set(base.concat("spawn.x"), homeX);
                        config.set(base.concat("spawn.y"), homeY);
                        config.set(base.concat("spawn.z"), homeZ);
                        config.set(base.concat("spawn.yaw"), yaw);
                        config.set(base.concat("spawn.pitch"), pitch);
                        config.set(base.concat("blocks"), 0);
                        config.set(base.concat("phase"), config.getStringList("phases").get(0));
                        config.set(base.concat("trusted"), new ArrayList<>());
                        config.set(base.concat("trusted-perms"), new ArrayList<>());
                        config.set(base.concat("visitor-perms"), new ArrayList<>());

                        // Save all the changes
                        plugin.saveConfig();

                        // Create oneblock
                        List<String> starter = config.getStringList("starter");
                        Location oneblock = new Location(oneblockWorld, x, y, z);
                        Block oneblockBlock = oneblock.getBlock();
                        if (!starter.isEmpty()) {
                            String[] parts = starter.get(0).split(":");
                            if (parts.length != 0) {
                                Material material = Material.getMaterial(parts[0]);
                                if (material == null) logger.warning("Material name ".concat(parts[0]).concat(" is invalid."));
                                oneblockBlock.setType(material);
                                if (parts.length > 1) oneblockBlock.setData(Byte.parseByte(parts[1]));
                            } else logger.warning("The first block of the starter selection is invalid.");
                        } else listener.replaceBlock(oneblockBlock, player);

                        // Create safety block
                        Location safetyBlock = new Location(oneblockWorld, x, y - 1, z);
                        safetyBlock.getBlock().setType(Material.OBSIDIAN);
                    } else {
                        // Get home values if not setting them by creating a island
                        homeX = config.getDouble(base.concat("home.x"));
                        homeY = config.getDouble(base.concat("home.y"));
                        homeZ = config.getDouble(base.concat("home.z"));
                        yaw = config.getDouble(base.concat("home.yaw"));
                        pitch = config.getDouble(base.concat("home.pitch"));
                    }

                    // Teleport player to their home
                    sendInfo(player, config.getString("oneblock-home-msg"));
                    player.teleport(new Location(oneblockWorld, homeX, homeY, homeZ, (float) yaw, (float) pitch));
                }
            }
            case "list" -> {
                if (checkPerms(player, "mod")) {
                    List<Player> playerList = Bukkit.getWorld(config.getString("oneblock-world")).getPlayers();
                    String listMsg = config.getString("oneblock-list-header");
                    for (Player i : playerList) {
                        String playerIsland = "islands.".concat(i.getName().toLowerCase()).concat(".");
                        listMsg += "\n".concat(player.getName()).concat(" - x: ").concat(config.getString(playerIsland.concat("x"))).concat(" z: ").concat(config.getString(playerIsland.concat("z")));
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
                            ItemStack item = new ItemStack(Material.valueOf(materials.get(i).toUpperCase()));

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
                    } else sendInfo(player, "<visitor|trusted> argument required to be \"visitor\" or \"trusted\". (/oneblock perm <visitor|trusted>)");
                }
            }
            case "phase" -> {
                sendInfo(player, ChatColor.RED + "This command is a work in progress. Check back in a later version to see if it's been added.");
                // if (checkPerms(player, "phase") && hasIsland(player)) {
                //     // Setup chest
                //     List<String> phases = config.getStringList("phases");
                //     int rows = (int) (Math.ceil(phases.size() / 7.0) + 2);
                //     Inventory phaseUI = Bukkit.createInventory(player, rows * 9, config.getString("phase-menu.title"));

                //     // Parse border block
                //     String[] borderParts = config.getString("phase-menu.border").split(":");
                //     ItemStack border = new ItemStack(Material.getMaterial(borderParts[0]));
                //     border.setDurability(borderParts.length > 1 ? Short.parseShort(borderParts[1]) : 0);

                //     // Set first row of border blocks + the first one on the second row
                //     ItemStack exitButton = new ItemStack(Material.BARRIER);
                //     ItemMeta exitButtonMeta = exitButton.getItemMeta();
                //     exitButtonMeta.setDisplayName("Close Menu");
                //     exitButton.setItemMeta(exitButtonMeta);
                //     phaseUI.setItem(0, exitButton);
                //     int i = 1;
                //     for (; i < 10; i++) phaseUI.setItem(i, border);

                //     // Get locked phases info
                //     List<Integer> phaseLength = config.getIntegerList("phase-length");
                //     int total = 0;
                //     for (int j : phaseLength) total += j;
                //     String currentPhaseString = config.getString(base.concat("phase"));
                //     int currentPhase = phases.indexOf(currentPhaseString);
                //     if (currentPhase == -1) logger.warning(() -> "Current phase " + currentPhaseString + " is not in the phases list.");
                //     if (total < config.getInt(base.concat("blocks"))) currentPhase = phases.size();

                //     // Get the locked icon beforehand
                //     String[] lockedIcon = config.getString("phase-menu.locked").split(":");
                //     Material lockedMaterial = Material.getMaterial(lockedIcon[0]);
                //     if (lockedMaterial == null) logger.warning("Material ".concat(lockedIcon[0]).concat(" is invalid."));

                //     // Build the next few rows
                //     int phaseOn = 0;
                //     List<String> phaseIcons = config.getStringList("phase-menu.icons"), phaseDesc = config.getStringList("phase-descriptions");
                //     for (int j = 0; j < rows - 2; j++) {
                //         for (int k = 0; k < 7; k++, i++) {
                //             if (phaseOn < phaseIcons.size()) {
                //                 // Only show the phase's true icon if the phase isn't locked
                //                 String[] phaseIcon;
                //                 Material phaseMaterial;
                //                 if (phaseOn <= currentPhase) {
                //                     // Get the icon material
                //                     phaseIcon = phaseIcons.get(phaseOn).split(":");
                //                     phaseMaterial = Material.getMaterial(phaseIcon[0]);

                //                     // Make sure the material is valid
                //                     if (phaseMaterial == null) logger.warning("Material ".concat(phaseIcon[0]).concat(" is invalid."));
                //                     else {
                //                         // Get the phase icon item
                //                         ItemStack phaseItem = new ItemStack(phaseMaterial);

                //                         // Set the item's durability value
                //                         phaseItem.setDurability(phaseIcon.length > 1 ? Short.parseShort(phaseIcon[1]) : 0);

                //                         // Set item metadata (display name, description, & enchantment for indication of allowed)
                //                         ItemMeta meta = phaseItem.getItemMeta();
                //                         String title = phases.get(phaseOn);
                //                         meta.setDisplayName(title.substring(0, 1).toUpperCase().concat(title.substring(1)));
                //                         meta.setLore(List.of(phaseDesc.get(phaseOn)));
                //                         if (phaseOn == currentPhase) meta.addEnchant(Enchantment.DURABILITY, 5, true);
                //                         phaseItem.setItemMeta(meta);

                //                         // Set item in slot
                //                         phaseUI.setItem(i, phaseItem);
                //                     }
                //                 } else {
                //                     // Set the locked icon
                //                     ItemStack phaseItem = new ItemStack(lockedMaterial);
                //                     phaseItem.setDurability(lockedIcon.length > 1 ? Short.parseShort(lockedIcon[1]) : 0);
                //                     ItemMeta phaseMeta = phaseItem.getItemMeta();
                //                     phaseMeta.setDisplayName("Locked phase");
                //                     phaseItem.setItemMeta(phaseMeta);
                //                     phaseUI.setItem(i, phaseItem);
                //                 }
                //                 phaseOn++;
                //             } else phaseUI.setItem(i, border);
                //         }
                //         for (int l = 0; l < 2; l++, i++) phaseUI.setItem(i, border);
                //     }

                //     // Set last row
                //     for (int h = 0; h < 8; h++, i++) phaseUI.setItem(i, border);
                    
                //     // Show player the phase selector
                //     player.openInventory(phaseUI);
                // }
            }
            case "phasecount" -> {
                if (checkPerms(player, "phase") && hasIsland(player)) sendInfo(player, config.getString("phasecount-msg").replace("%p", config.getString(base.concat("phase"))).replace("%c", config.getString(base.concat("blocks"))));
            }
            case "reload" -> {
                if (checkPerms(player, "mod")) plugin.reloadConfig();
            }
            case "reset" -> {
                if (checkPerms(player, "reset") && hasIsland(player)) {
                    // Make sure a confirm argument is present to make sure the player is ok with going through with this or else show them the warning
                    if (args.length > 1 && args[1].equals("confirm")) {
                        int islandSpacing = config.getInt("island-spacing"), range = islandSpacing / 2 - 1;
                        int[] oneblock = {config.getInt(base + "x") * islandSpacing, config.getInt(base + "z") * islandSpacing}, start = {oneblock[0] - range, 0, oneblock[1] - range}, end = {oneblock[0] + range, 255, oneblock[1] + range};
                        
                        // Teleport player to ontop their oneblock to make sure they are safe
                        World oneblockWorld = Bukkit.getWorld(config.getString("oneblock-world"));
                        Double oneblockY = config.getDouble("oneblock-y"), defaultHomeYaw = config.getDouble("default-home-yaw"), defaultHomePitch = config.getDouble("default-home-pitch");
                        player.teleport(new Location(oneblockWorld, oneblock[0], oneblockY + 1, oneblock[1], defaultHomeYaw.floatValue(), defaultHomePitch.floatValue()));

                        // Set values
                        base += "home.";
                        config.set(base.concat("x"), oneblock[0]);
                        config.set(base.concat("y"), oneblockY + 1);
                        config.set(base.concat("z"), oneblock[1]);
                        config.set(base.concat("yaw"), defaultHomeYaw);
                        config.set(base.concat("pitch"), defaultHomePitch);

                        // Remove all the blocks except the oneblock and the obsidian below
                        int x, y, z;
                        for (x = start[0]; x <= end[0]; x++) for (y = start[1]; y <= end[1]; y++) for (z = start[2]; z <= end[2]; z++) if (x != 0 || z != 0 || (y != oneblockY && y != oneblockY - 1)) oneblockWorld.getBlockAt(x, y, z).setType(Material.AIR);
                        
                        // Give player a indication of conclusion.
                        sendInfo(player, "Your Oneblock island has been reset.");
                    } else sendInfo(player, config.getString("oneblock-reset-warning"));
                }
            }
            case "resethome" -> {
                if (checkPerms(player, "home") && hasIsland(player)) {
                    // Set values
                    base += "home.";
                    double islandSpacing = config.getInt("island-spacing");
                    config.set(base.concat("x"), config.getDouble(base.concat("x")) * islandSpacing);
                    config.set(base.concat("y"), config.getDouble("oneblock-y") + 1);
                    config.set(base.concat("z"), config.getDouble(base.concat("z")) * islandSpacing);
                    config.set(base.concat("yaw"), config.getDouble("default-home-yaw"));
                    config.set(base.concat("pitch"), config.getDouble("default-home-pitch"));

                    // Save config
                    plugin.saveConfig();
                    sendInfo(player, "Your home location has been reset.");
                }
            }
            case "sethome" -> setLocation(args, player, base);
            case "setspawn" -> setLocation(args, player, base);
            case "spawn" -> visit(player, player.getName());
            case "tp" -> {
                if (checkPerms(player, "mod")) {
                    // Check if the tp is to a plot, else tp to a player
                    String[] parts = args[1].split(",");
                    if (parts.length > 1) {
                        // Get coords
                        double islandSpacing = config.getDouble("island-spacing");
                        double x = Double.parseDouble(parts[0]) * islandSpacing;
                        double z = Double.parseDouble(parts[1]) * islandSpacing;
                        double y = config.getDouble("oneblock-y") + 1;

                        // Send teleporting message
                        sendInfo(player, "Teleporting to x: ".concat(Double.toString(x)).concat(" y: ").concat(Double.toString(y)).concat(" z: ").concat(Double.toString(z)));

                        // Teleport player to the target location
                        player.teleport(new Location(Bukkit.getWorld(config.getString("oneblock-world")), x, y, z));
                    } else player.teleport(Bukkit.getPlayer(parts[0]).getLocation());
                }
            }
            case "tpa" -> {
                if (checkPerms(player, "tp-request")) {
                    if (args.length < 1) sendInfo(player, "\"player\" argument required.");
                    else {
                        for (int i = 0; i < tpRequests.size(); i++) {
                            TpRequest current = tpRequests.get(i);
                            if (current.ifAccept(args[1], player.getName())) {
                                if (current.tpPlayer()) {
                                    sendInfo(player, "Teleporting %p to you.".replace("%p", args[1]));
                                    sendInfo(Bukkit.getPlayer(args[1]), "Teleporting you to %p.".replace("%p", player.getName()));
                                } else sendInfo(player, "Unsuccessful teleportation. Player %p offline.".replace("%p", args[1]));
                                break;
                            }
                        }
                        sendInfo(player, "Unsuccessful tpa. Request not found");
                    }
                }
            }
            case "tpr" -> {
                if (checkPerms(player, "tp-request")) {
                    if (args.length < 1) sendInfo(player, "\"player\" argument required");
                    else {
                        TpRequest request = new TpRequest(player.getName(), args[1]);
                        request.setTask(Bukkit.getScheduler().runTaskLater(plugin, () -> tpRequests.remove(request), 1200L));
                        tpRequests.add(request);
                        sendInfo(player, "TP request made.");
                    }
                }
            }
            case "trust" -> trust(args, player, base);
            case "trustlist" -> {
                if (checkPerms(player, "trust") && hasIsland(player)) {
                    List<String> trustList = config.getStringList(base.concat("trusted"));
                    String trustListMsg = config.getString("trusted-list-header");
                    for (int i = 0; i < trustList.size(); i++) trustListMsg += "\n".concat(Integer.toString(i + 1)).concat(". ").concat(trustList.get(i));
                    sendMessage(player, trustListMsg);
                }
            }
            case "untrust" -> trust(args, player, base);
            case "user" -> {
                if (checkPerms(player, "mod")) {
                    if (args.length < 2) sendInfo(player, "\"player\" argument required.");
                    else {
                        base = "islands.".concat(args[1].toLowerCase()).concat(".");
                        sendMessage(player,
                            config.getString("oneblock-user-header").replace("%p", args[1])
                            .concat("\nIsland: x: ").concat(config.getString(base.concat("x")))
                            .concat(" y: ").concat(config.getString(base.concat("y")))
                            .concat("\nHome: x: ").concat(config.getString(base.concat("home.x")))
                            .concat(" y: ").concat(config.getString(base.concat("home.y")))
                            .concat(" z: ").concat(config.getString(base.concat("home.z")))
                            .concat("\nVisitor Spawn: x: ").concat(config.getString(base.concat("spawn.x")))
                            .concat(" y: ").concat(config.getString(base.concat("spawn.y")))
                            .concat(" z: ").concat(config.getString(base.concat("spawn.z")))
                            .concat("\nBlocks: ").concat(config.getString(base.concat("blocks")))
                            .concat("\nPhase: ").concat(config.getString(base.concat("phase")))
                        );
                    }
                }
            }
            case "visit" -> visit(player, args[1]);
            default -> {
                sendInfo(player, ChatColor.RED + "Unknown subcommand.");
                if (checkPerms(player, "help")) sendHelp(player);
            }
        }

        return true;
    }

    private Boolean hasIsland(Player player) {
        if (config.contains("islands." + player.getName().toLowerCase())) return true;
        sendInfo(player, ChatColor.RED + "You need a island to perform this command.");
        return false;
    }

    private Boolean checkPerms(Player player, String perm) {
        if (player.hasPermission("oneblock.".concat(perm))) return true;
        sendInfo(player, ChatColor.RED + "Permission denied. If this is incorrect, get help from a moderator or admin.");
        return false;
    }

    public void sendInfo(Player player, String msg) {
        // Send each line separately
        String[] msgParts = msg.split("\n");
        for (String part : msgParts) sendMessage(player, ChatColor.YELLOW + plugin.getConfig().getString("msg-prefix") + ChatColor.WHITE + part);
    }
    
    private void sendMessage(Player player, String msg) {
        player.spigot().sendMessage(new TextComponent(msg));
    }
    
    private void sendHelp(Player player) {
        // Set config and log as a variable for easier use
        Logger log = plugin.getLogger();

        // Send warnings if ChatColor options aren't valid
        List<String> chatColors = List.of("help.header-color", "help.cmd-color", "help.msg-color", "mod-help.header-color", "mod-help.cmd-color", "mod-help.msg-color");
        List<String> chatColorNames = List.of("Help menu's header", "Help menu's command", "Help menu's message", "Mod help's header", "Mod help's command", "Mod help's message");
        for (int i = 0; i < chatColors.size(); i++) if (!Arrays.stream(ChatColor.values()).anyMatch(ChatColor.valueOf(config.getString(chatColors.get(i)).toUpperCase())::equals)) log.warning(chatColorNames.get(i).concat(" color is invalid."));

        // Prefill the help message with the header
        String helpMessage = ChatColor.valueOf(config.getString("help.header-color").toUpperCase()) + config.getString("help.header") + "\n";

        // Get the commands and messages as a list
        List<String> cmds = config.getStringList("help.cmds");
        List<String> msgs = config.getStringList("help.msgs");

        // Dynamically build help message with custom colors and strings
        for (int i = 0; i < cmds.size(); i++) helpMessage += ChatColor.valueOf(config.getString("help.cmd-color").toUpperCase()) + "/oneblock " + cmds.get(i) + ChatColor.valueOf(config.getString("help.msg-color").toUpperCase()) + " - " + msgs.get(i) + "\n";

        // If player has mod perms, build mod help section
        if (player.hasPermission("oneblock.mod")) {
            // Add header (first newline is for separating the two help sections)
            helpMessage += ChatColor.valueOf(config.getString("mod-help.header-color").toUpperCase()) + config.getString("mod-help.header");

            // Get the commands and messages as a list
            cmds = config.getStringList("mod-help.cmds");
            msgs = config.getStringList("mod-help.msgs");

            // Dynamically build mod-help section with custom colors and strings
            for (int i = 0; i < cmds.size(); i++) helpMessage += "\n" + ChatColor.valueOf(config.getString("mod-help.cmd-color").toUpperCase()) + "/oneblock " + cmds.get(i) + ChatColor.valueOf(config.getString("mod-help.msg-color").toUpperCase()) + " - " + msgs.get(i);
        }

        // Send finished help message to player
        sendMessage(player, helpMessage);
    }

    private void setLocation(String[] args, Player player, String base) {
        // home or spawn (visitor spawn)
        String sethome = args[0].toLowerCase().equals("sethome") ? "home" : "spawn";

        // Make sure perms exist
        if (checkPerms(player, sethome.equals("home") ? "home" : "visitor") && hasIsland(player)) {
            // Location variables
            double x, y, z;
            float yaw, pitch;

            // If its "x,y,z", "x,y,z,yaw,pitch", or just the current location
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
                    yaw = (float) config.getDouble("default-".concat(sethome).concat("-yaw"));
                    pitch = (float) config.getDouble("default-".concat(sethome).concat("-pitch"));
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
            base += sethome.concat(".");
            config.set(base.concat("x"), x);
            config.set(base.concat("y"), y);
            config.set(base.concat("z"), z);
            config.set(base.concat("yaw"), yaw);
            config.set(base.concat("pitch"), pitch);

            // Save
            plugin.saveConfig();
        }
    }

    private void trust(String[] args, Player player, String base) {
        // Make sure trust perm exists
        if (checkPerms(player, "trust") && hasIsland(player)) {
            // Make sure the player argument exists
            if (args.length < 2) sendMessage(player, "\"player\" argument required");
            else {
                // If to change all the users or just one
                List<String> trusted = config.getStringList(base.concat("trusted"));
                Boolean trust = args[0].toLowerCase().equals("trust");
                if (args[1].toLowerCase().equals("all")) {
                    if (trust) trusted.add("*");
                    else trusted = new ArrayList<>();
                } else {
                    if (!trusted.contains(args[1].toLowerCase()) && trust) trusted.add(args[1].toLowerCase());
                    else if (trusted.contains(args[1].toLowerCase()) && !trust) trusted.remove(args[1].toLowerCase());
                }
                
                // Set changed list
                config.set(base.concat("trusted"), trusted);

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
                // Get base string for simpler use
                String base = "islands.".concat(target.toLowerCase()).concat(".spawn.");

                // Tell player about the teleportation
                sendInfo(player, config.getString("oneblock-visit-msg").replace("%p", target));

                // Teleport player
                player.teleport(new Location(
                    Bukkit.getWorld(config.getString("oneblock-world")),
                    config.getDouble(base.concat("x")),
                    config.getDouble(base.concat("y")),
                    config.getDouble(base.concat("z")),
                    (float) config.getDouble(base.concat("yaw")),
                    (float) config.getDouble(base.concat("pitch")))
                );
            } else sendInfo(player, "Island not found.");
        }
    }

    private Location indexToLocation(int index, double spacing) {
        int x = 0, z = 0, step = 1, taken = 0, turns = 0, i = 0;

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

        return new Location(Bukkit.getWorld(config.getString("oneblock-world")), x * spacing, config.getDouble("oneblock-y"), z * spacing);
    }
}
