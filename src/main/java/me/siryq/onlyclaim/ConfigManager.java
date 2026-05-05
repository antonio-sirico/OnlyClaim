package me.siryq.onlyclaim;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final OnlyClaim plugin;
    private FileConfiguration langConfig;
    private File langFile;

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
        langFile = new File(plugin.getDataFolder(), fileName);

        if (!langFile.exists()) {
            // Salva il file dalla cartella resources del plugin se presente
            plugin.saveResource(fileName, false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
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
     * Recupera un messaggio colorato SENZA prefisso (per nomi oggetti o lore).
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