package io.github.lumine1909.cartography.processor;

import io.github.lumine1909.cartography.command.Argument;
import io.github.lumine1909.cartography.command.CommandContext;
import io.github.lumine1909.cartography.util.ImageUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.MapId;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.craftbukkit.map.CraftMapView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.github.lumine1909.cartography.Cartography.LOG_FILE;
import static io.github.lumine1909.cartography.Cartography.plugin;

@SuppressWarnings("removal")
public class ImageProcessor {

    private static final int TILE_SIZE = 128;

    public static final NamespacedKey keyIdStart = new NamespacedKey("cartography", "id_start");
    public static final NamespacedKey keyLength = new NamespacedKey("cartography", "length");
    public static final NamespacedKey keyWidth = new NamespacedKey("cartography", "width");

    private static final Processor<int[], Void> ITEM_PIPELINE = convertToItems()
        .then(giveToPlayer());

    private static final Processor<String, int[]> MAP_PIPELINE = fetchImage()
        .then(rescale())
        .then(applyDitheringIfEnabled())
        .then(convertToMap());

    private static final Processor<String, Void> CREATE_PIPELINE = MAP_PIPELINE.then(ITEM_PIPELINE);

    private static final Processor<Argument, Void> LOAD_PIPELINE = loadOrFetch().then(ITEM_PIPELINE);

    private static final Field field$worldMap;

