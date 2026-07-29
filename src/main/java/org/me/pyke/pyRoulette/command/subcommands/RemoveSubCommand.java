package org.me.pyke.pyRoulette.command.subcommands;

import org.bukkit.command.CommandSender;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.command.BaseSubCommand;
import org.me.pyke.pyRoulette.lang.Lang;
import org.me.pyke.pyRoulette.roulette.RouletteManager;

import java.util.List;
import java.util.Map;

public final class RemoveSubCommand extends BaseSubCommand {
    private final PyRoulette plugin;
    private final RouletteManager rouletteManager;

    public RemoveSubCommand(PyRoulette plugin, RouletteManager rouletteManager) {
        this.plugin = plugin;
        this.rouletteManager = rouletteManager;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Remove a saved roulette.";
    }

    @Override
    public String getUsage() {
        return "/pyroulette remove <id>";
    }

    @Override
    public List<String> getAliases() {
        return List.of("delete");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return rouletteManager.all().stream().map(instance -> instance.definition().id()).toList();
        }
        return List.of();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Lang.sendList(plugin, sender, "messages.usage");
            return;
        }
        if (rouletteManager.remove(args[1])) {
            Lang.send(plugin, sender, "messages.removed", Map.of("id", args[1]));
        } else {
            Lang.send(plugin, sender, "messages.not-found", Map.of());
        }
    }
}
