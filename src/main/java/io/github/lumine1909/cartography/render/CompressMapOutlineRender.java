package io.github.lumine1909.cartography.render;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import static io.github.lumine1909.cartography.Cartography.checkSize;
import static io.github.lumine1909.cartography.processor.ImageProcessor.*;
import static io.github.lumine1909.cartography.util.GeometryUtil.getPhase;

public class CompressMapOutlineRender {

    public static void render(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.PAINTING) {
            return;
        }

        var container = item.getPersistentDataContainer();
        if (!container.has(keyIdStart, PersistentDataType.INTEGER)) {
            return;
        }
        int length, width;
        try {
            length = container.get(keyLength, PersistentDataType.INTEGER);
            width = container.get(keyWidth, PersistentDataType.INTEGER);
        } catch (Exception e) {
            return;
        }
        if (!checkSize(length * width, player)) {
            return;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            return;
        }
        BlockFace face = player.getTargetBlockFace(5);
        if (face == null) {
            return;
        }
        face = face.getOppositeFace();
        Location start = block.getLocation();

        switch (face) {
            case NORTH -> {
                start.add(0, 1, 1.1);
                drawRectangleKeepZ(player, start, length, -width);
            }
            case SOUTH -> {
                start.add(1, 1, -0.1);
                drawRectangleKeepZ(player, start, -length, -width);
            }
            case EAST -> {
                start.add(-0.1, 1, 0);
                drawRectangleKeepX(player, start, -width, length);
            }
            case WEST -> {
                start.add(1.1, 1, 1);
                drawRectangleKeepX(player, start, -width, -length);
            }
            case DOWN -> {
                switch (getPhase(player.getLocation().getYaw())) {
                    case 0 -> {
                        start.add(0, 1.1, 0);
                        drawRectangleKeepY(player, start, length, width);
                    }
                    case 1 -> {
                        start.add(1, 1.1, 0);
                        drawRectangleKeepY(player, start, -width, length);
                    }
                    case 2 -> {
                        start.add(1, 1.1, 1);
                        drawRectangleKeepY(player, start, -length, -width);
                    }
                    case 3 -> {
                        start.add(0, 1.1, 1);
                        drawRectangleKeepY(player, start, width, -length);
                    }
                }
            }
            case UP -> {
                switch (getPhase(player.getLocation().getYaw())) {
                    case 0 -> {
                        start.add(0, -0.1, 1);
                        drawRectangleKeepY(player, start, length, -width);
                    }
                    case 1 -> {
                        start.add(0, -0.1, 0);
                        drawRectangleKeepY(player, start, width, length);
                    }
                    case 2 -> {
                        start.add(1, -0.1, 0);
                        drawRectangleKeepY(player, start, -length, width);
                    }
                    case 3 -> {
                        start.add(1, -0.1, 1);
                        drawRectangleKeepY(player, start, -width, -length);
                    }
                }
            }
        }
    }

    private static void drawRectangleKeepY(Player player, Location origin, double dx, double dz) {
        double x0 = origin.x();
        double y = origin.y();
        double z0 = origin.z();
        double x1 = x0 + dx;
        double z1 = z0 + dz;

        drawLine(player, x0, y, z0, x1, y, z0);
        drawLine(player, x1, y, z0, x1, y, z1);
        drawLine(player, x1, y, z1, x0, y, z1);
        drawLine(player, x0, y, z1, x0, y, z0);
    }

    private static void drawRectangleKeepZ(Player player, Location origin, double dx, double dy) {
        double x0 = origin.x();
        double y0 = origin.y();
        double z = origin.z();
        double x1 = x0 + dx;
        double y1 = y0 + dy;

        drawLine(player, x0, y0, z, x1, y0, z);
        drawLine(player, x1, y0, z, x1, y1, z);
        drawLine(player, x1, y1, z, x0, y1, z);
        drawLine(player, x0, y1, z, x0, y0, z);
    }

    private static void drawRectangleKeepX(Player player, Location origin, double dy, double dz) {
        double x = origin.x();
        double y0 = origin.y();
        double z0 = origin.z();
        double y1 = y0 + dy;
        double z1 = z0 + dz;

        drawLine(player, x, y0, z0, x, y1, z0);
        drawLine(player, x, y1, z0, x, y1, z1);
        drawLine(player, x, y1, z1, x, y0, z1);
        drawLine(player, x, y0, z1, x, y0, z0);
    }


    private static void drawLine(Player player, double x0, double y0, double z0, double x1, double y1, double z1) {
        Vector start = new Vector(x0, y0, z0);
        Vector end = new Vector(x1, y1, z1);
        Vector diff = end.clone().subtract(start);

        int steps = (int) Math.max(Math.abs(diff.getX()), Math.max(Math.abs(diff.getY()), Math.abs(diff.getZ())));
        if (steps == 0) steps = 1;
        steps *= 4;

        Vector stepVec = diff.clone().multiply(1.0 / steps);

        Vector point = start.clone();
        for (int i = 0; i <= steps; i++) {
            player.spawnParticle(
                Particle.TRAIL,
                point.getX(), point.getY(), point.getZ(),
                1,
                0, 0, 0,
                new Particle.Trail(new Location(null, point.getX(), point.getY(), point.getZ()), Color.fromRGB(0x66ccff), 10)
            );
            point.add(stepVec);
        }
    }
}