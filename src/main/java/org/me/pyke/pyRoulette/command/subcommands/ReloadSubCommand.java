package org.me.pyke.pyRoulette.command.subcommands;

import org.bukkit.command.CommandSender;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.command.BaseSubCommand;
import org.me.pyke.pyRoulette.economy.EconomyManager;
import org.me.pyke.pyRoulette.lang.Lang;

import java.util.Map;

public final class ReloadSubCommand extends BaseSubCommand {
    private final PyRoulette plugin;
    private final EconomyManager economyManager;

    public ReloadSubCommand(PyRoulette plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload pyRoulette.";
    }

    @Override
    public String getUsage() {
        return "/pyroulette reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadPlugin();
        Lang.send(plugin, sender, "messages.reloaded", Map.of("economy", economyManager.providerName()));
    }
}
