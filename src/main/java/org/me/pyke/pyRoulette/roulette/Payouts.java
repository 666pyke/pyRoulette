package org.me.pyke.pyRoulette.roulette;

import org.bukkit.configuration.file.FileConfiguration;

public final class Payouts {
    private Payouts() {
    }

    public static double multiplier(FileConfiguration config, BetType type) {
        return switch (type) {
            case NUMBER -> config.getDouble("payouts.number", 35.0);
            case RED -> config.getDouble("payouts.red", 1.95);
            case BLACK -> config.getDouble("payouts.black", 1.95);
            case GREEN -> config.getDouble("payouts.green", 17.5);
            case COLUMN_1, COLUMN_2, COLUMN_3 -> config.getDouble("payouts.column", 3.0);
        };
    }
}
