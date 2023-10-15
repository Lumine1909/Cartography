package io.github.lumine1909.cartography;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Cartography extends JavaPlugin {
    public static Map<Player, Long> mapCoolDown = new HashMap<>();


    public static Cartography instance;
    public static YamlConfiguration translation;
    public static Server server;
    public static int maxLength;
    public static int maxWidth;
    public static int coolDown;
    public static int maxTimeout;
    public static String serverVersion;
    public static boolean isLogging;

    public static int version;
    public static boolean fixMap;
    public static YamlConfiguration logConfig;
    public static File logFile;
    @Override
    public void onLoad() {
        instance = this;
        server = this.getServer();
        saveResource("translation.yml", false);
        saveResource("translation_zh_CN.yml", false);
        translation = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "translation.yml"));
        serverVersion = Cartography.server.getClass().getPackage().getName();
        version = Integer.parseInt(serverVersion.split("\\.")[3].split("_")[1]);
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        if (version < 13) {
            getLogger().warning(translation.getString("unsupported", "Unsupported version! This plugin is at least available for version 1.13"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        server.getPluginManager().registerEvents(new EventListener(), this);
        maxLength = getConfig().getInt("max-length", 4);
        maxWidth = getConfig().getInt("max-width", 4);
        coolDown = getConfig().getInt("cool-down", 30000);
        maxTimeout = getConfig().getInt("max-timeout", 10000);
        maxWidth = getConfig().getInt("max-width", 4);
        fixMap = getConfig().getBoolean("map-frame-fix", true);
        isLogging = getConfig().getBoolean("enable-log", true);
        if (isLogging) {
            logFile = new File(getDataFolder(), "map.log");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException ignored) {
                }
            }
            logConfig = YamlConfiguration.loadConfiguration(logFile);
        }
        getLogger().info(translation.getString("done", "Plugin loading completed, author: Lumine1909"));
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + translation.getString("player-check", "Only players can use this command!"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("cartography.getimage")) {
            sender.sendMessage(ChatColor.RED + translation.getString("no-permission", "You do not have permission to execute this command!"));
            return true;
        }
        if (args.length < 2 || args.length > 4) {
            sender.sendMessage(ChatColor.RED + translation.getString("incorrect-usage", "Incorrect command usage!"));
            sender.sendMessage(ChatColor.RED + translation.getString("usage", "Please use /getimage <URL> [Height] [Width]"));
            return true;
        }
        try {
            int length, width;
            if (args.length == 2) {
                length = 1;
                width = 1;
            } else {
                length = Integer.parseInt(args[2]);
                width = Integer.parseInt(args[3]);
            }
            if (length > maxLength && !player.hasPermission("cartography.bypass")) {
                player.sendMessage(ChatColor.RED + translation.getString("length-too-large", "The length of the image is too large, with a maximum of ") + maxLength);
                return true;
            }
            if (width > maxWidth && !player.hasPermission("cartography.bypass")) {
                player.sendMessage(ChatColor.RED + translation.getString("width-too-large", "The length of the image is too large, with a maximum of ") + maxWidth);
                return true;
            }
            if (mapCoolDown.containsKey(player) && !player.hasPermission("cartography.bypass")) {
                if (System.currentTimeMillis() - mapCoolDown.get(player) < coolDown) {
                    player.sendMessage(ChatColor.RED + translation.getString("cool-down", "Remaining cooling time for generation: ") + (coolDown-System.currentTimeMillis()+mapCoolDown.get(player))/1000 + "s!");
                    return true;
                }
            }
            new NetImage(args[1], player, length, width, args[0]).runTaskAsynchronously(instance);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + translation.getString("url-error", "URL error or inaccessible!"));
            return true;
        }
        return true;
    }
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 1) {
            List<String> var = new ArrayList<>();
            var.add("normal");
            if (sender.hasPermission("cartography.shulker")) var.add("shulker");
            //if (sender.hasPermission("cartography.compress")) var.add("compress");
            return var;
        }
        return null;
    }
}
