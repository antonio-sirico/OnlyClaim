package me.siryq.onlyclaim;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Claim implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID owner;
    private String name;
    private final String worldName;

    private final int minX, minZ;
    private final int maxX, maxZ;

    private final Map<String, Boolean> flags;
    private final List<Claim> subClaims;
    private final boolean isSubClaim;

    public Claim(UUID owner, String name, Location pos1, Location pos2, boolean isSubClaim) {
        this.owner = owner;
        this.name = name;
        this.worldName = pos1.getWorld().getName();
        this.isSubClaim = isSubClaim;

        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        this.flags = new HashMap<>();
        this.subClaims = new ArrayList<>();
    }

    /**
     * Verifica se una posizione si trova nel claim (anche su mondi diversi).
     */
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(this.worldName)) return false;

        // Usiamo blockX per essere sicuri di prendere l'intero blocco
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Calcola l'area 2D (superficie).
     */
    public int getArea() {
        return ((maxX - minX) + 1) * ((maxZ - minZ) + 1);
    }

    // --- FLAGS ---
    public boolean getFlag(String flag, boolean defaultValue) {
        return flags.getOrDefault(flag, defaultValue);
    }

    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    // --- GETTER ---

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

    /**
     * Recupera l'oggetto World dal nome salvato.
     * Utile per le particelle e teletrasporti.
     */
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
}