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
        // Gestione corretta della creazione cartella con controllo errore
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().severe("ERRORE CRITICO: Impossibile creare la cartella OnlyClaim!");
                return;
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(claims);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore critico nel salvataggio claims.dat: " + e.getMessage());
        }
    }

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

    public Claim getClaimAt(Location loc) {
        for (Claim claim : claims) {
            if (claim.contains(loc)) {
                for (Claim sub : claim.getSubClaims()) {
                    if (sub.contains(loc)) return sub;
                }
                return claim;
            }
        }
        return null;
    }

    public void addClaim(Claim claim) {
        this.claims.add(claim);
        saveClaims();
    }

    public void removeClaim(Claim claim) {
        for (Claim main : claims) {
            if (main.getSubClaims().remove(claim)) {
                saveClaims();
                return;
            }
        }
        if (claims.remove(claim)) {
            saveClaims();
        }
    }

    public Claim getClaimByName(String name) {
        for (Claim claim : claims) {
            if (claim.getName().equalsIgnoreCase(name)) return claim;
            for (Claim sub : claim.getSubClaims()) {
                if (sub.getName().equalsIgnoreCase(name)) return sub;
            }
        }
        return null;
    }

    public boolean exists(String name) {
        return claims.stream().anyMatch(c -> c.getName().equalsIgnoreCase(name));
    }

    /**
     * Calcola l'area totale occupata da un giocatore (solo claim principali).
     */
    public int getTotalUsedArea(UUID ownerUUID) {
        return claims.stream()
                .filter(c -> c.getOwner().equals(ownerUUID) && !c.isSubClaim())
                .mapToInt(Claim::getArea)
                .sum();
    }

    /**
     * Restituisce il numero massimo di chunk permessi.
     */
    public int getMaxChunksForPlayer(Player player) {
        String adminPerm = plugin.getConfig().getString("admin-permission", "onlyclaim.admin");
        if (player.hasPermission(adminPerm)) return -1;

        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return 4;

        int max = 0;
        boolean hasInfinite = false;

        for (String key : groups.getKeys(false)) {
            String perm = groups.getString(key + ".permission");
            if (perm != null && player.hasPermission(perm)) {
                int chunks = groups.getInt(key + ".max-chunks", 0);
                if (chunks == -1) hasInfinite = true;
                if (chunks > max) max = chunks;
            }
        }

        if (hasInfinite) return -1;
        return (max == 0) ? 4 : max;
    }

    /**
     * Verifica se il giocatore può claimare un'area aggiuntiva.
     */
    public boolean canClaimMore(Player player, int areaRichiesta) {
        int maxChunks = getMaxChunksForPlayer(player);
        if (maxChunks == -1) return true;

        int maxBlocksAllowed = maxChunks * 256;
        int currentArea = getTotalUsedArea(player.getUniqueId());

        return (currentArea + areaRichiesta) <= maxBlocksAllowed;
    }

    public List<Claim> getAllClaims() {
        return new ArrayList<>(claims);
    }
}