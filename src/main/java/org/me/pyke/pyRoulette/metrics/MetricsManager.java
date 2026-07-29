package org.me.pyke.pyRoulette.metrics;

import org.bstats.bukkit.Metrics;
import org.me.pyke.pyRoulette.PyRoulette;

public final class MetricsManager {
    private static final int BSTATS_PLUGIN_ID = 32967;
    private final PyRoulette plugin;
    private Metrics metrics;

    public MetricsManager(PyRoulette plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        if (!plugin.getConfig().getBoolean("bstats.enabled", true)) {
            metrics = null;
            return;
        }
        if (metrics == null) {
            metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
        }
    }
}
