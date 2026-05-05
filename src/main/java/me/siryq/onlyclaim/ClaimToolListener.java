package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClaimToolListener implements Listener {

    private final OnlyClaim plugin;
    // Mappa che memorizza temporaneamente i due punti selezionati per ogni giocatore
    private final Map<UUID, Location[]> selections = new HashMap<>();

    public ClaimToolListener(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1. Controllo validità dell'oggetto
        if (item == null || item.getType() != Material.GOLDEN_HOE || !item.hasItemMeta()) return;

        String toolName = plugin.getConfigManager().getMessageRaw("claim-tool-name");
        if (!item.getItemMeta().getDisplayName().equals(toolName)) return;

        // 2. Controllo se ha cliccato un blocco
        if (event.getClickedBlock() == null) return;

        // Impediamo alla zappa di arare il terreno o aprire inventari
        event.setCancelled(true);

        Location clickedLoc = event.getClickedBlock().getLocation();
        UUID uuid = player.getUniqueId();

        // Inizializza l'array se è la prima volta che il giocatore clicca
        selections.putIfAbsent(uuid, new Location[2]);

        // 3. Gestione Sinistro (Pos 1) e Destro (Pos 2)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selections.get(uuid)[0] = clickedLoc;
            player.sendMessage(plugin.getConfigManager().getMessage("pos-1-set"));

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selections.get(uuid)[1] = clickedLoc;
            player.sendMessage(plugin.getConfigManager().getMessage("pos-2-set"));
        }

        // 4. Feedback se la selezione è completa
        Location[] currentSel = selections.get(uuid);
        if (currentSel[0] != null && currentSel[1] != null) {
            // Mandiamo un suggerimento in chat solo quando ha entrambi i punti
            plugin.spawnBorderParticles(player, currentSel[0], currentSel[1]);
            player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§eSelezione completata! Digita §b/oc confirm §eper proteggere l'area.");
        }
    }

    /**
     * Metodo fondamentale che mancava!
     * Permette alla classe principale di accedere alle selezioni.
     */
    public Map<UUID, Location[]> getSelections() {
        return selections;
    }
}