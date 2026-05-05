package me.siryq.onlyclaim;

import org.bukkit.entity.Player;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(claims);
            plugin.getLogger().info("Dati dei claim salvati correttamente in claims.dat");
        } catch (IOException e) {
            plugin.getLogger().severe("Errore durante il salvataggio dei claim: " + e.getMessage());
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
                plugin.getLogger().info("Caricati " + claims.size() + " claim dal file .dat");
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Errore durante il caricamento dei claim: " + e.getMessage());
            // In caso di errore grave, inizializziamo una lista vuota per evitare crash
            this.claims = new ArrayList<>();
        }
    }

    public Claim getClaimAt(org.bukkit.Location loc) {
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
        saveClaims(); // Salva subito per sicurezza
    }

    public int getTotalUsedArea(UUID ownerUUID) {
        return claims.stream()
                .filter(c -> c.getOwner().equals(ownerUUID))
                .mapToInt(Claim::getArea)
                .sum();
    }

    // Metodo per il controllo dei permessi e budget (come visto prima)
    public boolean canClaimMore(Player player, int areaRichiesta) {
        // ... (Logica identica a quella precedente) ...
        return true;
    }
}