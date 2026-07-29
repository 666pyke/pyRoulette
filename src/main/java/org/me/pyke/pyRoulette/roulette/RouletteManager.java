package org.me.pyke.pyRoulette.roulette;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.economy.EconomyManager;
import org.me.pyke.pyRoulette.gui.RouletteMenuManager;
import org.me.pyke.pyRoulette.lang.Lang;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RouletteManager implements Listener {
    private final PyRoulette plugin;
    private final EconomyManager economyManager;
    private final Map<String, RouletteInstance> roulettes = new LinkedHashMap<>();
    private final File dataFile;
    private YamlConfiguration data;
    private RouletteMenuManager menuManager;

    public RouletteManager(PyRoulette plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.dataFile = new File(plugin.getDataFolder(), "roulettes.yml");
    }

    public void setMenuManager(RouletteMenuManager menuManager) {
        this.menuManager = menuManager;
    }

    public void load() {
        data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = data.getConfigurationSection("roulettes");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(id);
            if (item == null) {
                continue;
            }
            RouletteDefinition definition = new RouletteDefinition(
                    id,
                    item.getString("world", "world"),
                    item.getDouble("x"),
                    item.getDouble("y"),
                    item.getDouble("z"),
                    (float) item.getDouble("yaw"),
                    (float) item.getDouble("pitch"),
                    item.getDouble("radius", plugin.getConfig().getDouble("settings.default-radius", 5.0))
            );
            RouletteInstance instance = new RouletteInstance(plugin, this, economyManager, definition);
            roulettes.put(id, instance);
            instance.spawnIfChunksLoaded();
        }
    }

    public void reloadVisuals() {
        for (RouletteInstance instance : roulettes.values()) {
            instance.cancelRound("reload");
            instance.removeEntities();
            instance.spawnIfChunksLoaded();
        }
    }

    public RouletteInstance create(Location location, double radius) {
        String id = nextId(location.getWorld().getName());
        RouletteDefinition definition = RouletteDefinition.fromLocation(id, location, radius);
        RouletteInstance instance = new RouletteInstance(plugin, this, economyManager, definition);
        roulettes.put(id, instance);
        instance.spawnIfChunksLoaded();
        save();
        return instance;
    }

    public boolean remove(String id) {
        RouletteInstance instance = roulettes.remove(id.toLowerCase(Locale.ROOT));
        if (instance == null) {
            return false;
        }
        instance.cancelRound("remove");
        instance.removeEntities();
        save();
        return true;
    }

    public RouletteInstance get(String id) {
        return roulettes.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<RouletteInstance> all() {
        return roulettes.values();
    }

    public void handleInteraction(Player player, UUID entityId) {
        for (RouletteInstance instance : roulettes.values()) {
            if (instance.hasInteraction(entityId)) {
                menuManager.open(player, instance);
                return;
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (RouletteInstance instance : roulettes.values()) {
            if (!instance.isSpawned() && instance.usesChunk(chunk)) {
                instance.spawnIfChunksLoaded();
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (RouletteInstance instance : roulettes.values()) {
            if (instance.isSpawned() && instance.usesChunk(chunk)) {
                instance.despawnForChunkUnload();
            }
        }
    }

    public void shutdown() {
        for (RouletteInstance instance : roulettes.values()) {
            instance.cancelRound("shutdown");
            instance.removeEntities();
        }
        roulettes.clear();
    }

    public void save() {
        data.set("roulettes", null);
        for (RouletteInstance instance : roulettes.values()) {
            RouletteDefinition definition = instance.definition();
            String path = "roulettes." + definition.id() + ".";
            data.set(path + "world", definition.worldName());
            data.set(path + "x", definition.x());
            data.set(path + "y", definition.y());
            data.set(path + "z", definition.z());
            data.set(path + "yaw", definition.yaw());
            data.set(path + "pitch", definition.pitch());
            data.set(path + "radius", definition.radius());
        }

        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save roulettes.yml: " + exception.getMessage());
        }
    }

    public void placeBet(Player player, RouletteInstance roulette, BetType type, String value, double amount) {
        if (!economyManager.isAvailable()) {
            Lang.send(plugin, player, "messages.economy-unavailable", Map.of());
            return;
        }
        roulette.placeBet(player, type, value, amount);
    }

    private String nextId(String worldName) {
        String base = worldName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_") + "-";
        int index = 1;
        while (roulettes.containsKey(base + index)) {
            index++;
        }
        return base + index;
    }
}
