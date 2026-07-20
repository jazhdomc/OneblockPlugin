package mc.jazhdo;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class OneblockPlugin extends JavaPlugin {
    private OneblockCommands commands;
    private BukkitTask saver;

    @Override
    public void onEnable() {
        getLogger().info("OneblockPlugin starting...");

        // Cache config
        saveDefaultConfig();

        // Load up listeners and commands
        OneblockListener listener = new OneblockListener(this);
        commands = new OneblockCommands(this, listener);
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(listener, this);
        manager.registerEvents(new PermListener(this), this);
        getCommand("oneblock").setExecutor(commands);
        
        // Save every 30s
        saver = Bukkit.getScheduler().runTaskTimer(this, this::saveConfig, 0L, 600L);
    }

    @Override
    public void onDisable() {
        getLogger().info("OneblockPlugin shutting down...");
        if (!saver.isCancelled()) saver.cancel();
        saveConfig();
    }

    public OneblockCommands getCommands() {
        return commands;
    }
}