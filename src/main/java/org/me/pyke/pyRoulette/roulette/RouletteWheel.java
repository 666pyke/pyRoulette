package org.me.pyke.pyRoulette.roulette;

import java.util.Arrays;
import java.util.List;

public final class RouletteWheel {
    public static final List<RoulettePocket> WHEEL_LAYOUT = Arrays.stream(new String[]{
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
            "13", "14", "15", "16", "17", "18", "00", "19", "20", "21", "22", "23",
            "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34",
            "35", "36"
    }).map(RoulettePocket::of).toList();
    public static final List<RoulettePocket> AMERICAN = WHEEL_LAYOUT;
    public static final List<RoulettePocket> BETTING_LAYOUT = Arrays.stream(new String[]{
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
            "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23",
            "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34",
            "35", "36"
    }).map(RoulettePocket::of).toList();

    private RouletteWheel() {
    }
}
