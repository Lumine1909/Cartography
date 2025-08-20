package io.github.lumine1909.cartography.command;

import org.bukkit.entity.Player;

public record CommandContext(Player player, Argument argument) {

}