    static {
        try {
            field$worldMap = CraftMapView.class.getDeclaredField("worldMap");
            field$worldMap.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void process(String url, Player player, Argument argument) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (argument.noCache()) {
                CREATE_PIPELINE.process(url, new CommandContext(player, argument));
            } else {
                LOAD_PIPELINE.process(argument, new CommandContext(player, argument));
            }
        });
    }

    private static Processor<String, BufferedImage> fetchImage() {
        return (url, context) -> {
            try {
                return ImageUtil.fetchImage(url);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch image: " + url, e);
            }
        };
    }

    private static Processor<Argument, int[]> loadOrFetch() {
        return (argument, context) -> {
            int[] range = plugin.storage.loadRange(argument);
            if (range == null) {
                return MAP_PIPELINE.process(argument.url(), context);
            }
            return range;
        };
    }

    private static Processor<BufferedImage, BufferedImage> rescale() {
        return (image, context) -> {
            boolean reshape = context.argument().keepScale();
            int w = context.argument().length() * 128, h = context.argument().width() * 128;
            return reshape ? ImageUtil.rescaleKeepingRatio(image, w, h) : ImageUtil.resizeExact(image, w, h);
        };
    }

    private static Processor<BufferedImage, BufferedImage> applyDitheringIfEnabled() {
        return (image, context) -> context.argument().useDithering() ? addDithering(image) : image;
    }

    private static Processor<BufferedImage, int[]> convertToMap() {
        return (image, context) -> {
            int start = -1, end = 0;
            for (int row = 0; row < context.argument().width(); row++) {
                for (int col = 0; col < context.argument().length(); col++) {
                    int x = col * TILE_SIZE;
                    int y = row * TILE_SIZE;
                    if (x + TILE_SIZE <= image.getWidth() && y + TILE_SIZE <= image.getHeight()) {
                        BufferedImage tile = image.getSubimage(x, y, TILE_SIZE, TILE_SIZE);
                        end = imageToMap(tile, context);
                        if (start == -1) {
                            start = end;
                        }
                    }
                }
            }
            plugin.storage.saveRange(context.argument(), start, end);
            return new int[]{start, end};
        };
    }

    private static Processor<int[], List<ItemStack>> convertToItems() {
        return (ids, context) -> {
            int begin = ids[0];
            List<ItemStack> items = new ArrayList<>();
            for (int row = 0; row < context.argument().width(); row++) {
                for (int col = 0; col < context.argument().length(); col++) {
                    items.add(mapToItem(begin++, row, col, context));
                }
            }
            return items;
        };
    }

    private static Processor<List<ItemStack>, Void> giveToPlayer() {
        return (items, context) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                switch (context.argument().pack()) {
                    case NORMAL -> context.player().getInventory().addItem(items.toArray(ItemStack[]::new));
                    case SHULKER -> {
                        int idStart = items.getFirst().getData(DataComponentTypes.MAP_ID).id();
                        int idEnd = items.getLast().getData(DataComponentTypes.MAP_ID).id();
                        int counter = 1, total = (int) Math.ceil(items.size() / 27.0);
                        List<ItemStack> currentBatch = new ArrayList<>();
                        for (ItemStack item : items) {
                            currentBatch.add(item.clone());
                            if (currentBatch.size() == 27) {
                                context.player().getInventory().addItem(createShulkerWithItems(currentBatch, counter++, total, idStart, idEnd));
                                currentBatch.clear();
                            }
                        }
                        context.player().getInventory().addItem(createShulkerWithItems(currentBatch, counter++, total, idStart, idEnd));
                    }
                    case COMPRESS -> {
                        int idStart = items.getFirst().getData(DataComponentTypes.MAP_ID).id();
                        int idEnd = items.getLast().getData(DataComponentTypes.MAP_ID).id();
                        ItemStack compressed = new ItemStack(Material.PAINTING);
                        compressed.editMeta(itemMeta -> {
                            itemMeta.displayName(Component.text("地图画: %d-%d (%dx%d)".formatted(idStart, idEnd, context.argument().length(), context.argument().width()), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                            var container = itemMeta.getPersistentDataContainer();
                            container.set(keyIdStart, PersistentDataType.INTEGER, idStart);
                            container.set(keyLength, PersistentDataType.INTEGER, context.argument().length());
                            container.set(keyWidth, PersistentDataType.INTEGER, context.argument().width());
                        });
                        context.player().getInventory().addItem(compressed);
                    }
                }
                context.player().sendMessage(ChatColor.AQUA + "生成完成! 总共 %d 张".formatted(context.argument().length() * context.argument().width()));
            });
            return null;
        };
    }

    private static ItemStack createShulkerWithItems(List<ItemStack> items, int count, int total, int idStart, int idEnd) {
        ItemStack shulker = new ItemStack(Material.WHITE_SHULKER_BOX);
        shulker.editMeta(itemMeta -> {
            itemMeta.displayName(Component.text("地图画: %d-%d (%d/%d)".formatted(idStart, idEnd, count, total, NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
            BlockStateMeta meta = (BlockStateMeta) itemMeta;
            ShulkerBox box = (ShulkerBox) meta.getBlockState();
            Inventory boxInv = box.getInventory();
            boxInv.addItem(items.toArray(ItemStack[]::new));
            meta.setBlockState(box);
        });
        return shulker;
    }

    private static BufferedImage addDithering(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                Color oldColor = new Color(pixels[index], true);

                if (oldColor.getAlpha() == 0) continue;

                byte mcIndex = MapPalette.matchColor(oldColor);
                Color newColor = MapPalette.getColor(mcIndex);

                newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), oldColor.getAlpha());
                pixels[index] = newColor.getRGB();

                int errR = oldColor.getRed() - newColor.getRed();
                int errG = oldColor.getGreen() - newColor.getGreen();
                int errB = oldColor.getBlue() - newColor.getBlue();

                distributeError(pixels, w, h, x + 1, y, errR, errG, errB, 7.0 / 16);
                distributeError(pixels, w, h, x - 1, y + 1, errR, errG, errB, 3.0 / 16);
                distributeError(pixels, w, h, x, y + 1, errR, errG, errB, 5.0 / 16);
                distributeError(pixels, w, h, x + 1, y + 1, errR, errG, errB, 1.0 / 16);
            }
        }

        img.setRGB(0, 0, w, h, pixels, 0, w);
        return img;
    }

    private static void distributeError(int[] pixels, int l, int h, int x, int y, int errR, int errG, int errB, double factor) {
        if (x < 0 || x >= l || y < 0 || y >= h) return;
        int idx = y * l + x;
        Color c = new Color(pixels[idx], true);

        if (c.getAlpha() == 0) return;

        int r = clamp(c.getRed() + Math.round(errR * (float) factor));
        int g = clamp(c.getGreen() + Math.round(errG * (float) factor));
        int b = clamp(c.getBlue() + Math.round(errB * (float) factor));

        pixels[idx] = new Color(r, g, b, c.getAlpha()).getRGB();
    }

    private static int clamp(int v) {
        return Math.clamp(v, 0, 255);
    }

    private static int imageToMap(BufferedImage img, CommandContext context) {
        MapView view = Bukkit.createMap(context.player().getWorld());
        view.setScale(MapView.Scale.CLOSEST);
        view.setLocked(true);

        MapItemSavedData nmsMap;
        try {
            nmsMap = (MapItemSavedData) field$worldMap.get(view);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                nmsMap.setColor(x, y, MapPalette.matchColor(new Color(img.getRGB(x, y), true)));
            }
        }

        try {
            Files.writeString(
                LOG_FILE.toPath(),
                view.getId() + ": " + context.player().getName() + " " + LocalDateTime.now() + " " + context.argument().url() + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return view.getId();
    }

    private static ItemStack mapToItem(int id, int row, int col, CommandContext context) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        mapItem.setData(DataComponentTypes.LORE,
            ItemLore.lore(List.of(Component.text("位置: " + row + ", " + col, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)))
        );
        mapItem.setData(DataComponentTypes.MAP_ID, MapId.mapId(id));
        return mapItem;
    }
}
