package mc.jazhdo;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class PermListener implements Listener {
    private final FileConfiguration config;

    public PermListener(OneblockPlugin plugin) {
        this.config = plugin.getConfig();
    }

    private void checkPerms(Player player, String perm, Event event) {
        // Find out whose island the player is on
        Location playerLoc = player.getLocation();
        Double playerX = playerLoc.getX(), playerZ = playerLoc.getZ(), islandSpacing = config.getDouble("island-spacing"), islandSpacingHalf = islandSpacing/2;
        for (String island : config.getConfigurationSection("islands").getKeys(false)) {
            String base = "islands.".concat(island).concat(".");
            Double islandX = config.getDouble(base.concat("x")) * islandSpacing, islandZ = config.getDouble(base.concat("z")) * islandSpacing;
            if (playerX > islandX - islandSpacingHalf && playerZ > islandZ - islandSpacingHalf && playerX <= islandX + islandSpacingHalf && playerZ <= islandZ + islandSpacingHalf) {
                if (!player.getName().toLowerCase().equals(island)) {
                    // Check whether or not player is trusted or the trusted all selector is present
                    List<String> trustedList = config.getStringList(base.concat("trusted"));
                    Boolean trusted = trustedList.contains(player.getName().toLowerCase()) || trustedList.contains("*");
                    if ((!trusted && !config.getStringList(base.concat("visitor-perms")).contains(perm)) || (trusted && !config.getStringList(base.concat("trusted-perms")).contains(perm))) cancelEvent(event);
                }
                return;
            }
        }

        // You can't do anything in unowned land
        cancelEvent(event);
    }

    private void cancelEvent(Event event) {
        if (event instanceof Cancellable cancellable && !cancellable.isCancelled()) cancellable.setCancelled(true);
    }

    @EventHandler
    public void inventoryUse(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            switch (event.getInventory().getType()) {
                case InventoryType.ANVIL -> checkPerms(player, "anvil-use", event);
                case InventoryType.BREWING -> checkPerms(player, "shulker-use", event);
                case InventoryType.CHEST -> checkPerms(player, "chest-use", event);
                case InventoryType.CRAFTING -> checkPerms(player, "crafting-use", event);
                case InventoryType.ENCHANTING -> checkPerms(player, "enchant-use", event);
                case InventoryType.FURNACE -> checkPerms(player, "furnace-use", event);
                case InventoryType.SHULKER_BOX -> checkPerms(player, "shulker-use", event);
                default -> {}
            }
        }
    }

    @EventHandler
    public void bedUse(PlayerBedEnterEvent event) {
        checkPerms(event.getPlayer(), "bed-use", event);
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        checkPerms(event.getPlayer(), "block-break", event);
    }

    @EventHandler
    public void blockInteract(PlayerInteractEvent event) {
        // Crop Trample subsection
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
           if (block != null && block.getType() == Material.SOIL) checkPerms(event.getPlayer(), "crop-trample", event);
        } else checkPerms(event.getPlayer(), "block-interact", event);
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        checkPerms(event.getPlayer(), "block-place", event);
    }

    @EventHandler
    public void breeding(EntityBreedEvent event) {
        checkPerms((Player) event.getBreeder(), "breeding", event);
    }

    @EventHandler
    public void bucketEmpty(PlayerBucketEmptyEvent event) {
        checkPerms(event.getPlayer(), "bucket-empty", event);
    }

    @EventHandler
    public void bucketFill(PlayerBucketFillEvent event) {
        checkPerms(event.getPlayer(), "bucket-fill", event);
    }

    @EventHandler
    public void lighterUse(BlockIgniteEvent event) {
        if (event.getIgnitingEntity() instanceof Player player) checkPerms(player, "lighter-use", event);
    }

    @EventHandler
    public void hurt(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (event.getDamager() instanceof Player player) {
            // Hurt animals
            if (entity instanceof Animals) checkPerms(player, "hurt-animals", event);

            // Hurt hostile
            if (entity instanceof Monster) checkPerms(player, "hurt-hostile", event);

            // Pvp
            if (entity instanceof Player) checkPerms(player, "pvp", event);
        } else if (event.getDamager() instanceof LivingEntity && entity instanceof Player player) checkPerms(player, "mob-damage", event);
    }

    @EventHandler
    public void itemDrop(PlayerDropItemEvent event) {
        checkPerms(event.getPlayer(), "item-drop", event);
    }

    @EventHandler
    public void itemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) checkPerms(player, "item-pickup", event);
    }

    @EventHandler
    public void teleportation(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) checkPerms(event.getPlayer(), "pearl-use", event);
        else if (cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) checkPerms(event.getPlayer(), "portal-use", event);
    }

    @EventHandler
    public void playerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlock() != event.getTo().getBlock()) checkPerms(event.getPlayer(), "player-move", event);
    }

    @EventHandler
    public void taming(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) checkPerms(player, "taming", event);
    }

    @EventHandler
    public void vehicleUse(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) checkPerms(player, "vehicle-use", event);
    }
}
