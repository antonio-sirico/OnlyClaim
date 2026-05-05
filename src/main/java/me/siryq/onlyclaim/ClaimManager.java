package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.io.*;
import java.util.*;

public class ClaimManager {

    private final OnlyClaim plugin;
    private List<Claim> claims;
    private final File dataFile;

    public ClaimManager(OnlyClaim plugin) {
        this.plugin = plugin;
        this.claims = new ArrayList<>();
        this.dataFile = new File(plugin.getDataFolder(), "claims.dat");
        loadClaims();
    }

    /**
     * Salva la lista dei claim in formato binario .dat
     */
    public void saveClaims() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(claims);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore critico nel salvataggio claims.dat: " + e.getMessage());
        }
    }

    /**
     * Carica la lista dei claim dal file binario .dat
     */
    @SuppressWarnings("unchecked")
    private void loadClaims() {
        if (!dataFile.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                this.claims = (List<Claim>) obj;
                plugin.getLogger().info("Database caricato: " + claims.size() + " claim attivi.");
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Impossibile leggere claims.dat. Il file potrebbe essere corrotto.");
            this.claims = new ArrayList<>();
        }
    }

    /**
     * Trova il claim (o sottoclaim) in una posizione specifica.
     */
    public Claim getClaimAt(Location loc) {
        for (Claim claim : claims) {
            if (claim.contains(loc)) {
                // Se siamo dentro un claim, controlliamo se siamo sopra un sottoclaim più specifico
                for (Claim sub : claim.getSubClaims()) {
                    if (sub.contains(loc)) return sub;
                }
                return claim;
            }
        }
        return null;
    }

    /**
     * Aggiunge un nuovo claim e salva i dati.
     */
    public void addClaim(Claim claim) {
        this.claims.add(claim);
        saveClaims();
    }

    /**
     * Rimuove un claim dal database e salva le modifiche.
     * @param claim Il claim da rimuovere.
     */
    public void removeClaim(Claim claim) {
        // Se è un sottoclaim, dobbiamo cercarlo all'interno del claim padre per rimuoverlo correttamente
        for (Claim main : claims) {
            if (main.getSubClaims().remove(claim)) {
                saveClaims();
                return;
            }
        }

        // Se arriviamo qui, era un claim principale
        if (claims.remove(claim)) {
            saveClaims();
        }
    }

    /**
     * Cerca un claim nella lista globale tramite il suo nome (case-insensitive).
     */
    public Claim getClaimByName(String name) {
        for (Claim claim : claims) {
            if (claim.getName().equalsIgnoreCase(name)) return claim;

            // Cerca anche tra i sottoclaim se necessario
            for (Claim sub : claim.getSubClaims()) {
                if (sub.getName().equalsIgnoreCase(name)) return sub;
            }
        }
        return null;
    }

    /**
     * Verifica se un nome di claim è già stato utilizzato.
     */
    public boolean exists(String name) {
        return claims.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    /**
     * Calcola l'area totale occupata da un giocatore (in blocchi).
     */
    public int getTotalUsedArea(UUID ownerUUID) {
        return claims.stream()
                .filter(c -> c.getOwner().equals(ownerUUID))
                .mapToInt(Claim::getArea)
                .sum();
    }

    /**
     * Logica dei limiti: controlla se il giocatore ha abbastanza chunk
     * basandosi sul suo gruppo di permessi nel config.yml.
     */
    public boolean canClaimMore(Player player, int areaRichiesta) {
        if (player.hasPermission("onlyclaim.admin")) return true;

        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return false;

        int maxChunks = 0;

        // Controlla tutti i gruppi e prende il valore più alto tra quelli che il giocatore possiede
        for (String key : groups.getKeys(false)) {
            String permission = groups.getString(key + ".permission");
            if (permission != null && player.hasPermission(permission)) {
                int groupMax = groups.getInt(key + ".max-chunks", 0);
                if (groupMax > maxChunks) maxChunks = groupMax;
            }
        }

        int maxBlocksAllowed = maxChunks * 256; // 1 chunk = 16x16 = 256 blocchi
        int currentArea = getTotalUsedArea(player.getUniqueId());

        return (currentArea + areaRichiesta) <= maxBlocksAllowed;
    }

    /**
     * Restituisce una copia della lista per iterazioni sicure.
     */
    public List<Claim> getAllClaims() {
        return new ArrayList<>(claims);
    }
}