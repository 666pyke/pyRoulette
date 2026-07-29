package org.me.pyke.pyRoulette.roulette;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.me.pyke.pyRoulette.PyRoulette;
import org.me.pyke.pyRoulette.economy.EconomyManager;
import org.me.pyke.pyRoulette.lang.Lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Set;
import java.util.UUID;

public final class RouletteInstance {
    private final PyRoulette plugin;
    private final RouletteManager manager;
    private final EconomyManager economyManager;
    private final RouletteDefinition definition;
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> centerEntities = new ArrayList<>();
    private final Set<UUID> interactions = new HashSet<>();
    private final List<RouletteBet> bets = new ArrayList<>();
    private final SecureRandom random = new SecureRandom();
    private BukkitTask idleTask;
    private BukkitTask startTask;
    private BukkitTask spinTask;
    private BukkitTask resultTask;
    private BukkitTask particleTask;
    private BukkitTask hologramTask;
    private BukkitTask spinSoundTask;
    private double angleOffset;
    private long roundCounter;
    private long currentRoundId;
    private boolean spinning;
    private int highlightedWinnerIndex = -1;
    private int secondsUntilSpin = -1;

    public RouletteInstance(PyRoulette plugin, RouletteManager manager, EconomyManager economyManager, RouletteDefinition definition) {
        this.plugin = plugin;
        this.manager = manager;
        this.economyManager = economyManager;
        this.definition = definition;
    }

    public RouletteDefinition definition() {
        return definition;
    }

    public boolean hasInteraction(UUID entityId) {
        return interactions.contains(entityId);
    }

    public void spawn() {
        if (isSpawned()) {
            return;
        }
        Location center = definition.location();
        if (center == null || center.getWorld() == null) {
            plugin.getLogger().warning("Could not spawn roulette '" + definition.id() + "' because world '" + definition.worldName() + "' is not loaded.");
            return;
        }
        if (!requiredChunksLoaded()) {
            return;
        }

        for (int i = 0; i < RouletteWheel.AMERICAN.size(); i++) {
            RoulettePocket pocket = RouletteWheel.AMERICAN.get(i);
            spawnPocket(center, i, pocket);
        }
        spawnPointer(center);
        spawnCenterHologram(center);
        startHologramUpdates();
        startIdleAnimation();
    }

    public void spawnIfChunksLoaded() {
        if (requiredChunksLoaded()) {
            spawn();
        }
    }

    public boolean isSpawned() {
        return !entities.isEmpty() || !centerEntities.isEmpty();
    }

    public boolean requiredChunksLoaded() {
        Location center = definition.location();
        if (center == null || center.getWorld() == null) {
            return false;
        }
        World world = center.getWorld();
        for (ChunkCoordinate chunk : chunksUsedByRoulette(center)) {
            if (!world.isChunkLoaded(chunk.x(), chunk.z())) {
                return false;
            }
        }
        return true;
    }

    public boolean usesChunk(org.bukkit.Chunk chunk) {
        Location center = definition.location();
        if (center == null || center.getWorld() == null || !center.getWorld().equals(chunk.getWorld())) {
            return false;
        }
        return chunksUsedByRoulette(center).contains(new ChunkCoordinate(chunk.getX(), chunk.getZ()));
    }

