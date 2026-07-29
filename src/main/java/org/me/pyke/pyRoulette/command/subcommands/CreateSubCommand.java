package org.me.pyke.pyRoulette.command.subcommands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.command.BaseSubCommand;
import org.me.pyke.pyRoulette.lang.Lang;
import org.me.pyke.pyRoulette.roulette.RouletteInstance;
import org.me.pyke.pyRoulette.roulette.RouletteManager;

import java.util.Locale;
import java.util.Map;

public final class CreateSubCommand extends BaseSubCommand {
    private final PyRoulette plugin;
    private final RouletteManager rouletteManager;

    public CreateSubCommand(PyRoulette plugin, RouletteManager rouletteManager) {
        this.plugin = plugin;
        this.rouletteManager = rouletteManager;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Create a roulette at your location.";
    }

    @Override
    public String getUsage() {
        return "/pyroulette create [radius]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.send(plugin, sender, "messages.only-player", Map.of());
            return;
        }

        double radius = plugin.getConfig().getDouble("settings.default-radius", 5.0);
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException ignored) {
                Lang.send(plugin, sender, "messages.invalid-radius", Map.of("input", args[1]));
                return;
            }
            if (!Double.isFinite(radius) || radius <= 0.0) {
                Lang.send(plugin, sender, "messages.invalid-radius", Map.of("input", args[1]));
                return;
            }
        }

        double min = plugin.getConfig().getDouble("settings.min-radius", 2.0);
        double max = plugin.getConfig().getDouble("settings.max-radius", 20.0);
        radius = Math.max(min, Math.min(max, radius));
        Location location = player.getLocation();
        RouletteInstance roulette = rouletteManager.create(location, radius);
        Lang.send(plugin, sender, "messages.created", Map.of(
                "id", roulette.definition().id(),
                "radius", String.format(Locale.US, "%.2f", radius)
        ));
    }
}
