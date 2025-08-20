package io.github.lumine1909.cartography.util;

import org.bukkit.Rotation;

public class GeometryUtil {

    public static int getPhase(double yaw) {
        double normalizedYaw = (yaw + 180 + 45) % 360;
        return (int) (normalizedYaw / 90);
    }

    public static Rotation getRotation(int phase, boolean invert) {
        // N, E, S, W
        return switch (phase) {
            case 0 -> Rotation.NONE;
            case 1 -> invert ? Rotation.CLOCKWISE_135 : Rotation.CLOCKWISE_45;
            case 2 -> Rotation.CLOCKWISE;
            case 3 -> invert ? Rotation.CLOCKWISE_45 : Rotation.CLOCKWISE_135;
            default -> throw new IllegalArgumentException("Invalid phase value: " + phase);
        };
    }
}
