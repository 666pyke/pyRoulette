package org.me.pyke.pyRoulette.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.me.pyke.pyRoulette.PyRoulette;

import java.util.Locale;
import java.util.UUID;

public final class VaultEconomyProvider implements EconomyProvider {
    private final Economy economy;

    public VaultEconomyProvider(PyRoulette plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            plugin.getLogger().warning("Vault is not installed. Roulette betting will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
        if (economy == null) {
            plugin.getLogger().warning("Vault is installed, but no economy provider is registered.");
        }
    }

    @Override
    public boolean isAvailable() {
        return economy != null;
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return economy != null && economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(UUID playerId, String playerName, double amount) {
        if (economy == null) {
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return economy.depositPlayer(offlinePlayer, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy == null ? String.format(Locale.US, "%.2f", amount) : economy.format(amount);
    }

    @Override
    public String name() {
        return "Vault";
    }
}
