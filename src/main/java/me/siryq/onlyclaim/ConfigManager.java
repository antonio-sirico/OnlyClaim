package me.siryq.onlyclaim;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final OnlyClaim plugin;
    private FileConfiguration langConfig;

    public ConfigManager(OnlyClaim plugin) {
        this.plugin = plugin;
        setupConfig();
        loadLanguage();
    }

    /**
     * Crea il config.yml se non esiste.
     */
    private void setupConfig() {
        plugin.saveDefaultConfig();
    }

    /**
     * Carica il file della lingua basandosi su quello impostato nel config.yml.
     */
    public void loadLanguage() {
        String langTag = plugin.getConfig().getString("language", "it");
        String fileName = "lang_" + langTag + ".yml";
        File langFile = new File(plugin.getDataFolder(), fileName);

        if (!langFile.exists()) {
            // Salva il file dalla cartella resources del plugin se presente
            plugin.saveResource(fileName, false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Ricarica tutti i file di configurazione (config.yml e lingua).
     */
    public void reloadAll() {
        // Ricarica il config.yml di Spigot/Bukkit
        plugin.reloadConfig();
        // Ricarica il file della lingua specifico
        loadLanguage();
    }

    /**
     * Recupera un messaggio colorato con prefisso.
     */
    public String getMessage(String path) {
        String prefix = getMessageRaw("prefix");
        String message = langConfig.getString(path, "§cMessaggio mancante: " + path);
        return color(prefix + message);
    }

    /**
     * Recupera una lista di messaggi colorati.
     */
    public List<String> getMessageList(String key) {
        List<String> lines = langConfig.getStringList(key);
        List<String> translated = new ArrayList<>();
        for (String line : lines) {
            translated.add(color(line));
        }
        return translated;
    }

    /**
     * Recupera un messaggio colorato SENZA prefisso.
     */
    public String getMessageRaw(String path) {
        String message = langConfig.getString(path, "");
        return color(message);
    }

    /**
     * Utility per tradurre i codici colore '&'.
     */
    private String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }
}