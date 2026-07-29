package org.me.pyke.pyRoulette.gui;

import org.me.pyke.pyRoulette.roulette.BetType;

public record PendingBet(String rouletteId, BetType type, String value, long expiresAtMillis) {
    public String display() {
        if (type == BetType.NUMBER) {
            return value;
        }
        return switch (type) {
            case COLUMN_1 -> "column 1";
            case COLUMN_2 -> "column 2";
            case COLUMN_3 -> "column 3";
            default -> type.name().toLowerCase();
        };
    }

    public boolean expired() {
        return System.currentTimeMillis() > expiresAtMillis;
    }
}
