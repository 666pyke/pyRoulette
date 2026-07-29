package org.me.pyke.pyRoulette.gui;

import org.bukkit.inventory.Inventory;

public final class RouletteMenuHolder implements org.bukkit.inventory.InventoryHolder {
    private final String rouletteId;
    private Inventory inventory;

    public RouletteMenuHolder(String rouletteId) {
        this.rouletteId = rouletteId;
    }

    public String rouletteId() {
        return rouletteId;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
