package mc.jazhdo;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TpRequest {
    private final String from;
    private final String to;
    private BukkitTask task;

    public TpRequest(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public boolean ifAccept(String from2, String to2) {
        return from2.equalsIgnoreCase(from) && to2.equalsIgnoreCase(to);
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }

    /**
     * Returns the player from the tp request
     * 
     * @return A list in the format: [from, to]
     */
    public String[] getPlayers() {
        return new String[] {from, to};
    }

    public boolean tpPlayer() {
        task.cancel();
        Player fromPlayer = Bukkit.getPlayer(from);
        if (fromPlayer == null) return false;
        fromPlayer.teleport((Entity) Bukkit.getPlayer(to));
        return true;
    }
}