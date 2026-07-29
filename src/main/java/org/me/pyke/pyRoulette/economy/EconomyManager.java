package org.me.pyke.pyRoulette.economy;

import org.bukkit.entity.Player;
import org.me.pyke.pyRoulette.PyRoulette;

import java.util.Locale;
import java.util.UUID;

public final class EconomyManager {
    private final PyRoulette plugin;
    private EconomyProvider provider;

    public EconomyManager(PyRoulette plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        String configured = plugin.getConfig().getString("economy.provider", "VAULT").toUpperCase(Locale.ROOT);
        if ("VAULT".equals(configured)) {
            provider = new VaultEconomyProvider(plugin);
            return;
        }

        plugin.getLogger().warning("Unknown economy provider '" + configured + "'. Falling back to Vault.");
        provider = new VaultEconomyProvider(plugin);
    }

    public boolean isAvailable() {
        return provider != null && provider.isAvailable();
    }

    public boolean has(Player player, double amount) {
        return provider != null && provider.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return provider != null && provider.withdraw(player, amount);
    }

    public boolean deposit(Player player, double amount) {
        return provider != null && provider.deposit(player, amount);
    }

    public boolean deposit(UUID playerId, String playerName, double amount) {
        return provider != null && provider.deposit(playerId, playerName, amount);
    }

    public String format(double amount) {
        if (provider == null) {
            return String.format(Locale.US, "%.2f", amount);
        }
        return provider.format(amount);
    }

    public String providerName() {
        return provider == null ? "none" : provider.name();
    }
}
