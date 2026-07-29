package org.me.pyke.pyRoulette.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.lang.Lang;
import org.me.pyke.pyRoulette.roulette.BetType;
import org.me.pyke.pyRoulette.roulette.PocketColor;
import org.me.pyke.pyRoulette.roulette.Payouts;
import org.me.pyke.pyRoulette.roulette.RouletteInstance;
import org.me.pyke.pyRoulette.roulette.RouletteManager;
import org.me.pyke.pyRoulette.roulette.RoulettePocket;
import org.me.pyke.pyRoulette.roulette.RouletteWheel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RouletteMenuManager implements Listener {
    private final PyRoulette plugin;
    private final RouletteManager rouletteManager;
    private final Map<UUID, PendingBet> pendingBets = new HashMap<>();
    private final Map<Integer, RoulettePocket> numberSlots = new HashMap<>();
    private final Map<Integer, ColumnBet> columnSlots = new HashMap<>();

    public RouletteMenuManager(PyRoulette plugin, RouletteManager rouletteManager) {
        this.plugin = plugin;
        this.rouletteManager = rouletteManager;
        reload();
    }

    public void reload() {
        numberSlots.clear();
        columnSlots.clear();
        loadNumberSlots("gui.selection.red-numbers", RoulettePocket.RED_LABELS);
        loadNumberSlots("gui.selection.black-numbers", RoulettePocket.BLACK_LABELS);
        loadNumberSlots("gui.selection.green-numbers", RoulettePocket.GREEN_LABELS);
        loadColumnSlots("gui.selection.column-bets.column-1.slots", BetType.COLUMN_1, "column-1");
        loadColumnSlots("gui.selection.column-bets.column-2.slots", BetType.COLUMN_2, "column-2");
        loadColumnSlots("gui.selection.column-bets.column-3.slots", BetType.COLUMN_3, "column-3");
        if (numberSlots.isEmpty()) {
            int index = 0;
            for (int slot : plugin.getConfig().getIntegerList("gui.selection.number-slots")) {
                if (index >= RouletteWheel.BETTING_LAYOUT.size()) {
                    break;
                }
                numberSlots.put(slot, RouletteWheel.BETTING_LAYOUT.get(index++));
            }
        }
    }

    public void open(Player player, RouletteInstance roulette) {
        openSelection(player, roulette);
    }

    private void openSelection(Player player, RouletteInstance roulette) {
        RouletteMenuHolder holder = new RouletteMenuHolder(roulette.definition().id());
        Inventory inventory = Bukkit.createInventory(holder, inventorySize("gui.selection.size", 54), Lang.component(plugin.getConfig().getString("gui.selection.title", "&8Roulette - Choose Bet")));
        holder.inventory(inventory);
        fill(inventory, "gui.selection.filler");

        for (Map.Entry<Integer, RoulettePocket> entry : numberSlots.entrySet()) {
            inventory.setItem(entry.getKey(), numberItem(entry.getValue()));
        }
        placeColumn(inventory, "column-1", BetType.COLUMN_1);
        placeColumn(inventory, "column-2", BetType.COLUMN_2);
        placeColumn(inventory, "column-3", BetType.COLUMN_3);
        placeSpecial(inventory, "black", BetType.BLACK);
        placeSpecial(inventory, "green", BetType.GREEN);
        placeSpecial(inventory, "red", BetType.RED);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        rouletteManager.handleInteraction(event.getPlayer(), clicked.getUniqueId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RouletteMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RouletteMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        RouletteInstance roulette = rouletteManager.get(holder.rouletteId());
        if (roulette == null) {
            player.closeInventory();
            Lang.send(plugin, player, "messages.not-found", Map.of());
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (numberSlots.containsKey(slot)) {
            RoulettePocket pocket = numberSlots.get(slot);
            waitForChatAmount(player, roulette, BetType.NUMBER, pocket.label());
            return;
        }

        if (columnSlots.containsKey(slot)) {
            ColumnBet columnBet = columnSlots.get(slot);
            waitForChatAmount(player, roulette, columnBet.type(), columnBet.value());
            return;
        }

        BetType special = specialBet(slot);
        if (special != null) {
            waitForChatAmount(player, roulette, special, special.name().toLowerCase(Locale.ROOT));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingBet pendingBet = pendingBets.remove(event.getPlayer().getUniqueId());
        if (pendingBet == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleChatAmount(event.getPlayer(), pendingBet, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingBets.remove(event.getPlayer().getUniqueId());
    }

    private BetType specialBet(int slot) {
        if (slot == plugin.getConfig().getInt("gui.selection.special-bets.black.slot", -1)) {
            return BetType.BLACK;
        }
        if (slot == plugin.getConfig().getInt("gui.selection.special-bets.green.slot", -1)) {
            return BetType.GREEN;
        }
        if (slot == plugin.getConfig().getInt("gui.selection.special-bets.red.slot", -1)) {
            return BetType.RED;
        }
        return null;
    }

    private void placeSpecial(Inventory inventory, String key, BetType type) {
        String path = "gui.selection.special-bets." + key;
        inventory.setItem(plugin.getConfig().getInt(path + ".slot", -1), configuredItem(path, Map.of(
                "multiplier", String.valueOf(multiplier(type))
        )));
    }

    private void placeColumn(Inventory inventory, String key, BetType type) {
        String path = "gui.selection.column-bets." + key;
        ItemStack item = configuredItem(path, Map.of(
                "column", key.substring(key.length() - 1),
                "multiplier", String.valueOf(multiplier(type))
        ));
        for (int slot : plugin.getConfig().getIntegerList(path + ".slots")) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    private ItemStack numberItem(RoulettePocket pocket) {
        String materialPath = switch (pocket.color()) {
            case RED -> "gui.selection.number-bet.material-red";
            case BLACK -> "gui.selection.number-bet.material-black";
            case GREEN -> "gui.selection.number-bet.material-green";
        };
        ItemStack item = new ItemStack(material(plugin.getConfig().getString(materialPath), Material.STONE));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        String color = switch (pocket.color()) {
            case RED -> "&c";
            case BLACK -> "&f";
            case GREEN -> "&a";
        };
        Map<String, String> placeholders = Map.of(
                "number", pocket.label(),
                "color", color,
                "multiplier", String.valueOf(multiplier(BetType.NUMBER))
        );
        meta.displayName(Lang.component(Lang.placeholders(plugin.getConfig().getString("gui.selection.number-bet.name", "{color}{number}"), placeholders)));
        meta.lore(lore(plugin.getConfig().getStringList("gui.selection.number-bet.lore"), placeholders));
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory, String path) {
        Material material = material(plugin.getConfig().getString(path + ".material", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.displayName(Lang.component(plugin.getConfig().getString(path + ".name", " ")));
        item.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item);
        }
    }

    private ItemStack configuredItem(String path, Map<String, String> placeholders) {
        ItemStack item = new ItemStack(material(plugin.getConfig().getString(path + ".material"), Material.STONE));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Lang.component(Lang.placeholders(plugin.getConfig().getString(path + ".name", ""), placeholders)));
        meta.lore(lore(plugin.getConfig().getStringList(path + ".lore"), placeholders));
        item.setItemMeta(meta);
        return item;
    }

    private List<net.kyori.adventure.text.Component> lore(List<String> lines, Map<String, String> placeholders) {
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(Lang.component(Lang.placeholders(line, placeholders)));
        }
        return lore;
    }

    private int inventorySize(String path, int fallback) {
        int size = Math.max(9, Math.min(54, plugin.getConfig().getInt(path, fallback)));
        return (size / 9) * 9;
    }

    private Material material(String name, Material fallback) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        return material == null ? fallback : material;
    }

    private void waitForChatAmount(Player player, RouletteInstance roulette, BetType type, String value) {
        int timeoutSeconds = Math.max(1, plugin.getConfig().getInt("gui.chat-input.timeout-seconds", 30));
        PendingBet pendingBet = new PendingBet(roulette.definition().id(), type, value, System.currentTimeMillis() + timeoutSeconds * 1000L);
        pendingBets.put(player.getUniqueId(), pendingBet);
        player.closeInventory();
        Lang.send(plugin, player, "messages.enter-bet-amount", Map.of(
                "bet", pendingBet.display(),
                "seconds", String.valueOf(timeoutSeconds),
                "min_bet", plugin.getEconomyManager().format(plugin.getConfig().getDouble("settings.min-bet", 100.0)),
                "max_bet", plugin.getEconomyManager().format(plugin.getConfig().getDouble("settings.max-bet", 100000.0))
        ));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingBet current = pendingBets.get(player.getUniqueId());
            if (current == pendingBet) {
                pendingBets.remove(player.getUniqueId());
                Lang.send(plugin, player, "messages.bet-input-expired", Map.of());
            }
        }, timeoutSeconds * 20L);
    }

    private void loadNumberSlots(String path, List<String> numbers) {
        List<Integer> slots = plugin.getConfig().getIntegerList(path);
        int amount = Math.min(slots.size(), numbers.size());
        for (int i = 0; i < amount; i++) {
            int slot = slots.get(i);
            if (slot >= 0) {
                numberSlots.put(slot, RoulettePocket.of(numbers.get(i)));
            }
        }
    }

    private void loadColumnSlots(String path, BetType type, String value) {
        for (int slot : plugin.getConfig().getIntegerList(path)) {
            if (slot >= 0) {
                columnSlots.put(slot, new ColumnBet(type, value));
            }
        }
    }

    private void handleChatAmount(Player player, PendingBet pendingBet, String message) {
        if (pendingBet.expired()) {
            Lang.send(plugin, player, "messages.bet-input-expired", Map.of());
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            Lang.send(plugin, player, "messages.bet-cancelled", Map.of());
            return;
        }
        Double amount = parseAmount(message);
        if (amount == null) {
            Lang.send(plugin, player, "messages.invalid-amount-format", Map.of("input", message));
            pendingBets.put(player.getUniqueId(), pendingBet);
            return;
        }
        RouletteInstance roulette = rouletteManager.get(pendingBet.rouletteId());
        if (roulette == null) {
            Lang.send(plugin, player, "messages.not-found", Map.of());
            return;
        }
        rouletteManager.placeBet(player, roulette, pendingBet.type(), pendingBet.value(), amount);
    }

    private Double parseAmount(String input) {
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("_", "");
        double multiplier = 1.0;
        if (normalized.endsWith("k")) {
            multiplier = 1_000.0;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("m")) {
            multiplier = 1_000_000.0;
            normalized = normalized.substring(0, normalized.length() - 1);
        } else if (normalized.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            double amount = Double.parseDouble(normalized) * multiplier;
            if (!Double.isFinite(amount) || amount <= 0.0) {
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private double multiplier(BetType type) {
        return Payouts.multiplier(plugin.getConfig(), type);
    }

    private record ColumnBet(BetType type, String value) {
    }
}
