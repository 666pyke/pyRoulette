package org.me.pyke.pyRoulette.command.subcommands;

import org.bukkit.command.CommandSender;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.command.BaseSubCommand;
import org.me.pyke.pyRoulette.lang.Lang;
import org.me.pyke.pyRoulette.roulette.RouletteDefinition;
import org.me.pyke.pyRoulette.roulette.RouletteInstance;
import org.me.pyke.pyRoulette.roulette.RouletteManager;

import java.util.Locale;
import java.util.Map;

public final class ListSubCommand extends BaseSubCommand {
    private final PyRoulette plugin;
    private final RouletteManager rouletteManager;

    public ListSubCommand(PyRoulette plugin, RouletteManager rouletteManager) {
        this.plugin = plugin;
        this.rouletteManager = rouletteManager;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "List saved roulettes.";
    }

    @Override
    public String getUsage() {
        return "/pyroulette list";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (rouletteManager.all().isEmpty()) {
            Lang.send(plugin, sender, "messages.list-empty", Map.of());
            return;
        }
        Lang.send(plugin, sender, "messages.list-header", Map.of());
        for (RouletteInstance roulette : rouletteManager.all()) {
            RouletteDefinition definition = roulette.definition();
            Lang.sendRaw(sender, plugin.getConfig().getString("messages.list-entry", ""), Map.of(
                    "id", definition.id(),
                    "world", definition.worldName(),
                    "x", String.format(Locale.US, "%.2f", definition.x()),
                    "y", String.format(Locale.US, "%.2f", definition.y()),
                    "z", String.format(Locale.US, "%.2f", definition.z()),
                    "radius", String.format(Locale.US, "%.2f", definition.radius())
            ));
        }
    }
}
