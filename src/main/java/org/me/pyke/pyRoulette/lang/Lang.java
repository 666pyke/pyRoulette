package org.me.pyke.pyRoulette.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.me.pyke.pyRoulette.PyRoulette;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Lang {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Lang() {
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                replacement.append('§').append(character);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static Component component(String text) {
        return LEGACY.deserialize(color(text));
    }

    public static String placeholders(String input, Map<String, String> placeholders) {
        String result = input == null ? "" : input;
        if (placeholders == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static void send(PyRoulette plugin, CommandSender sender, String path, Map<String, String> placeholders) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String message = plugin.getConfig().getString(path, "");
        sender.sendMessage(color(placeholders(prefix + message, placeholders)));
    }

    public static void sendRaw(CommandSender sender, String message, Map<String, String> placeholders) {
        sender.sendMessage(color(placeholders(message, placeholders)));
    }

    public static void sendList(PyRoulette plugin, CommandSender sender, String path) {
        List<String> lines = plugin.getConfig().getStringList(path);
        for (String line : lines) {
            sender.sendMessage(color(line));
        }
    }
}
