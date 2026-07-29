package org.me.pyke.pyRoulette.roulette;

import java.util.UUID;

public record RouletteBet(UUID playerId, String playerName, BetType type, String value, double amount) {
    public boolean wins(RoulettePocket pocket) {
        return switch (type) {
            case NUMBER -> pocket.label().equals(value);
            case RED -> pocket.color() == PocketColor.RED;
            case BLACK -> pocket.color() == PocketColor.BLACK;
            case GREEN -> pocket.color() == PocketColor.GREEN;
            case COLUMN_1 -> inColumn(pocket, 1);
            case COLUMN_2 -> inColumn(pocket, 2);
            case COLUMN_3 -> inColumn(pocket, 3);
        };
    }

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

    private boolean inColumn(RoulettePocket pocket, int column) {
        if (pocket.color() == PocketColor.GREEN) {
            return false;
        }
        int number = Integer.parseInt(pocket.label());
        return switch (column) {
            case 1 -> number % 3 == 1;
            case 2 -> number % 3 == 2;
            case 3 -> number % 3 == 0;
            default -> false;
        };
    }
}
