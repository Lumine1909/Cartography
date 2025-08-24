package io.github.lumine1909.cartography.command;

import io.github.lumine1909.cartography.Cartography;
import io.github.lumine1909.cartography.processor.ImageProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.github.lumine1909.cartography.Cartography.*;

public class GetImageCommand implements TabExecutor {

    public GetImageCommand() {
        Objects.requireNonNull(plugin.getCommand("getimage")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("getimage")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("cartography.getimage")) {
            return true;
        }
        try {
            Argument argument = Argument.fromCommandArgs(args);
            if (!checkSize(argument.length() * argument.width(), player)) {
                player.sendMessage(ChatColor.RED + "地图画过大, 最大限制总张数为: " + Cartography.MAX_SIZE);
                return true;
            }
            if (!player.hasPermission("cartography.bypasscd") && mapCoolDown.containsKey(player.getUniqueId()) && mapCoolDown.get(player.getUniqueId()) > System.currentTimeMillis()) {
                player.sendMessage(ChatColor.RED + "地图画生成还在冷却时间");
                return true;
            }
            player.sendMessage(ChatColor.AQUA + "开始生成: " + argument.url());
            mapCoolDown.put(player.getUniqueId(), System.currentTimeMillis() + Cartography.GENERATE_CD);
            ImageProcessor.process(argument.url(), player, argument);
        } catch (Exception e) {
            player.sendMessage(Component.text("命令格式错误", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        try {
            return Argument.getCompletion(args);
        } catch (Exception e) {
            sender.sendMessage(Component.text("命令格式错误", NamedTextColor.RED));
        }
        return Collections.emptyList();
    }
}
