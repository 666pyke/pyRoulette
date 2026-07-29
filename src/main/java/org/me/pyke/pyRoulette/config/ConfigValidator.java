package org.me.pyke.pyRoulette.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.me.pyke.pyRoulette.PyRoulette;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static void validate(PyRoulette plugin) {
        validate(plugin.getConfig());
    }

    public static void validateSavedConfig(PyRoulette plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        validate(YamlConfiguration.loadConfiguration(file));
    }

    private static void validate(FileConfiguration config) {
        List<String> errors = new ArrayList<>();

        double minBet = finite(config, errors, "settings.min-bet", 100.0);
        double maxBet = finite(config, errors, "settings.max-bet", 100000.0);
        if (minBet <= 0.0) {
            errors.add("settings.min-bet must be greater than 0.");
        }
        if (maxBet <= 0.0) {
            errors.add("settings.max-bet must be greater than 0.");
        }
        if (minBet > maxBet) {
            errors.add("settings.min-bet cannot be greater than settings.max-bet.");
        }

        double minRadius = finite(config, errors, "settings.min-radius", 2.0);
        double maxRadius = finite(config, errors, "settings.max-radius", 20.0);
        double defaultRadius = finite(config, errors, "settings.default-radius", 5.0);
        if (minRadius <= 0.0) {
            errors.add("settings.min-radius must be greater than 0.");
        }
        if (maxRadius <= 0.0) {
            errors.add("settings.max-radius must be greater than 0.");
        }
        if (minRadius > maxRadius) {
            errors.add("settings.min-radius cannot be greater than settings.max-radius.");
        }
        if (defaultRadius < minRadius || defaultRadius > maxRadius) {
            errors.add("settings.default-radius must be between settings.min-radius and settings.max-radius.");
        }

        validatePositive(config, errors, "payouts.number");
        validatePositive(config, errors, "payouts.red");
        validatePositive(config, errors, "payouts.black");
        validatePositive(config, errors, "payouts.green");
        validatePositive(config, errors, "payouts.column");
        validatePositive(config, errors, "messages.broadcast.range");
        validatePositive(config, errors, "sounds.spin.range");
        validatePositive(config, errors, "sounds.result.range");
        double animationViewRange = finite(config, errors, "settings.animation-view-range", 48.0);
        if (animationViewRange < 0.0) {
            errors.add("settings.animation-view-range cannot be negative.");
        }

        int guiSize = config.getInt("gui.selection.size", 54);
        if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
            errors.add("gui.selection.size must be a multiple of 9 between 9 and 54.");
        }
        validateSlots(config, errors, "gui.selection.red-numbers", guiSize);
        validateSlots(config, errors, "gui.selection.black-numbers", guiSize);
        validateSlots(config, errors, "gui.selection.green-numbers", guiSize);
        validateSlots(config, errors, "gui.selection.column-bets.column-1.slots", guiSize);
        validateSlots(config, errors, "gui.selection.column-bets.column-2.slots", guiSize);
        validateSlots(config, errors, "gui.selection.column-bets.column-3.slots", guiSize);
        validateSlot(config, errors, "gui.selection.special-bets.black.slot", guiSize);
        validateSlot(config, errors, "gui.selection.special-bets.green.slot", guiSize);
        validateSlot(config, errors, "gui.selection.special-bets.red.slot", guiSize);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid pyRoulette config: " + String.join(" ", errors));
        }
    }

    private static void validatePositive(FileConfiguration config, List<String> errors, String path) {
        double value = finite(config, errors, path, 1.0);
        if (value <= 0.0) {
            errors.add(path + " must be greater than 0.");
        }
    }

    private static double finite(FileConfiguration config, List<String> errors, String path, double fallback) {
        double value = config.getDouble(path, fallback);
        if (!Double.isFinite(value)) {
            errors.add(path + " must be a finite number.");
        }
        return value;
    }

    private static void validateSlots(FileConfiguration config, List<String> errors, String path, int inventorySize) {
        for (int slot : config.getIntegerList(path)) {
            if (slot < 0 || slot >= inventorySize) {
                errors.add(path + " contains invalid slot " + slot + " for inventory size " + inventorySize + ".");
            }
        }
    }

    private static void validateSlot(FileConfiguration config, List<String> errors, String path, int inventorySize) {
        int slot = config.getInt(path, -1);
        if (slot < 0 || slot >= inventorySize) {
            errors.add(path + " must be between 0 and " + (inventorySize - 1) + ".");
        }
    }
}
