package io.github.lumine1909.cartography;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.List;

public class NetImage extends BukkitRunnable {
    int length, width;
    URL url;
    Player player;
    public NetImage(String url, Player player, int mapLength, int mapWidth) throws IOException {
        this.url = new URL(url);
        this.player = player;
        this.length = mapLength;
        this.width = mapWidth;
    }
    public void genSingleImage(Player player, BufferedImage image, int i, int j) {
        try {
            MapView mapView = Bukkit.createMap(player.getWorld());
            mapView.addRenderer(new MapRenderer() {
                @Override
                public void render(@NotNull MapView mapView, @NotNull MapCanvas mapCanvas, @NotNull Player player) {
                    mapCanvas.drawImage(0, 0, image);
                }
            });
            ItemStack is = new ItemStack(Material.FILLED_MAP);
            mapView.setScale(MapView.Scale.CLOSEST);
            MapMeta meta = (MapMeta) is.getItemMeta();
            meta.setMapView(mapView);
            Component lore = Component.text(ChatColor.WHITE + "pos: " + i + "," + j);
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
