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
import java.util.Objects;
import java.util.UUID;

public class OnlyClaim extends JavaPlugin {

    private static OnlyClaim instance;
    private ClaimManager claimManager;
    private ConfigManager configManager;
    private ClaimToolListener claimToolListener;
    private RTPManager rtpManager;
    private GUIManager guiManager;

    private final Map<UUID, Integer> particleTasks = new HashMap<>();
    private final Map<UUID, String> pendingNames = new HashMap<>();

    public void setPendingName(UUID uuid, String name) { pendingNames.put(uuid, name); }
    public String getPendingName(UUID uuid) { return pendingNames.get(uuid); }
    public void clearPendingName(UUID uuid) { pendingNames.remove(uuid); }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Inizializzazione Configurazione (Sempre per prima)
        this.configManager = new ConfigManager(this);

        // 2. Inizializzazione Manager Dati
        this.claimManager = new ClaimManager(this);
        this.rtpManager = new RTPManager(this);
        this.guiManager = new GUIManager(this);

        // 3. Registrazione Listener
        this.claimToolListener = new ClaimToolListener(this);
        getServer().getPluginManager().registerEvents(this.claimToolListener, this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        // Registro la GUI come listener solo se la classe implementa effettivamente Listener
        getServer().getPluginManager().registerEvents(this.guiManager, this);

        // 4. Registrazione Comandi e TabCompleter
        if (getCommand("onlyclaim") != null) {
            ClaimCommand cmd = new ClaimCommand(this);
            Objects.requireNonNull(getCommand("onlyclaim")).setExecutor(cmd);
            Objects.requireNonNull(getCommand("onlyclaim")).setTabCompleter(cmd);
        }

        String version = getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §aPlugin abilitato con successo! §7(v" + version + ")");
        Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §fInterfaccia GUI e Visualizer: §aATTIVI");
        Bukkit.getConsoleSender().sendMessage("");
    }

    @Override
    public void onDisable() {
        // Ferma i task particelle
        for (int taskId : particleTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        particleTasks.clear();

        // Salvataggio sicuro
        if (claimManager != null) {
            claimManager.saveClaims();
            Bukkit.getConsoleSender().sendMessage("§b§lOnlyClaim §8» §eDati salvati correttamente.");
        }
    }

    // --- SISTEMA DI VISUALIZZAZIONE PARTICELLE ---

    public void startVisualizer(Player player, Location p1, Location p2, Color color) {
        UUID uuid = player.getUniqueId();
        stopVisualizer(uuid);

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!player.isOnline()) {
                stopVisualizer(uuid);
                return;
            }
            spawnBorderParticles(player, p1, p2, color);
        }, 0L, 20L);

        particleTasks.put(uuid, taskId);
    }

    public void stopVisualizer(UUID uuid) {
        if (particleTasks.containsKey(uuid)) {
            Bukkit.getScheduler().cancelTask(particleTasks.get(uuid));
            particleTasks.remove(uuid);
        }
    }

    public void spawnBorderParticles(Player player, Location p1, Location p2, Color color) {
        if (p1 == null || p2 == null) return;

        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;
        double y = player.getLocation().getY() + 1.1;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);

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

    public void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public Location[] getSelection(UUID uuid) {
        return claimToolListener != null ? claimToolListener.getSelections().get(uuid) : null;
    }

    public void clearSelection(UUID uuid) {
        if (claimToolListener != null) claimToolListener.getSelections().remove(uuid);
        stopVisualizer(uuid);
    }

    public boolean hasActiveVisualizer(UUID uuid) {
        return particleTasks.containsKey(uuid);
    }

    // --- GETTER ---

    public static OnlyClaim getInstance() { return instance; }
    public ClaimManager getClaimManager() { return claimManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public RTPManager getRtpManager() { return rtpManager; }
    public GUIManager getGuiManager() { return guiManager; }
}