package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Claim implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID owner;
    private String name;
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final Map<String, Boolean> flags;
    private final List<Claim> subClaims;
    private final boolean isSubClaim;

    /**
     * Costruttore per un nuovo Claim o Sottoclaim.
     */
    public Claim(UUID owner, String name, Location pos1, Location pos2, boolean isSubClaim) {
        this.owner = owner;
        this.name = name;
        this.world = pos1.getWorld();
        this.isSubClaim = isSubClaim;

        // Calcoliamo min e max per creare il rettangolo di protezione
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        this.flags = new HashMap<>();
        this.subClaims = new ArrayList<>();
    }

    /**
     * Verifica se una posizione specifica si trova all'interno di questo claim.
     */
    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(world)) return false;

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    /**
     * Calcola l'area totale (superficie 2D) del claim.
     * Serve per il calcolo del budget basato sui chunk (1 chunk = 256 blocchi).
     */
    public int getArea() {
        int width = (maxX - minX) + 1;
        int length = (maxZ - minZ) + 1;
        return width * length;
    }

    /**
     * Restituisce il valore di una flag. Se non impostata nel claim,
     * restituisce il valore di default passato.
     */
    public boolean getFlag(String flag, boolean defaultValue) {
        return flags.getOrDefault(flag, defaultValue);
    }

    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    // --- GETTER E SETTER ---

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Claim> getSubClaims() {
        return subClaims;
    }

    public void addSubClaim(Claim subClaim) {
        if (!this.isSubClaim) {
            this.subClaims.add(subClaim);
        }
    }

    public boolean isSubClaim() {
        return isSubClaim;
    }

    public World getWorld() {
        return world;
    }

    // Metodi di utilità per il salvataggio (coordinate grezze)
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
}