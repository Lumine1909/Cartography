package io.github.lumine1909.cartography.command;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.github.lumine1909.cartography.Cartography.plugin;

public class GetMapCommand implements TabExecutor {

    public GetMapCommand() {
        Objects.requireNonNull(plugin.getCommand("getmap")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("getmap")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("cartography.getmap") || args.length != 1) {
            return true;
        }
        try {
            int id = Integer.parseInt(args[0]);
            ItemStack map = new ItemStack(Material.FILLED_MAP);
            map.setData(DataComponentTypes.MAP_ID, MapId.mapId(id));
            player.getInventory().addItem(map);
            player.sendMessage(ChatColor.GREEN + "已获得地图物品: " + id);
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
