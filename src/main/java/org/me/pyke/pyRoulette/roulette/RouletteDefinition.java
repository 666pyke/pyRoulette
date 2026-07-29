package org.me.pyke.pyRoulette.roulette;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record RouletteDefinition(String id, String worldName, double x, double y, double z, float yaw, float pitch, double radius) {
    public static RouletteDefinition fromLocation(String id, Location location, double radius) {
        return new RouletteDefinition(
                id,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                radius
        );
    }

    public Location location() {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }
}