    public void removeEntities() {
        stopVisualTasks();
        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }
        if (spinTask != null) {
            spinTask.cancel();
            spinTask = null;
        }
        if (resultTask != null) {
            resultTask.cancel();
            resultTask = null;
        }
        removeVisualEntities();
        spinning = false;
        secondsUntilSpin = -1;
    }

    public void despawnForChunkUnload() {
        if (spinning || resultTask != null) {
            stopVisualTasks();
            removeVisualEntities();
            audit("ROUND_VISUAL_UNLOAD", Map.of(
                    "roulette", definition.id(),
                    "round", String.valueOf(currentRoundId),
                    "state", resultTask != null ? "result-freeze" : "spinning"
            ));
            return;
        }
        cancelRound("chunk unload");
        removeEntities();
    }

    private void stopVisualTasks() {
        if (idleTask != null) {
            idleTask.cancel();
            idleTask = null;
        }
        if (hologramTask != null) {
            hologramTask.cancel();
            hologramTask = null;
        }
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (spinSoundTask != null) {
            spinSoundTask.cancel();
            spinSoundTask = null;
        }
    }

    private void removeVisualEntities() {
        for (Entity entity : entities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        for (Entity entity : centerEntities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        entities.clear();
        centerEntities.clear();
        interactions.clear();
    }

    public void cancelRound(String reason) {
        refundActiveBets(reason);
        stopRoundTasks();
        spinning = false;
        secondsUntilSpin = -1;
        highlightedWinnerIndex = -1;
        updateHologramText();
    }

    public int betCount(UUID playerId) {
        int count = 0;
        for (RouletteBet bet : bets) {
            if (bet.playerId().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    public void placeBet(Player player, BetType type, String value, double amount) {
        amount = normalizeMoney(amount);
        double min = plugin.getConfig().getDouble("settings.min-bet", 100.0);
        double max = plugin.getConfig().getDouble("settings.max-bet", 100000.0);
        if (amount < min || amount > max) {
            Lang.send(plugin, player, "messages.invalid-bet", Map.of("min_bet", economyManager.format(min), "max_bet", economyManager.format(max)));
            return;
        }

        int maxBets = plugin.getConfig().getInt("settings.max-player-bets-per-round", 1);
        if (maxBets > 0 && betCount(player.getUniqueId()) >= maxBets) {
            Lang.send(plugin, player, "messages.bet-limit", Map.of());
            return;
        }

        if (spinning) {
            Lang.send(plugin, player, "messages.round-spinning", Map.of());
            return;
        }

        if (!economyManager.has(player, amount) || !economyManager.withdraw(player, amount)) {
            Lang.send(plugin, player, "messages.not-enough-money", Map.of());
            return;
        }

        RouletteBet bet = new RouletteBet(player.getUniqueId(), player.getName(), type, value, amount);
        bets.add(bet);
        updateHologramText();
        scheduleStart();
        audit("BET_PLACED", Map.of(
                "roulette", definition.id(),
                "player", player.getName(),
                "uuid", player.getUniqueId().toString(),
                "bet", bet.display(),
                "type", type.name(),
                "amount", economyManager.format(amount),
                "round", String.valueOf(currentRoundId)
        ));
        Lang.send(plugin, player, "messages.bet-placed", Map.of("amount", economyManager.format(amount), "bet", bet.display()));
    }

    private void scheduleStart() {
        if (startTask != null || spinning) {
            return;
        }
        int delaySeconds = plugin.getConfig().getInt("settings.start-delay-seconds", 10);
        currentRoundId = ++roundCounter;
        secondsUntilSpin = delaySeconds;
        audit("ROUND_SCHEDULED", Map.of(
                "roulette", definition.id(),
                "round", String.valueOf(currentRoundId),
                "delaySeconds", String.valueOf(delaySeconds),
                "totalBets", String.valueOf(bets.size()),
                "totalAmount", economyManager.format(totalBetAmount())
        ));
        broadcast("messages.round-starting", Map.of("seconds", String.valueOf(delaySeconds)));
        startCountdown(delaySeconds);
        startTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            startTask = null;
            secondsUntilSpin = -1;
            spin();
        }, Math.max(1, delaySeconds) * 20L);
    }

    private void spin() {
        if (bets.isEmpty()) {
            return;
        }
        spinning = true;
        if (idleTask != null) {
            idleTask.cancel();
            idleTask = null;
        }
        broadcast("messages.round-spinning", Map.of());
        audit("ROUND_SPIN_START", Map.of(
                "roulette", definition.id(),
                "round", String.valueOf(currentRoundId),
                "totalBets", String.valueOf(bets.size()),
                "totalAmount", economyManager.format(totalBetAmount())
        ));
        startSpinSound();
        int winnerIndex = random.nextInt(RouletteWheel.AMERICAN.size());
        int durationTicks = Math.max(20, plugin.getConfig().getInt("settings.spin-duration-seconds", 8) * 20);
        double startAngle = angleOffset;
        double targetAngle = -2.0 * Math.PI * winnerIndex / RouletteWheel.AMERICAN.size();
        int fullRotations = Math.max(1, plugin.getConfig().getInt("settings.spin-full-rotations", 5));
        double delta = forwardDelta(startAngle, targetAngle) + (2.0 * Math.PI * fullRotations);
        int[] tick = {0};

        spinTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double progress = Math.min(1.0, tick[0] / (double) durationTicks);
            angleOffset = startAngle + delta * easeOutCubic(progress);
            updatePositions(false);
            tick[0]++;
            if (tick[0] > durationTicks) {
                spinTask.cancel();
                spinTask = null;
                stopSpinSound();
                finishSpin(winnerIndex);
            }
        }, 1L, 1L);
    }

    private void finishSpin(int winnerIndex) {
        RoulettePocket winner = RouletteWheel.AMERICAN.get(winnerIndex);
        double targetAngle = -2.0 * Math.PI * winnerIndex / RouletteWheel.AMERICAN.size();
        angleOffset = targetAngle;
        updatePositions(true);
        highlightWinner(winnerIndex, winner);
        startWinnerParticles(winnerIndex);
        playConfiguredSound("sounds.result");
        audit("ROUND_RESULT", Map.of(
                "roulette", definition.id(),
                "round", String.valueOf(currentRoundId),
                "winner", winner.label(),
                "winnerColor", winner.color().name(),
                "totalBets", String.valueOf(bets.size()),
                "totalAmount", economyManager.format(totalBetAmount())
        ));
        broadcast("messages.round-result", Map.of("result", winner.label()));

        List<RouletteBet> settled = new ArrayList<>(bets);
        bets.clear();
        int freezeTicks = Math.max(0, plugin.getConfig().getInt("settings.result-freeze-seconds", 4) * 20);
        resultTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resultTask = null;
            stopWinnerParticles();
            settleBets(winner, settled);
            resetWinnerHighlight();
            spinning = false;
            if (isSpawned()) {
                startIdleAnimation();
            }
        }, freezeTicks);
    }

    private void settleBets(RoulettePocket winner, List<RouletteBet> settled) {
        for (RouletteBet bet : settled) {
            if (bet.wins(winner)) {
                double payout = normalizeMoney(bet.amount() * Payouts.multiplier(plugin.getConfig(), bet.type()));
                boolean deposited = economyManager.deposit(bet.playerId(), bet.playerName(), payout);
                Player player = Bukkit.getPlayer(bet.playerId());
                if (player != null) {
                    if (deposited) {
                        Lang.send(plugin, player, "messages.bet-won", Map.of("amount", economyManager.format(payout)));
                    } else {
                        Lang.send(plugin, player, "messages.payout-failed", Map.of("amount", economyManager.format(payout)));
                    }
                }
                if (!deposited) {
                    plugin.getLogger().warning("Could not deposit roulette payout " + payout + " to " + bet.playerName() + " (" + bet.playerId() + ")");
                }
                audit("BET_WIN", Map.of(
                        "roulette", definition.id(),
                        "round", String.valueOf(currentRoundId),
                        "player", bet.playerName(),
                        "uuid", bet.playerId().toString(),
                        "bet", bet.display(),
                        "betAmount", economyManager.format(bet.amount()),
                        "payout", economyManager.format(payout),
                        "deposited", String.valueOf(deposited)
                ));
            } else {
                Player player = Bukkit.getPlayer(bet.playerId());
                if (player != null) {
                    Lang.send(plugin, player, "messages.bet-lost", Map.of("bet", bet.display()));
                }
                audit("BET_LOSS", Map.of(
                        "roulette", definition.id(),
                        "round", String.valueOf(currentRoundId),
                        "player", bet.playerName(),
                        "uuid", bet.playerId().toString(),
                        "bet", bet.display(),
                        "betAmount", economyManager.format(bet.amount()),
                        "payout", economyManager.format(0.0)
                ));
            }
        }
        updateHologramText();
        currentRoundId = 0L;
    }

    private void refundActiveBets(String reason) {
        if (bets.isEmpty()) {
            return;
        }
        List<RouletteBet> refunds = new ArrayList<>(bets);
        bets.clear();
        for (RouletteBet bet : refunds) {
            boolean deposited = economyManager.deposit(bet.playerId(), bet.playerName(), bet.amount());
            Player player = Bukkit.getPlayer(bet.playerId());
            if (player != null) {
                if (deposited) {
                    Lang.send(plugin, player, "messages.bet-refunded", Map.of(
                            "amount", economyManager.format(bet.amount()),
                            "reason", reason
                    ));
                } else {
                    Lang.send(plugin, player, "messages.refund-failed", Map.of("amount", economyManager.format(bet.amount())));
                }
            }
            if (!deposited) {
                plugin.getLogger().warning("Could not refund roulette bet " + bet.amount() + " to " + bet.playerName() + " (" + bet.playerId() + ")");
            }
            audit("BET_REFUND", Map.of(
                    "roulette", definition.id(),
                    "round", String.valueOf(currentRoundId),
                    "player", bet.playerName(),
                    "uuid", bet.playerId().toString(),
                    "bet", bet.display(),
                    "amount", economyManager.format(bet.amount()),
                    "reason", reason,
                    "deposited", String.valueOf(deposited)
            ));
        }
        currentRoundId = 0L;
    }

    private void stopRoundTasks() {
        if (startTask != null) {
            startTask.cancel();
            startTask = null;
        }
        if (spinTask != null) {
            spinTask.cancel();
            spinTask = null;
        }
        if (resultTask != null) {
            resultTask.cancel();
            resultTask = null;
        }
        stopSpinSound();
        stopWinnerParticles();
    }

    private void broadcast(String path, Map<String, String> placeholders) {
        Location center = definition.location();
        if (center == null || center.getWorld() == null) {
            return;
        }
        double radius = plugin.getConfig().getDouble("messages.broadcast.range", Math.max(16.0, definition.radius() * 4.0));
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radius * radius) {
                Lang.send(plugin, player, path, placeholders);
            }
        }
    }

    private double normalizeMoney(double amount) {
        return Math.round(amount * 100.0D) / 100.0D;
    }

    private void spawnPocket(Location center, int index, RoulettePocket pocket) {
        Location base = locationFor(center, index);
        Location textLoc = textLocation(base, center, index);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        ItemDisplay itemDisplay = (ItemDisplay) world.spawnEntity(base, EntityType.ITEM_DISPLAY);
        itemDisplay.setPersistent(false);
        itemDisplay.setItemStack(displayItem(pocket.color()));
        itemDisplay.setBillboard(Display.Billboard.FIXED);
        itemDisplay.setBrightness(new Display.Brightness(15, 15));
        applyDisplayInterpolation(itemDisplay);
        Transformation transformation = itemDisplay.getTransformation();
        float scale = pocketScale(pocket.color());
        itemDisplay.setTransformation(new Transformation(
                transformation.getTranslation(),
                transformation.getLeftRotation(),
                new Vector3f(scale, scale, scale),
                transformation.getRightRotation()
        ));
        entities.add(itemDisplay);

        TextDisplay textDisplay = (TextDisplay) world.spawnEntity(textLoc, EntityType.TEXT_DISPLAY);
        textDisplay.setPersistent(false);
        textDisplay.text(Lang.component(Lang.placeholders(plugin.getConfig().getString("display.number-text", "&6{number}"), Map.of("number", pocket.label()))));
        textDisplay.setBillboard(billboard(plugin.getConfig().getString("display.number-billboard", "VERTICAL")));
        textDisplay.setBrightness(new Display.Brightness(15, 15));
        textDisplay.setSeeThrough(plugin.getConfig().getBoolean("display.number-see-through", false));
        textDisplay.setShadowed(plugin.getConfig().getBoolean("display.number-shadow", true));
        textDisplay.setDefaultBackground(false);
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(plugin.getConfig().getInt("display.number-background-argb", 0)));
        applyDisplayInterpolation(textDisplay);
        entities.add(textDisplay);

        Interaction interaction = (Interaction) world.spawnEntity(base, EntityType.INTERACTION);
        interaction.setPersistent(false);
        interaction.setInteractionWidth((float) plugin.getConfig().getDouble("settings.interaction-width", 1.15));
        interaction.setInteractionHeight((float) plugin.getConfig().getDouble("settings.interaction-height", 1.4));
        interactions.add(interaction.getUniqueId());
        entities.add(interaction);
    }

    private Set<ChunkCoordinate> chunksUsedByRoulette(Location center) {
        Set<ChunkCoordinate> chunks = new HashSet<>();
        chunks.add(chunkCoordinate(center));
        chunks.add(chunkCoordinate(pointerLocation(center)));
        chunks.add(chunkCoordinate(hologramLocation(center)));
        for (int i = 0; i < RouletteWheel.AMERICAN.size(); i++) {
            chunks.add(chunkCoordinate(locationFor(center, i)));
        }
        return chunks;
    }

    private ChunkCoordinate chunkCoordinate(Location location) {
        return new ChunkCoordinate(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private record ChunkCoordinate(int x, int z) {
    }

    private void spawnPointer(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Location pointerLocation = pointerLocation(center);
        ItemDisplay pointer = (ItemDisplay) world.spawnEntity(pointerLocation, EntityType.ITEM_DISPLAY);
        pointer.setPersistent(false);
        pointer.setItemStack(new ItemStack(pointerMaterial()));
        pointer.setItemDisplayTransform(itemDisplayTransform(plugin.getConfig().getString("display.pointer-transform", "FIXED")));
        pointer.setBillboard(Display.Billboard.FIXED);
        pointer.setBrightness(new Display.Brightness(15, 15));
        applyDisplayInterpolation(pointer);
        Transformation transformation = pointer.getTransformation();
        float scale = (float) plugin.getConfig().getDouble("display.pointer-scale", 0.7);
        pointer.setTransformation(new Transformation(
                transformation.getTranslation(),
                configuredRotation("display.pointer-rotation"),
                new Vector3f(scale, scale, scale),
                transformation.getRightRotation()
        ));
        entities.add(pointer);
    }

    private void spawnCenterHologram(Location center) {
        if (!plugin.getConfig().getBoolean("hologram.enabled", true) || center.getWorld() == null) {
            return;
        }
        TextDisplay hologram = (TextDisplay) center.getWorld().spawnEntity(hologramLocation(center), EntityType.TEXT_DISPLAY);
        hologram.setPersistent(false);
        hologram.setBillboard(billboard(plugin.getConfig().getString("hologram.billboard", "VERTICAL")));
        hologram.setBrightness(new Display.Brightness(15, 15));
        hologram.setShadowed(plugin.getConfig().getBoolean("hologram.shadow", true));
        hologram.setSeeThrough(plugin.getConfig().getBoolean("hologram.see-through", false));
        hologram.setDefaultBackground(false);
        hologram.setBackgroundColor(org.bukkit.Color.fromARGB(plugin.getConfig().getInt("hologram.background-argb", 0)));
        hologram.text(hologramText());
        centerEntities.add(hologram);
    }

    private void updatePositions(boolean moveInteractions) {
        Location center = definition.location();
        if (center == null) {
            return;
        }
        if (entities.size() < RouletteWheel.AMERICAN.size() * 3) {
            return;
        }
        int entityIndex = 0;
        for (int i = 0; i < RouletteWheel.AMERICAN.size(); i++) {
            Location base = locationFor(center, i);
            Entity item = entities.get(entityIndex++);
            Entity text = entities.get(entityIndex++);
            Entity interaction = entities.get(entityIndex++);
            item.teleport(base);
            text.teleport(textLocation(base, center, i));
            if (moveInteractions) {
                interaction.teleport(base);
            }
        }
    }

    private Location locationFor(Location center, int index) {
        double angle = (2.0 * Math.PI * index / RouletteWheel.AMERICAN.size()) + angleOffset;
        double x = center.getX() + definition.radius() * Math.cos(angle);
        double z = center.getZ() + definition.radius() * Math.sin(angle);
        double y = center.getY() + plugin.getConfig().getDouble("settings.entity-y-offset", 1.15);
        float yaw = 0.0F;
        if (plugin.getConfig().getString("display.pocket-rotation-mode", "NONE").equalsIgnoreCase("RADIAL")) {
            yaw = (float) Math.toDegrees(-angle) + 90.0F;
        } else {
            yaw = (float) plugin.getConfig().getDouble("display.pocket-fixed-yaw", 0.0);
        }
        return new Location(center.getWorld(), x, y, z, yaw, (float) plugin.getConfig().getDouble("display.pocket-fixed-pitch", 0.0));
    }

    private Location pointerLocation(Location center) {
        double pocketScale = plugin.getConfig().getDouble("display.pocket-scale", 0.45);
        double offset = plugin.getConfig().getDouble("display.pointer-radius-offset", 0.35);
        double x = center.getX() + definition.radius() + (pocketScale * 0.7) + offset;
        double y = center.getY() + plugin.getConfig().getDouble("display.pointer-y-offset", 1.9);
        float yaw = (float) plugin.getConfig().getDouble("display.pointer-location-yaw", 90.0);
        float pitch = (float) plugin.getConfig().getDouble("display.pointer-location-pitch", 0.0);
        return new Location(center.getWorld(), x, y, center.getZ(), yaw, pitch);
    }

    private Location textLocation(Location base, Location center, int index) {
        double angle = (2.0 * Math.PI * index / RouletteWheel.AMERICAN.size()) + angleOffset;
        double radialOffset = plugin.getConfig().getDouble("display.number-radius-offset", 0.0);
        double x = base.getX() + radialOffset * Math.cos(angle);
        double z = base.getZ() + radialOffset * Math.sin(angle);
        double y = center.getY() + plugin.getConfig().getDouble("display.number-y-offset", plugin.getConfig().getDouble("settings.text-y-offset", 1.85));
        return new Location(center.getWorld(), x, y, z, base.getYaw(), base.getPitch());
    }

    private Location hologramLocation(Location center) {
        Location location = center.clone().add(0.0, plugin.getConfig().getDouble("hologram.y-offset", 2.6), 0.0);
        location.setYaw((float) plugin.getConfig().getDouble("hologram.location-yaw", 0.0));
        location.setPitch((float) plugin.getConfig().getDouble("hologram.location-pitch", 0.0));
        return location;
    }

    private Material pointerMaterial() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("display.pointer-material", "SPECTRAL_ARROW"));
        return material == null ? Material.SPECTRAL_ARROW : material;
    }

    private Quaternionf configuredRotation(String path) {
        float x = (float) Math.toRadians(plugin.getConfig().getDouble(path + ".x", 0.0));
        float y = (float) Math.toRadians(plugin.getConfig().getDouble(path + ".y", 0.0));
        float z = (float) Math.toRadians(plugin.getConfig().getDouble(path + ".z", 0.0));
        return new Quaternionf().rotateXYZ(x, y, z);
    }

    private void highlightWinner(int winnerIndex, RoulettePocket winner) {
        resetWinnerHighlight();
        highlightedWinnerIndex = winnerIndex;
        if (entities.size() < RouletteWheel.AMERICAN.size() * 3) {
            return;
        }
        applyPocketScale(winnerIndex, (float) plugin.getConfig().getDouble("display.winner-scale", 0.75));
        Entity text = entities.get(winnerIndex * 3 + 1);
        if (text instanceof TextDisplay textDisplay) {
            textDisplay.text(Lang.component(Lang.placeholders(plugin.getConfig().getString("display.winning-text", "&aWinner: &f{result}"), Map.of("result", winner.label()))));
        }
    }

    private void resetWinnerHighlight() {
        if (highlightedWinnerIndex < 0 || highlightedWinnerIndex >= RouletteWheel.AMERICAN.size() || entities.size() < RouletteWheel.AMERICAN.size() * 3) {
            highlightedWinnerIndex = -1;
            return;
        }
        RoulettePocket pocket = RouletteWheel.AMERICAN.get(highlightedWinnerIndex);
        applyPocketScale(highlightedWinnerIndex, pocketScale(pocket.color()));
        Entity text = entities.get(highlightedWinnerIndex * 3 + 1);
        if (text instanceof TextDisplay textDisplay) {
            textDisplay.text(Lang.component(Lang.placeholders(plugin.getConfig().getString("display.number-text", "&6{number}"), Map.of("number", pocket.label()))));
        }
        highlightedWinnerIndex = -1;
    }

    private void applyPocketScale(int index, float scale) {
        Entity entity = entities.get(index * 3);
        if (!(entity instanceof ItemDisplay itemDisplay)) {
            return;
        }
        Transformation transformation = itemDisplay.getTransformation();
        itemDisplay.setTransformation(new Transformation(
                transformation.getTranslation(),
                transformation.getLeftRotation(),
                new Vector3f(scale, scale, scale),
                transformation.getRightRotation()
        ));
    }

    private float pocketScale(PocketColor color) {
        String key = color.name().toLowerCase(Locale.ROOT);
        boolean usesHead = plugin.getConfig().getString("textures." + key, "").isBlank() == false;
        String path = usesHead ? "display.head-scale" : "display.pocket-scale";
        return (float) plugin.getConfig().getDouble(path, usesHead ? 0.9 : 0.45);
    }

    private void startIdleAnimation() {
        if (!plugin.getConfig().getBoolean("settings.idle-animation.enabled", true) || idleTask != null || spinning) {
            return;
        }
        double speed = Math.toRadians(plugin.getConfig().getDouble("settings.idle-animation.speed-degrees-per-tick", 0.35));
        int interactionInterval = Math.max(1, plugin.getConfig().getInt("settings.idle-animation.interaction-update-interval-ticks", 5));
        int[] tick = {0};
        idleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (spinning) {
                return;
            }
            if (!hasNearbyViewers()) {
                return;
            }
            angleOffset += speed;
            updatePositions(tick[0]++ % interactionInterval == 0);
        }, 1L, 1L);
    }

    private boolean hasNearbyViewers() {
        Location center = definition.location();
        if (center == null || center.getWorld() == null) {
            return false;
        }
        double range = plugin.getConfig().getDouble("settings.animation-view-range", 48.0);
        if (range <= 0.0) {
            return true;
        }
        double rangeSquared = range * range;
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    private void startCountdown(int delaySeconds) {
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (startTask == null || secondsUntilSpin <= 0) {
                task.cancel();
                return;
            }
            secondsUntilSpin--;
            updateHologramText();
        }, 20L, 20L);
    }

    private void startHologramUpdates() {
        if (!plugin.getConfig().getBoolean("hologram.enabled", true) || hologramTask != null) {
            return;
        }
        hologramTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateHologramText, 20L, 20L);
    }

    private void updateHologramText() {
        if (centerEntities.isEmpty()) {
            return;
        }
        Entity entity = centerEntities.get(0);
        if (entity instanceof TextDisplay textDisplay) {
            textDisplay.text(hologramText());
        }
    }

    private net.kyori.adventure.text.Component hologramText() {
        List<String> lines = plugin.getConfig().getStringList("hologram.lines");
        if (lines.isEmpty()) {
            lines = List.of("&6pyRoulette", "&7Bets: &f{total_bets}", "&7Amount: &f{amount}", "&7Spin: &f{time_until_spin}");
        }
        Map<String, String> placeholders = Map.of(
                "total_bets", String.valueOf(bets.size()),
                "amount", economyManager.format(totalBetAmount()),
                "total_bet_amount", economyManager.format(totalBetAmount()),
                "time_until_spin", timeUntilSpinText()
        );
        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            parsed.add(Lang.placeholders(line, placeholders));
        }
        return Lang.component(String.join("\n", parsed));
    }

    private double totalBetAmount() {
        double total = 0.0;
        for (RouletteBet bet : bets) {
            total += bet.amount();
        }
        return total;
    }

    private String timeUntilSpinText() {
        if (spinning) {
            return plugin.getConfig().getString("hologram.states.spinning", "Spinning");
        }
        if (secondsUntilSpin >= 0) {
            return secondsUntilSpin + "s";
        }
        return plugin.getConfig().getString("hologram.states.waiting", "Waiting");
    }

    private double forwardDelta(double from, double to) {
        double delta = (to - from) % (2.0 * Math.PI);
        if (delta < 0.0) {
            delta += 2.0 * Math.PI;
        }
        return delta;
    }

    private double easeOutCubic(double progress) {
        double inverse = 1.0 - progress;
        return 1.0 - inverse * inverse * inverse;
    }

    private void applyDisplayInterpolation(Display display) {
        display.setInterpolationDelay(plugin.getConfig().getInt("display.interpolation-delay", 0));
        display.setInterpolationDuration(plugin.getConfig().getInt("display.interpolation-duration", 2));
        display.setTeleportDuration(plugin.getConfig().getInt("display.teleport-duration", 1));
    }

    private void startSpinSound() {
        if (!plugin.getConfig().getBoolean("sounds.spin.enabled", true) || spinSoundTask != null) {
            return;
        }
        long interval = Math.max(1L, plugin.getConfig().getLong("sounds.spin.interval-ticks", 6L));
        spinSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> playConfiguredSound("sounds.spin"), 0L, interval);
    }

    private void stopSpinSound() {
        if (spinSoundTask != null) {
            spinSoundTask.cancel();
            spinSoundTask = null;
        }
    }

    private void playConfiguredSound(String path) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        Location center = definition.location();
        if (center == null || center.getWorld() == null) {
            return;
        }
        String sound = plugin.getConfig().getString(path + ".sound", "BLOCK_NOTE_BLOCK_HAT");
        org.bukkit.SoundCategory category = soundCategory(plugin.getConfig().getString(path + ".category", "PLAYERS"));
        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 0.7);
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0);
        double range = plugin.getConfig().getDouble(path + ".range", Math.max(16.0, definition.radius() * 4.0));
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= range * range) {
                playSound(player, center, sound, category, volume, pitch);
            }
        }
    }

    private void playSound(Player player, Location location, String configured, org.bukkit.SoundCategory category, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(configured.toUpperCase(Locale.ROOT));
            player.playSound(location, sound, category, volume, pitch);
            return;
        } catch (IllegalArgumentException ignored) {
        }

        String key = configured.toLowerCase(Locale.ROOT);
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        player.playSound(location, key, category, volume, pitch);
    }

    private void audit(String action, Map<String, String> values) {
        plugin.getAuditLogger().log(action, values);
    }

    private org.bukkit.SoundCategory soundCategory(String configured) {
        try {
            return org.bukkit.SoundCategory.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return org.bukkit.SoundCategory.PLAYERS;
        }
    }

    private Display.Billboard billboard(String configured) {
        try {
            return Display.Billboard.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Display.Billboard.VERTICAL;
        }
    }

    private ItemDisplay.ItemDisplayTransform itemDisplayTransform(String configured) {
        try {
            return ItemDisplay.ItemDisplayTransform.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ItemDisplay.ItemDisplayTransform.FIXED;
        }
    }

    private void startWinnerParticles(int winnerIndex) {
        if (!plugin.getConfig().getBoolean("display.winner-particles.enabled", true) || !isSpawned()) {
            return;
        }
        Particle particle = particle(plugin.getConfig().getString("display.winner-particles.type", "HAPPY_VILLAGER"));
        int count = plugin.getConfig().getInt("display.winner-particles.count", 8);
        double spread = plugin.getConfig().getDouble("display.winner-particles.spread", 0.25);
        long interval = Math.max(1L, plugin.getConfig().getLong("display.winner-particles.interval-ticks", 5L));
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Location center = definition.location();
            if (center == null) {
                return;
            }
            Location location = locationFor(center, winnerIndex).add(0.0, plugin.getConfig().getDouble("display.winner-particles.y-offset", 0.35), 0.0);
            World world = location.getWorld();
            if (world != null) {
                world.spawnParticle(particle, location, count, spread, spread, spread, 0.01);
            }
        }, 0L, interval);
    }

    private void stopWinnerParticles() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }

    private Particle particle(String configured) {
        try {
            return Particle.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Particle.HAPPY_VILLAGER;
        }
    }

    private ItemStack displayItem(PocketColor color) {
        String key = color.name().toLowerCase(Locale.ROOT);
        String texture = plugin.getConfig().getString("textures." + key, "");
        if (texture != null && !texture.isBlank()) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) {
                return head;
            }
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(("pyroulette-" + key).getBytes()));
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
            return head;
        }

        Material material = Material.matchMaterial(plugin.getConfig().getString("textures.fallback-material." + key, ""));
        return new ItemStack(material == null ? Material.STONE : material);
    }
}
