package io.github.lumine1909.cartography.util;

import java.util.Arrays;

public enum PackMode {
    NORMAL,
    SHULKER,
    COMPRESS;

    public static final String[] values = Arrays.stream(values()).map(mode -> mode.name().toLowerCase()).toList().toArray(String[]::new);

    public static PackMode get(String str) {
        return PackMode.valueOf(str.toUpperCase());
    }
}
