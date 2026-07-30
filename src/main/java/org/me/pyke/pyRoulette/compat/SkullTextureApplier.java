package org.me.pyke.pyRoulette.compat;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkullTextureApplier {
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private SkullTextureApplier() {
    }

    public static ItemStack apply(ItemStack head, String base64, String profileKey) {
        if (!(head.getItemMeta() instanceof SkullMeta meta) || base64 == null || base64.isBlank()) {
            return head;
        }

        String url = textureUrl(base64);
        if (url == null) {
            return head;
        }

        try {
            UUID uuid = UUID.nameUUIDFromBytes(("pyroulette-" + profileKey).getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, profileKey.substring(0, Math.min(profileKey.length(), 16)));
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create(url).toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (IllegalArgumentException | MalformedURLException exception) {
            Bukkit.getLogger().warning("[pyRoulette] Failed to apply custom skull texture for " + profileKey + ": " + exception.getMessage());
        }
        return head;
    }

    private static String textureUrl(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            return matcher.find() ? matcher.group(1) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
