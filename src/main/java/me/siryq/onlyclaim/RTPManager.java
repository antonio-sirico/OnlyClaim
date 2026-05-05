package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RTPManager {

    private final OnlyClaim plugin;
    private final Map<UUID, Long> rtpCooldowns = new HashMap<>();
    private final Random random = new Random();

    public RTPManager(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    public void teleportRandomly(Player player) {
        // Recuperiamo il prefisso dedicato
        String prefix = plugin.getConfigManager().getMessageRaw("rtp-prefix");

        // 1. Controllo Permessi
        if (!player.hasPermission("onlyclaim.command.rtp")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        // 2. Gestione Cooldown
        if (!player.hasPermission("onlyclaim.rtp.bypass")) {
            int cooldownTime = plugin.getConfig().getInt("rtp-cooldown", 60);
            if (rtpCooldowns.containsKey(player.getUniqueId())) {
                long secondsLeft = ((rtpCooldowns.get(player.getUniqueId()) / 1000) + cooldownTime) - (System.currentTimeMillis() / 1000);
                if (secondsLeft > 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("rtp-cooldown-msg")
                            .replace("{prefix}", prefix)
                            .replace("{time}", String.valueOf(secondsLeft)));
                    return;
                }
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("rtp-searching")
                .replace("{prefix}", prefix));

        int range = plugin.getConfig().getInt("rtp-range", 5000);
        Location randomLoc = findSafeLocation(player.getWorld(), range);

        // 3. Controllo Esito Ricerca
        if (randomLoc == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("rtp-no-location")
                    .replace("{prefix}", prefix));
            plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // 4. Successo
        player.teleport(randomLoc.add(0.5, 1, 0.5));
        rtpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        String title = plugin.getConfigManager().getMessageRaw("rtp-title");
        String sub = plugin.getConfigManager().getMessageRaw("rtp-subtitle");
        player.sendTitle(title, sub, 10, 40, 10);

        plugin.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    private Location findSafeLocation(World world, int range) {
        for (int i = 0; i < 20; i++) {
            int x = random.nextInt(range * 2) - range;
            int z = random.nextInt(range * 2) - range;
            int y = world.getHighestBlockYAt(x, z);

            Location loc = new Location(world, x, y, z);

            // Verifica claim
            if (plugin.getClaimManager().getClaimAt(loc) != null) continue;

            // Verifica sicurezza blocchi
            Material blockType = loc.getBlock().getType();
            if (blockType == Material.WATER || blockType == Material.LAVA || blockType == Material.AIR) continue;

            return loc;
        }
        return null;
    }
}