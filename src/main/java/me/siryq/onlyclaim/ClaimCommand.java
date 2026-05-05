package me.siryq.onlyclaim;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final OnlyClaim plugin;

    public ClaimCommand(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Il comando reload è l'unico accessibile anche da console
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadPlugin(sender);
            return true;
        }

        // Controllo per tutti gli altri comandi (solo Player)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageRaw("only-players"));
            return true;
        }

        if (args.length == 0) {
            giveClaimTool(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> createNamedClaim(player, args);
            case "confirm" -> confirmClaim(player);
            case "delete", "remove" -> deleteClaimByName(player, args);
            case "subclaim" -> confirmSubClaim(player);
            case "view" -> viewClaim(player, args);
            case "rename" -> renameClaim(player, args);
            case "help" -> sendHelp(player);
            case "flag" -> setClaimFlag(player, args);
            case "rtp" -> plugin.getRtpManager().teleportRandomly(player);
            case "home", "tp" -> teleportToClaim(player, args);
            case "list" -> plugin.getGuiManager().openClaimsMenu(player);
            default -> giveClaimTool(player);
        }

        return true;
    }

    private void reloadPlugin(CommandSender sender) {
        if (!sender.hasPermission("onlyclaim.admin.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        // Ricarica la config principale di Bukkit
        plugin.reloadConfig();

        // Ricarica i file gestiti dal tuo ConfigManager (config e lang)
        plugin.getConfigManager().reloadAll();

        sender.sendMessage(plugin.getConfigManager().getMessage("plugin-reloaded"));

        if (sender instanceof Player p) {
            plugin.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return null;

        if (args.length == 1) {
            List<String> subCommands = List.of("create", "confirm", "delete", "remove", "subclaim", "view", "rename", "help", "flag", "rtp", "reload");
            return filterTabs(subCommands, args[0]);
        }

        if (args.length == 2) {
            if (List.of("delete", "remove", "rename", "flag").contains(args[0].toLowerCase())) {
                List<String> claimNames = plugin.getClaimManager().getAllClaims().stream()
                        .filter(c -> c.getOwner().equals(player.getUniqueId()) || player.hasPermission("onlyclaim.admin"))
                        .map(Claim::getName)
                        .collect(Collectors.toList());
                return filterTabs(claimNames, args[1]);
            }
        }

        if (args[0].equalsIgnoreCase("flag")) {
            if (args.length == 3) {
                return filterTabs(List.of("tnt", "interact", "block-break", "block-place"), args[2]);
            }
            if (args.length == 4) {
                return filterTabs(List.of("true", "false"), args[3]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> filterTabs(List<String> list, String input) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void setClaimFlag(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getConfigManager().getMessage("flag-usage"));
            return;
        }

        Claim claim = plugin.getClaimManager().getClaimByName(args[1]);
        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-not-found"));
            return;
        }

        if (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("onlyclaim.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        String flag = args[2].toLowerCase();
        boolean value = Boolean.parseBoolean(args[3]);

        List<String> validFlags = List.of("tnt", "interact", "block-break", "block-place");
        if (!validFlags.contains(flag)) {
            player.sendMessage("§cFlag non valida! Usa: tnt, interact, block-break, block-place");
            return;
        }

        claim.setFlag(flag, value);
        plugin.getClaimManager().saveClaims();

        String msg = plugin.getConfigManager().getMessage("flag-updated")
                .replace("{flag}", flag)
                .replace("{value}", String.valueOf(value))
                .replace("{name}", claim.getName());
        player.sendMessage(msg);
        plugin.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME);
    }

    private void createNamedClaim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("create-usage"));
            return;
        }
        String customName = args[1];
        if (plugin.getClaimManager().exists(customName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-already-exists"));
            return;
        }

        plugin.setPendingName(player.getUniqueId(), customName);
        giveClaimTool(player);

        String msg = plugin.getConfigManager().getMessage("pending-name-set")
                .replace("{name}", customName);
        player.sendMessage(msg);
    }

    private void confirmClaim(Player player) {
        String finalName = plugin.getPendingName(player.getUniqueId());

        if (finalName == null) {
            String format = plugin.getConfigManager().getMessageRaw("default-claim-name-format");
            String baseName = format.replace("{player}", player.getName());

            finalName = baseName;
            int count = 1;

            while (plugin.getClaimManager().exists(finalName)) {
                finalName = baseName + "_" + count;
                count++;
            }
        }

        processClaimCreation(player, finalName);
        plugin.clearPendingName(player.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private void processClaimCreation(Player player, String name) {
        Location[] selection = plugin.getSelection(player.getUniqueId());
        if (selection == null || selection[0] == null || selection[1] == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("select-points-first"));
            plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        int area = calculateArea(selection[0], selection[1]);
        if (!plugin.getClaimManager().canClaimMore(player, area)) {
            String maxBlocks = String.valueOf(getPlayerMaxChunks(player) * 256);
            String title = plugin.getConfigManager().getMessageRaw("limit-exceeded");
            String sub = plugin.getConfigManager().getMessageRaw("limit-exceeded-sub").replace("{max}", maxBlocks);

            player.sendTitle(title, sub, 10, 70, 20);
            plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        if (isAreaOverlapping(selection[0], selection[1])) {
            player.sendMessage(plugin.getConfigManager().getMessage("area-overlapping"));
            plugin.playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        Claim newClaim = new Claim(player.getUniqueId(), name, selection[0], selection[1], false);
        plugin.getClaimManager().addClaim(newClaim);

        player.sendTitle(plugin.getConfigManager().getMessageRaw("claim-created"), "§f" + name, 10, 40, 10);
        plugin.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        removeClaimTool(player);
        plugin.clearSelection(player.getUniqueId());
    }

    private void deleteClaimByName(Player player, String[] args) {
        Claim targetClaim;

        if (args.length < 2) {
            targetClaim = plugin.getClaimManager().getClaimAt(player.getLocation());
            if (targetClaim == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("delete-usage"));
                return;
            }
        } else {
            targetClaim = plugin.getClaimManager().getClaimByName(args[1]);
        }

        if (targetClaim == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-not-found"));
            return;
        }

        if (!targetClaim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("onlyclaim.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        String name = targetClaim.getName();
        plugin.getClaimManager().removeClaim(targetClaim);
        player.sendMessage(plugin.getConfigManager().getMessage("claim-deleted").replace("{name}", name));
        plugin.playSound(player, Sound.ENTITY_BLAZE_HURT);
        plugin.stopVisualizer(player.getUniqueId());
    }

    private void viewClaim(Player player, String[] args) {
        Claim claim;
        if (args.length < 2) {
            claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        } else {
            claim = plugin.getClaimManager().getClaimByName(args[1]);
        }

        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-not-found"));
            return;
        }

        // Visualizzazione (Particle)
        Location loc1 = new Location(player.getWorld(), claim.getMinX(), player.getLocation().getY(), claim.getMinZ());
        Location loc2 = new Location(player.getWorld(), claim.getMaxX(), player.getLocation().getY(), claim.getMaxZ());
        plugin.startVisualizer(player, loc1, loc2, Color.LIME);

        player.sendMessage(plugin.getConfigManager().getMessage("viewing-claim").replace("{name}", claim.getName()));
    }

    private void teleportToClaim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("home-usage"));
            return;
        }

        Claim claim = plugin.getClaimManager().getClaimByName(args[1]);
        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-not-found"));
            return;
        }

        // Controllo permessi specifico
        String perm = claim.isSubClaim() ? "onlyclaim.tp.subclaim" : "onlyclaim.tp.claim";
        if (!player.hasPermission(perm) && !claim.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        // Teletrasporto al centro del claim (o potresti aggiungere un campo 'spawn' alla classe Claim)
        Location center = claim.getCenter(player.getWorld());
        player.teleport(center.add(0.5, 1, 0.5));
        player.sendMessage(plugin.getConfigManager().getMessage("teleported-to").replace("{name}", claim.getName()));
        plugin.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    @SuppressWarnings("deprecation")
    private void giveClaimTool(Player player) {
        if (!player.hasPermission("onlyclaim.command.tool")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        if (!hasHotbarSpace(player)) {
            player.sendMessage(plugin.getConfigManager().getMessage("hotbar-full"));
            return;
        }

        ItemStack tool = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getMessageRaw("claim-tool-name"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessageRaw("claim-tool-lore"));
            meta.setLore(lore);
            tool.setItemMeta(meta);
        }

        player.getInventory().addItem(tool);
        player.sendMessage(plugin.getConfigManager().getMessage("tool-received"));
        plugin.playSound(player, Sound.ENTITY_ITEM_PICKUP);
    }

    @SuppressWarnings("deprecation")
    private void confirmSubClaim(Player player) {
        Location[] selection = plugin.getSelection(player.getUniqueId());
        if (selection == null || selection[0] == null || selection[1] == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("select-points-first"));
            return;
        }

        Claim parentClaim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (parentClaim == null || !parentClaim.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-your-claim"));
            return;
        }

        if (!checkSubClaimLimits(player, parentClaim)) {
            player.sendMessage(plugin.getConfigManager().getMessage("max-subclaims-reached"));
            return;
        }

        if (!parentClaim.contains(selection[0]) || !parentClaim.contains(selection[1])) {
            player.sendMessage(plugin.getConfigManager().getMessage("subclaim-out-of-bounds"));
            return;
        }

        Claim subClaim = new Claim(player.getUniqueId(), "Sottoclaim", selection[0], selection[1], true);
        parentClaim.addSubClaim(subClaim);
        plugin.getClaimManager().saveClaims();

        player.sendTitle(plugin.getConfigManager().getMessageRaw("subclaim-created"), "§7Area protetta", 10, 40, 10);
        plugin.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        removeClaimTool(player);
        plugin.clearSelection(player.getUniqueId());
    }

    private void renameClaim(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("rename-usage"));
            return;
        }

        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null || (!claim.getOwner().equals(player.getUniqueId()) && !player.hasPermission("onlyclaim.admin"))) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        claim.setName(args[1]);
        plugin.getClaimManager().saveClaims();
        player.sendMessage(plugin.getConfigManager().getMessage("claim-renamed").replace("{name}", args[1]));
        plugin.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    @SuppressWarnings("deprecation")
    private void removeClaimTool(Player player) {
        String toolName = plugin.getConfigManager().getMessageRaw("claim-tool-name");
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.GOLDEN_HOE && item.hasItemMeta()) {
                if (item.getItemMeta().getDisplayName().equals(toolName)) {
                    player.getInventory().setItem(i, null);
                    return;
                }
            }
        }
    }

    private boolean hasHotbarSpace(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) return true;
        }
        return false;
    }

    private boolean checkSubClaimLimits(Player player, Claim parent) {
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return false;
        int maxSub = 0;
        boolean allowed = false;
        for (String key : groups.getKeys(false)) {
            String perm = groups.getString(key + ".permission");
            if (perm != null && player.hasPermission(perm)) {
                allowed = groups.getBoolean(key + ".allow-subclaims", false);
                maxSub = Math.max(maxSub, groups.getInt(key + ".max-subclaims", 0));
            }
        }
        return allowed && parent.getSubClaims().size() < maxSub;
    }

    private int getPlayerMaxChunks(Player player) {
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        int max = 0;
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                String perm = groups.getString(key + ".permission");
                if (perm != null && player.hasPermission(perm)) {
                    max = Math.max(max, groups.getInt(key + ".max-chunks"));
                }
            }
        }
        return max;
    }

    private int calculateArea(Location p1, Location p2) {
        return (Math.abs(p1.getBlockX() - p2.getBlockX()) + 1) * (Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1);
    }

    private boolean isAreaOverlapping(Location p1, Location p2) {
        return plugin.getClaimManager().getClaimAt(p1) != null || plugin.getClaimManager().getClaimAt(p2) != null;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-header"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-tool"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-create"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-confirm"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-delete"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-subclaim"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-view"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-rename"));
        plugin.playSound(player, Sound.BLOCK_CHEST_OPEN);
    }
}