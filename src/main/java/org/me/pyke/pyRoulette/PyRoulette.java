package org.me.pyke.pyRoulette;

import org.me.pyke.pyRoulette.command.PyRouletteCommand;
import org.me.pyke.pyRoulette.config.ConfigValidator;
import org.me.pyke.pyRoulette.economy.EconomyManager;
import org.me.pyke.pyRoulette.gui.RouletteMenuManager;
import org.me.pyke.pyRoulette.logging.AuditLogger;
import org.me.pyke.pyRoulette.metrics.MetricsManager;
import org.me.pyke.pyRoulette.roulette.RouletteManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PyRoulette extends JavaPlugin {
    private EconomyManager economyManager;
    private RouletteManager rouletteManager;
    private RouletteMenuManager menuManager;
    private AuditLogger auditLogger;
    private MetricsManager metricsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            ConfigValidator.validate(this);
        } catch (IllegalStateException exception) {
            getLogger().severe(exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand pluginCommand = getCommand("pyroulette");
        if (pluginCommand == null) {
            getLogger().severe("Command 'pyroulette' is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auditLogger = new AuditLogger(this);
        metricsManager = new MetricsManager(this);
        metricsManager.reload();

        economyManager = new EconomyManager(this);
        economyManager.reload();

        rouletteManager = new RouletteManager(this, economyManager);
        menuManager = new RouletteMenuManager(this, rouletteManager);
        rouletteManager.setMenuManager(menuManager);

        rouletteManager.load();
        getServer().getPluginManager().registerEvents(menuManager, this);
        getServer().getPluginManager().registerEvents(rouletteManager, this);

        PyRouletteCommand command = new PyRouletteCommand(this, rouletteManager, economyManager);
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        if (rouletteManager != null) {
            rouletteManager.shutdown();
        }
    }

    public void reloadPlugin() {
        ConfigValidator.validateSavedConfig(this);
        reloadConfig();
        ConfigValidator.validate(this);
        auditLogger.reload();
        metricsManager.reload();
        economyManager.reload();
        menuManager.reload();
        rouletteManager.reloadVisuals();
    }

    public RouletteManager getRouletteManager() {
        return rouletteManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger;
    }
}
