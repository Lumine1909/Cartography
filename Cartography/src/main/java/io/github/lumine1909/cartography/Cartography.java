package io.github.lumine1909.cartography;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public final class Cartography extends JavaPlugin {
    public static Map<Player, Long> mapCoolDown = new HashMap<>();
    public static Server server;
    public static int maxLength;
    public static int maxWidth;
    public static int coolDown;
    public static int maxTimeout;
    public static Cartography instance;
    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        maxLength = getConfig().getInt("max-length", 4);
        maxWidth = getConfig().getInt("max-width", 4);
        coolDown = getConfig().getInt("cool-down", 30000);
        maxTimeout = getConfig().getInt("max-timeout", 10000);
        instance = this;
        server = this.getServer();
        getLogger().info("插件加载完成, 作者: Lumine1909");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家才能使用该命令");
            return true;
        }
        if (!player.hasPermission("cartography.getimage")) {
            sender.sendMessage(ChatColor.RED + "你没有权限!");
            return true;
        }
        if (args.length < 1 || args.length > 3) {
            sender.sendMessage(ChatColor.RED + "命令语法不正确!");
            sender.sendMessage(ChatColor.RED + "请使用/getimage <URL> [Height] [Width]");
            return true;
        }
        try {
            int length, width;
            if (args.length == 1) {
                length = 1;
                width = 1;
            } else {
                length = Integer.parseInt(args[1]);
                width = Integer.parseInt(args[2]);
            }
            if (length > maxLength && !player.hasPermission("cartography.bypass")) {
                player.sendMessage(ChatColor.RED + "图片长度过大, 最大为" + maxLength);
                return true;
            }
            if (length > maxWidth && !player.hasPermission("cartography.bypass")) {
                player.sendMessage(ChatColor.RED + "图片宽度过大, 最大为" + maxWidth);
                return true;
            }
            if (mapCoolDown.containsKey(player) && !player.hasPermission("cartography.bypass")) {
                if (System.currentTimeMillis() - mapCoolDown.get(player) < coolDown) {
                    player.sendMessage(ChatColor.RED + "生成冷却时间剩余: " + (coolDown-System.currentTimeMillis()+mapCoolDown.get(player))/1000 + "秒!");
                    return true;
                }
            }
            new NetImage(args[0], player, length, width).runTaskAsynchronously(instance);
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "URL错误或无法访问!");
            return true;
        }
        return true;
    }
}
