package io.github.lumine1909.cartography;

import io.github.lumine1909.cartography.command.GetImageCommand;
import io.github.lumine1909.cartography.command.GetMapCommand;
import io.github.lumine1909.cartography.render.CompressMapOutlineRender;
import io.github.lumine1909.cartography.storage.SqliteStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class Cartography extends JavaPlugin {

    public static Cartography plugin;
    public static Map<UUID, Long> mapCoolDown = new HashMap<>();
    public static File LOG_FILE;
    public static int MAX_SIZE, GENERATE_CD;

    public SqliteStorage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LOG_FILE = new File(getDataFolder(), "map.log");
        if (!LOG_FILE.exists()) {
            try {
                LOG_FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        plugin = this;
        MAX_SIZE = getConfig().getInt("max-size", 9);
        GENERATE_CD = getConfig().getInt("cool-down", 30000);
        new GetImageCommand();
        new GetMapCommand();
        storage = new SqliteStorage(new File(getDataFolder(), "storage.db").getPath());
        Bukkit.getPluginManager().registerEvents(new EventListener(), this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                CompressMapOutlineRender.render(player);
            }
        }, 1, 10);
    }

    @Override
    public void onDisable() {
        storage.close();
    }
}