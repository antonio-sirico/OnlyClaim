package me.siryq.onlyclaim;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OnlyClaim extends JavaPlugin {

    private static OnlyClaim instance;
    private ClaimManager claimManager;
    private ConfigManager configManager;
    private ClaimToolListener claimToolListener;

    // Gestione dei Task per le particelle persistenti
    private final Map<UUID, Integer> particleTasks = new HashMap<>();
    // Aggiungi questa variabile in cima alla classe OnlyClaim
    private final Map<UUID, String> pendingNames = new HashMap<>();

    // Aggiungi questi metodi per gestire i nomi
    public void setPendingName(UUID uuid, String name) {
        pendingNames.put(uuid, name);
    }

    public String getPendingName(UUID uuid) {
        return pendingNames.get(uuid);
    }

    public void clearPendingName(UUID uuid) {
        pendingNames.remove(uuid);
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Inizializzazione Configurazione e Messaggi
        this.configManager = new ConfigManager(this);

        // 2. Inizializzazione Manager Dati
        this.claimManager = new ClaimManager(this);

        // 3. Registrazione Listener (Strumento e Protezione)
        this.claimToolListener = new ClaimToolListener(this);
        getServer().getPluginManager().registerEvents(this.claimToolListener, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // 4. Registrazione Comandi
        if (getCommand("onlyclaim") != null) {
            getCommand("onlyclaim").setExecutor(new ClaimCommand(this));
        }

        // Messaggio di avvio elegante e colorato
        String version = getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §aPlugin abilitato con successo! §7(v" + version + ")");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §fSistema di visualizzazione: §aATTIVO");
        Bukkit.getConsoleSender().sendMessage("");
    }

    @Override
    public void onDisable() {
        // Ferma tutti i task delle particelle attivi per pulizia memoria
        for (int taskId : particleTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        particleTasks.clear();

        // Salvataggio finale dei dati nel file .dat
        if (claimManager != null) {
            claimManager.saveClaims();
            Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §eDati salvati correttamente. Alla prossima!");
        }
    }

    // --- SISTEMA DI VISUALIZZAZIONE PARTICELLE ---

    /**
     * Avvia un task che mostra il perimetro del colore specificato.
     * @param color Il colore delle particelle (es. Color.YELLOW per selezione, Color.LIME per view)
     */
    public void startVisualizer(Player player, Location p1, Location p2, Color color) {
        UUID uuid = player.getUniqueId();
        stopVisualizer(uuid); // Rimuove task precedenti

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!player.isOnline()) {
                stopVisualizer(uuid);
                return;
            }
            spawnBorderParticles(player, p1, p2, color);
        }, 0L, 20L); // Loop ogni secondo (20 ticks)

        particleTasks.put(uuid, taskId);
    }

    /**
     * Ferma la visualizzazione delle particelle.
     */
    public void stopVisualizer(UUID uuid) {
        if (particleTasks.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(particleTasks.get(uuid));
            particleTasks.remove(uuid);
        }
    }

    /**
     * Esegue il rendering fisico delle particelle DUST.
     */
    public void spawnBorderParticles(Player player, Location p1, Location p2, Color color) {
        if (p1 == null || p2 == null) return;

        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        // Altezza dinamica basata sulla posizione del giocatore per visibilità ottimale
        double y = player.getLocation().getY() + 1.1;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);

        // Disegno perimetro
        for (double x = minX; x <= maxX; x += 0.5) {
            player.spawnParticle(Particle.DUST, x, y, minZ, 1, dustOptions);
            player.spawnParticle(Particle.DUST, x, y, maxZ, 1, dustOptions);
        }
        for (double z = minZ; z <= maxZ; z += 0.5) {
            player.spawnParticle(Particle.DUST, minX, y, z, 1, dustOptions);
            player.spawnParticle(Particle.DUST, maxX, y, z, 1, dustOptions);
        }
    }

    // --- UTILS ---

    /**
     * Riproduce un suono per il giocatore.
     */
    public void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public Location[] getSelection(UUID uuid) {
        if (claimToolListener == null) return null;
        return claimToolListener.getSelections().get(uuid);
    }

    public void clearSelection(UUID uuid) {
        if (claimToolListener != null) {
            claimToolListener.getSelections().remove(uuid);
        }
        stopVisualizer(uuid);
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