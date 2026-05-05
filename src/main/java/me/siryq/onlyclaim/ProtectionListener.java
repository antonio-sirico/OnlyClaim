package me.siryq.onlyclaim;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

public class ProtectionListener implements Listener {

    private final OnlyClaim plugin;

    public ProtectionListener(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    /**
     * Gestisce la rottura dei blocchi con feedback sonoro e messaggio custom.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (canDoAction(player, event.getBlock().getLocation(), "block-break")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("cannot-build"));
        plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    /**
     * Gestisce il piazzamento dei blocchi con feedback sonoro e messaggio custom.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (canDoAction(player, event.getBlock().getLocation(), "block-place")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("cannot-build"));
        plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    /**
     * Gestisce le interazioni (casse, porte, fornaci).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (canDoAction(player, event.getClickedBlock().getLocation(), "interact")) return;

        event.setCancelled(true);
        // Feedback sonoro senza messaggio per evitare spam
        plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
    }

    /**
     * LOGICA TNT: Protegge i blocchi all'interno dei claim dalle esplosioni.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : event.blockList()) {
            Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());
            if (claim != null) {
                // Se la flag 'tnt' è false (default), proteggiamo il blocco
                if (!claim.getFlag("tnt", false)) {
                    blocksToRemove.add(block);
                }
            }
        }
        event.blockList().removeAll(blocksToRemove);
    }

    /**
     * Gestisce i titoli all'entrata e all'uscita dai claim con messaggi dal lang.yml.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Ottimizzazione: controlla solo se il giocatore ha cambiato blocco (X o Z)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        Claim fromClaim = plugin.getClaimManager().getClaimAt(event.getFrom());
        Claim toClaim = plugin.getClaimManager().getClaimAt(event.getTo());

        // CASO 1: Entrata in un nuovo claim o passaggio tra claim diversi
        if (toClaim != null && (fromClaim == null || !fromClaim.equals(toClaim))) {
            String ownerName = Bukkit.getOfflinePlayer(toClaim.getOwner()).getName();
            if (ownerName == null) ownerName = plugin.getConfigManager().getMessageRaw("unknown-player");

            // Caricamento titoli customizzati
            String title = plugin.getConfigManager().getMessageRaw("enter-claim-title")
                    .replace("{name}", toClaim.getName())
                    .replace("{owner}", ownerName);

            String sub = plugin.getConfigManager().getMessageRaw("enter-claim-subtitle")
                    .replace("{name}", toClaim.getName())
                    .replace("{owner}", ownerName);

            player.sendTitle(title, sub, 10, 40, 10);

            // Pulizia visualizzatori gialli di selezione
            plugin.stopVisualizer(player.getUniqueId());
        }

        // CASO 2: Uscita da un claim verso la zona libera
        else if (toClaim == null && fromClaim != null) {
            String title = plugin.getConfigManager().getMessageRaw("exit-claim-title");
            String sub = plugin.getConfigManager().getMessageRaw("exit-claim-subtitle");

            player.sendTitle(title, sub, 10, 40, 10);

            // Spegniamo visualizzatori particelle (view o selezione)
            if (plugin.hasActiveVisualizer(player.getUniqueId())) {
                plugin.stopVisualizer(player.getUniqueId());
                player.sendMessage(plugin.getConfigManager().getMessage("view-stopped-exit"));
            }
        }
    }

    /**
     * Metodo di controllo permessi centralizzato.
     */
    private boolean canDoAction(Player player, Location loc, String flag) {
        if (player.hasPermission("onlyclaim.admin")) return true;

        Claim claim = plugin.getClaimManager().getClaimAt(loc);
        if (claim == null) return true;

        // Il proprietario ha sempre accesso totale
        if (claim.getOwner().equals(player.getUniqueId())) return true;

        // Controlla se la flag specifica è stata abilitata per i visitatori
        return claim.getFlag(flag, false);
    }
}