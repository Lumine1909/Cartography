package io.github.lumine1909.cartography;

import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.lumine1909.cartography.Cartography.*;

public class NetImage extends BukkitRunnable {
    int length, width;
    URL url;
    Player player;
    List<ItemStack> stackList = new ArrayList<>();
    String method;
    public NetImage(String url, Player player, int mapLength, int mapWidth, String method) throws IOException {
        this.url = new URL(url);
        this.player = player;
        this.length = mapLength;
        this.width = mapWidth;
        this.method = method;
    }

    public void genSingleImage(Player player, BufferedImage image, int i, int j) {
        MapView view = server.createMap(player.getWorld());
        view.setScale(MapView.Scale.CLOSEST);
        try {
            Class<?> craftMapView = Class.forName(serverVersion + ".map.CraftMapView");
            Method method = craftMapView.getMethod("setLocked", boolean.class);
            method.invoke(view, true);
        } catch (Exception ignored) {
        }
        if (version >= 17) {
            try {
                Class<?> craftMapView = Class.forName(serverVersion + ".map.CraftMapView");
                Field mapField = craftMapView.getDeclaredField("worldMap");
                mapField.setAccessible(true);
                WorldMap worldMap = (WorldMap) mapField.get(view);
                byte[] bytes = MapPalette.imageToBytes(image);
                for (int x = 0; x < 128; ++x) {
                    for (int y = 0; y < 128; ++y) {
                        byte color = bytes[x+y*128];
                        worldMap.a(x, y, color);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                String var0 = serverVersion.split("\\.")[3];
                Class<?> craftMapView = Class.forName(serverVersion + ".map.CraftMapView");
                Field mapField = craftMapView.getDeclaredField("worldMap");
                mapField.setAccessible(true);
                Object worldMap = mapField.get(view);
                byte[] bytes = MapPalette.imageToBytes(image);
                Class<?> nmsWorldMap = Class.forName("net.minecraft.server." + var0 + ".WorldMap");
                Field colors = nmsWorldMap.getDeclaredField("colors");
                colors.setAccessible(true);
                byte[] temp = (byte[]) colors.get(worldMap);
                Method flagDirty = nmsWorldMap.getMethod("flagDirty", int.class, int.class);
                for (int x = 0; x < 128; ++x) {
                    for (int y = 0; y < 128; ++y) {
                        temp[x + 128 * y] = bytes[x + 128 * y];
                        flagDirty.invoke(worldMap, x, y);
                    }
                }
                colors.set(worldMap, temp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ItemStack is = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) is.getItemMeta();
        meta.setMapView(view);
        List<String> var1 = new ArrayList<>();
        var1.add(ChatColor.GREEN + "pos: " + i + "," + j);
        meta.setLore(var1);
        is.setItemMeta(meta);
        stackList.add(is);
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
            new Packager(player, stackList, method);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + translation.getString("url-error", "URL error or inaccessible!"));
        }
    }
}