package io.github.lumine1909.cartography;

import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EventListener implements Listener {
    @EventHandler
    public void fixMapFrame(PlayerInteractEntityEvent e) {
        if (!Cartography.fixMap) {
            return;
        }
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) entity;
            Player player = e.getPlayer();
            Material isMain = player.getInventory().getItemInMainHand().getType();
            Material isOff = player.getInventory().getItemInOffHand().getType();
            if (!(isMain.equals(Material.FILLED_MAP) || (isOff.equals(Material.FILLED_MAP) && isMain.equals(Material.AIR)))) {
                return;
            }
            if (!frame.getItem().getType().equals(Material.AIR)) {
                return;
            }
            if (!(frame.getFacing().equals(BlockFace.UP) || frame.getFacing().equals(BlockFace.DOWN))) {
                return;
            }
            double yaw = player.getLocation().getYaw();
            if (yaw >= -135 && yaw < -45) frame.setRotation(Rotation.CLOCKWISE_45);
            else if (yaw >= -45 && yaw < 45) frame.setRotation(Rotation.CLOCKWISE);
            else if (yaw >= 45 && yaw < 135) frame.setRotation(Rotation.CLOCKWISE_135);
            else frame.setRotation(Rotation.NONE);
        }
    }
}
