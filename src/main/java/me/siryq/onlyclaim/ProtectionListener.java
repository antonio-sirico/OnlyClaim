package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

public class ProtectionListener implements Listener {

    private final OnlyClaim plugin;

    public ProtectionListener(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    /**
     * Gestisce la rottura dei blocchi.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (canDoAction(player, event.getBlock().getLocation(), "block-break")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("cannot-build"));
    }

    /**
     * Gestisce il piazzamento dei blocchi.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (canDoAction(player, event.getBlock().getLocation(), "block-place")) return;

        event.setCancelled(true);
        player.sendMessage(plugin.getConfigManager().getMessage("cannot-build"));
    }

    /**
     * Gestisce le interazioni (aprire casse, porte, bottoni).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (canDoAction(player, event.getClickedBlock().getLocation(), "interact")) return;

        event.setCancelled(true);
    }

    /**
     * LOGICA TNT: Gestisce le esplosioni.
     * Se un blocco che sta per esplodere si trova in un claim protetto, viene rimosso dalla lista.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : event.blockList()) {
            Claim claim = plugin.getClaimManager().getClaimAt(block.getLocation());

            if (claim != null) {
                // Se la flag 'tnt' è false (di default), il blocco non deve esplodere
                if (!claim.getFlag("tnt", false)) {
                    blocksToRemove.add(block);
                }
            }
        }

        // Rimuoviamo i blocchi protetti dall'esplosione
        event.blockList().removeAll(blocksToRemove);
    }

    /**
     * Metodo interno per verificare se un giocatore può agire in una posizione.
     * Controlla se c'è un claim, se il giocatore è il proprietario o se ha i permessi admin.
     */
    private boolean canDoAction(Player player, Location loc, String flag) {
        // Se il giocatore ha il permesso bypass (admin), può fare tutto
        if (player.hasPermission("onlyclaim.admin")) return true;

        Claim claim = plugin.getClaimManager().getClaimAt(loc);

        // Se non c'è nessun claim, l'azione è permessa (mondo libero)
        if (claim == null) return true;

        // Se il giocatore è il proprietario, può agire a prescindere dalle flag
        if (claim.getOwner().equals(player.getUniqueId())) return true;

        // Se non è il proprietario, controlliamo se la flag specifica permette l'azione
        // (Esempio: un proprietario potrebbe mettere 'interact' su true per tutti)
        return claim.getFlag(flag, false);
    }
}