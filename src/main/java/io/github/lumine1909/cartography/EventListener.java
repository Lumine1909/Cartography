package io.github.lumine1909.cartography;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static io.github.lumine1909.cartography.Cartography.checkSize;
import static io.github.lumine1909.cartography.processor.ImageProcessor.*;
import static io.github.lumine1909.cartography.util.GeometryUtil.getPhase;
import static io.github.lumine1909.cartography.util.GeometryUtil.getRotation;

public class EventListener implements Listener {

    private static void modifyRotation(double yaw, ItemFrame frame) {
        if (frame.getFacing().equals(BlockFace.UP)) {
            frame.setRotation(getRotation(getPhase(yaw), false));
        } else if (frame.getFacing().equals(BlockFace.DOWN)) {
            frame.setRotation(getRotation(getPhase(yaw), true));
        }
    }

    @EventHandler
    public void correctMapRotation(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame frame) {
            Player player = e.getPlayer();
            Material main = player.getInventory().getItemInMainHand().getType();
            Material off = player.getInventory().getItemInOffHand().getType();
            if (!(main.equals(Material.FILLED_MAP) || (off.equals(Material.FILLED_MAP) && main.equals(Material.AIR)))) {
                return;
            }
            if (!frame.getItem().getType().equals(Material.AIR)) {
                return;
            }
            modifyRotation(player.getLocation().getYaw(), frame);
        }
    }

    @EventHandler
    public void fillCompressedMap(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.PAINTING) {
            return;
        }

        var container = item.getPersistentDataContainer();
        if (!container.has(keyIdStart, PersistentDataType.INTEGER)) {
            return;
        }
        int idStart, length, width;
        try {
            idStart = container.get(keyIdStart, PersistentDataType.INTEGER);
            length = container.get(keyLength, PersistentDataType.INTEGER);
            width = container.get(keyWidth, PersistentDataType.INTEGER);
        } catch (Exception e) {
            return;
        }
        if (!checkSize(length * width, player)) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        BlockFace face = event.getBlockFace().getOppositeFace();
        World world = clicked.getWorld();

        event.setCancelled(true);

        Location base = clicked.getLocation().add(0.5, 0.5, 0.5);
        int index = 0;
        for (int w = 0; w < width; w++) {
            for (int l = 0; l < length; l++) {
                int mapId = idStart + index++;
                ItemStack map = new ItemStack(Material.FILLED_MAP);
                map.setData(DataComponentTypes.MAP_ID, MapId.mapId(mapId));

                Location spawnLoc = base.clone();

                switch (face) {
                    case NORTH -> spawnLoc.add(l, -w, 1);
                    case SOUTH -> spawnLoc.add(-l, -w, -1);
                    case WEST -> spawnLoc.add(1, -w, -l);
                    case EAST -> spawnLoc.add(-1, -w, l);
                    case DOWN -> {
                        int phase = getPhase(player.getLocation().getYaw());
                        switch (phase) {
                            case 0 -> spawnLoc.add(l, 1, w);
                            case 1 -> spawnLoc.add(-w, 1, l);
                            case 2 -> spawnLoc.add(-l, 1, -w);
                            case 3 -> spawnLoc.add(w, 1, -l);
                        }
                    }
                    case UP -> {
                        int phase = getPhase(player.getLocation().getYaw());
                        switch (phase) {
                            case 0 -> spawnLoc.add(l, -1, -w);
                            case 1 -> spawnLoc.add(-w, -1, -l);
                            case 2 -> spawnLoc.add(-l, -1, w);
                            case 3 -> spawnLoc.add(w, -1, l);
                        }
                    }
                }

                if (spawnLoc.getBlock().getType() != Material.AIR || !spawnLoc.clone().add(face.getDirection().multiply(0.5)).getNearbyEntitiesByType(ItemFrame.class, 0.2).isEmpty()) {
                    continue;
                }
                if (!new PlayerInteractEvent(player, Action.PHYSICAL, item, spawnLoc.getBlock(), face).callEvent()) {
                    continue;
                }

                ItemFrame frame = world.spawn(spawnLoc, GlowItemFrame.class);
                frame.setItem(map);
                frame.setFacingDirection(face.getOppositeFace(), true);
                frame.setVisible(false);
                frame.setFixed(true);
                modifyRotation(player.getLocation().getYaw(), frame);
            }
        }
        player.swingHand(EquipmentSlot.HAND);
    }
}
