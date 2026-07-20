package mc.jazhdo;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OneblockPlugin extends JavaPlugin {
    private OneblockCommands commands;

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
    }

    @Override
    public void onDisable() {
        getLogger().info("OneblockPlugin shutting down...");
    }

    public OneblockCommands getCommands() {
        return commands;
    }
}