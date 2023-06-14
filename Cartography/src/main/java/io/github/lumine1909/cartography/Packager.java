package io.github.lumine1909.cartography;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

import static io.github.lumine1909.cartography.Cartography.translation;

public class Packager {
    private final Player player;
    private final List<ItemStack> stackList;
    public Packager(Player player, List<ItemStack> stackList, String arg) {
        this.player = player;
        this.stackList = stackList;
        switch (arg) {
            case "normal" : normalPackage();break;
            case "shulker" : shulkerPackage();break;
            case "compress" : compressmapPackage();break;
        }
    }
    public void normalPackage() {
        for (ItemStack is : stackList) {
            player.getInventory().addItem(is);
        }
        player.sendMessage(ChatColor.AQUA + translation.getString("gen-finish", "Map generation completed! %total% in total").replaceAll("%total%", String.valueOf(stackList.size())));
        Cartography.mapCoolDown.put(player, System.currentTimeMillis());
    }
    public void shulkerPackage() {
        try {
            int cnt = (int) Math.ceil(stackList.size()/27.0);
            List<ShulkerBox> boxList = new ArrayList<>();
            for (int i=0; i<cnt; i++) {
                ItemStack is = new ItemStack(Material.WHITE_SHULKER_BOX);
                BlockStateMeta meta = (BlockStateMeta) is.getItemMeta();
                ShulkerBox box = (ShulkerBox) meta.getBlockState();
                boxList.add(box);
            }
            for (int i=0; i < stackList.size(); i++) {
                boxList.get(i/27).getInventory().setItem(i%27, stackList.get(i));
            }
            for (int i=0; i<cnt; i++) {
                List<String> var0 = new ArrayList<>();
                var0.add(ChatColor.GREEN + "number: " + (i+1));
                ItemStack is = new ItemStack(Material.WHITE_SHULKER_BOX);
                BlockStateMeta meta = (BlockStateMeta) is.getItemMeta();
                meta.setLore(var0);
                meta.setBlockState(boxList.get(i));
                is.setItemMeta(meta);
                player.getInventory().addItem(is);
            }
            player.sendMessage(ChatColor.AQUA + translation.getString("gen-finish", "Map generation completed! %total% in total").replaceAll("%total%", String.valueOf(stackList.size())));
            Cartography.mapCoolDown.put(player, System.currentTimeMillis());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void compressmapPackage() {

    }
}
