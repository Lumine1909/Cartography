package io.github.lumine1909.cartography;

import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class NetImage extends BukkitRunnable {
    public static String serverVersion;
    int length, width;
    URL url;
    Player player;
    public NetImage(String url, Player player, int mapLength, int mapWidth) throws IOException {
        serverVersion = Cartography.server.getClass().getPackageName();
        this.url = new URL(url);
        this.player = player;
        this.length = mapLength;
        this.width = mapWidth;
    }
    private WorldMap createMap(World world) {
        try {
            Class<?> craftWorld = Class.forName(serverVersion + ".CraftWorld");
            Field f = craftWorld.getDeclaredField("world");
            f.setAccessible(true);
            Object result = f.get(world);
            net.minecraft.world.level.World minecraftWorld = (net.minecraft.world.level.World) result;
            int newId = ItemWorldMap.a(minecraftWorld, minecraftWorld.n_().a(), minecraftWorld.n_().c(), 3, false, false, minecraftWorld.ab());
            return minecraftWorld.a(ItemWorldMap.a(newId));
        } catch (Exception e) {
            return null;
        }
    }
    public void genSingleImage(Player player, BufferedImage image, int i, int j) {
        try {
            WorldMap map = createMap(player.getWorld());
            map.mapView.setLocked(true);
            byte[] bytes = MapPalette.imageToBytes(image);
            for(int x = 0; x < image.getWidth(null); ++x) {
                for(int y = 0; y < image.getHeight(null); ++y) {
                    byte color = bytes[y*image.getWidth(null)+x];
                    map.a(x, y, color);
                }
            }
            ItemStack is = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) is.getItemMeta();
            meta.setMapView(map.mapView);
            Component lore = Component.text(ChatColor.GREEN + "pos: " + i + "," + j);
            meta.lore(List.of(lore));
            is.setItemMeta(meta);
            player.getInventory().addItem(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage toBuffer(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        image = new ImageIcon(image).getImage();
        BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bimage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }
    @Override
    public void run() {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(Cartography.maxTimeout);
            InputStream inputStream = connection.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            Image img = image.getScaledInstance(128*length, 128*width, Image.SCALE_AREA_AVERAGING);

            image = toBuffer(img);
            for (int i=0; i<width; i++) {
                for (int j=0; j<length; j++) {
                    BufferedImage tempImg = image.getSubimage(j*128, i*128, 128, 128);
                    genSingleImage(player, tempImg, i, j);
                }
            }
            player.sendMessage(ChatColor.AQUA + "地图画生成完成!" + "总共 " + length*width + " 张");
            Cartography.mapCoolDown.put(player, System.currentTimeMillis());
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "URL错误或无法访问!");
        }
    }
}
