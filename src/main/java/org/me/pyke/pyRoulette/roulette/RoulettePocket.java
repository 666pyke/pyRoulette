package org.me.pyke.pyRoulette.roulette;

import java.util.Set;
import java.util.List;
import java.util.stream.IntStream;

public record RoulettePocket(String label, PocketColor color) {
    private static final Set<Integer> REDS = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    public static final List<String> RED_LABELS = IntStream.rangeClosed(1, 36)
            .filter(REDS::contains)
            .mapToObj(String::valueOf)
            .toList();
    public static final List<String> BLACK_LABELS = IntStream.rangeClosed(1, 36)
            .filter(number -> !REDS.contains(number))
            .mapToObj(String::valueOf)
            .toList();
    public static final List<String> GREEN_LABELS = List.of("0", "00");

    public static RoulettePocket of(String label) {
        if ("0".equals(label) || "00".equals(label)) {
            return new RoulettePocket(label, PocketColor.GREEN);
        }
        int number = Integer.parseInt(label);
        return new RoulettePocket(label, REDS.contains(number) ? PocketColor.RED : PocketColor.BLACK);
    }
}
