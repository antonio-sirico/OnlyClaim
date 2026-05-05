package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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

        // 1. Controllo validità dell'oggetto (deve essere il Claim Tool)
        if (item == null || item.getType() != Material.GOLDEN_HOE || !item.hasItemMeta()) return;

        String toolName = plugin.getConfigManager().getMessageRaw("claim-tool-name");
        if (!item.getItemMeta().getDisplayName().equals(toolName)) return;

        // 2. Controllo se ha cliccato un blocco
        if (event.getClickedBlock() == null) return;

        // Impediamo alla zappa di arare il terreno o attivare altri eventi
        event.setCancelled(true);

        Location clickedLoc = event.getClickedBlock().getLocation();
        UUID uuid = player.getUniqueId();

        // Inizializza l'array se è la prima volta che il giocatore seleziona
        selections.putIfAbsent(uuid, new Location[2]);
        Location[] currentSel = selections.get(uuid);

        // 3. Gestione Sinistro (Pos 1) e Destro (Pos 2)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            currentSel[0] = clickedLoc;
            player.sendMessage(plugin.getConfigManager().getMessage("pos-1-set"));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            currentSel[1] = clickedLoc;
            player.sendMessage(plugin.getConfigManager().getMessage("pos-2-set"));
        }

        // 4. Gestione Visualizzatore Particelle
        if (currentSel[0] != null && currentSel[1] != null) {
            plugin.startVisualizer(player, currentSel[0], currentSel[1], org.bukkit.Color.YELLOW);

            String pendingName = plugin.getPendingName(player.getUniqueId());
            String nameToShow;

            nameToShow = Objects.requireNonNullElseGet(pendingName, () -> plugin.getConfigManager().getMessageRaw("default-claim-name-format")
                    .replace("{player}", player.getName()));

            // Recuperiamo la lista dal config e la inviamo riga per riga
            List<String> summary = plugin.getConfigManager().getMessageList("selection-summary");
            for (String line : summary) {
                player.sendMessage(line.replace("{name}", nameToShow));
            }
        }
    }

    /**
     * Permette alle altre classi (OnlyClaim e ClaimCommand) di accedere alle selezioni attive.
     */
    public Map<UUID, Location[]> getSelections() {
        return selections;
    }
}