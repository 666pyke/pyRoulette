package org.me.pyke.pyRoulette.economy;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface EconomyProvider {
    boolean isAvailable();

    boolean has(Player player, double amount);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);

    boolean deposit(UUID playerId, String playerName, double amount);

    String format(double amount);

    String name();
}
