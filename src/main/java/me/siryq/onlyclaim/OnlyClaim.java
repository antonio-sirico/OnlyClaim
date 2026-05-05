package me.siryq.onlyclaim;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;

public class OnlyClaim extends JavaPlugin {

    private static OnlyClaim instance;
    private ClaimManager claimManager;
    private ConfigManager configManager;
    private ClaimToolListener claimToolListener;

    @Override
    public void onEnable() {
        instance = this;

        // Recupero versione dal plugin.yml
        String version = getDescription().getVersion();

        // 1. Inizializza Configurazione e Messaggi
        this.configManager = new ConfigManager(this);

        // 2. Inizializza il Manager dei Dati
        this.claimManager = new ClaimManager(this);

        // 3. Inizializza e Registra i Listener
        this.claimToolListener = new ClaimToolListener(this);
        getServer().getPluginManager().registerEvents(this.claimToolListener, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // 4. Registra il comando principale
        if (getCommand("onlyclaim") != null) {
            getCommand("onlyclaim").setExecutor(new ClaimCommand(this));
        }

        // Messaggio di avvio colorato in console
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §aPlugin abilitato correttamente! §7(v" + version + ")");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §fSistema di salvataggio .dat: §aATTIVO");
        Bukkit.getConsoleSender().sendMessage("");
    }

    public void spawnBorderParticles(Player player, Location p1, Location p2) {
        if (p1 == null || p2 == null) return;

        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        double y = player.getLocation().getY() + 1.0; // Altezza petto

        // Creiamo il colore giallo (RGB)
        org.bukkit.Particle.DustOptions dust = new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f);

        // Disegniamo il perimetro (linee sottili di particelle)
        for (double x = minX; x <= maxX; x += 0.5) {
            player.spawnParticle(org.bukkit.Particle.DUST, x, y, minZ, 1, dust);
            player.spawnParticle(org.bukkit.Particle.DUST, x, y, maxZ, 1, dust);
        }
        for (double z = minZ; z <= maxZ; z += 0.5) {
            player.spawnParticle(org.bukkit.Particle.DUST, minX, y, z, 1, dust);
            player.spawnParticle(org.bukkit.Particle.DUST, maxX, y, z, 1, dust);
        }
    }

    @Override
    public void onDisable() {
        // Salvataggio finale
        if (claimManager != null) {
            claimManager.saveClaims();
            Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §eDati salvati e plugin disabilitato.");
        }
    }

    /**
     * Recupera le posizioni selezionate da un giocatore.
     */
    public Location[] getSelection(UUID uuid) {
        if (claimToolListener == null) return null;
        return claimToolListener.getSelections().get(uuid);
    }

    /**
     * Pulisce la selezione attuale di un giocatore.
     */
    public void clearSelection(UUID uuid) {
        if (claimToolListener != null) {
            claimToolListener.getSelections().remove(uuid);
        }
    }

    // --- GETTER ---

    public static OnlyClaim getInstance() {
        return instance;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}