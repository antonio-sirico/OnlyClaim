package me.siryq.onlyclaim;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClaimCommand implements CommandExecutor {

    private final OnlyClaim plugin;

    public ClaimCommand(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessageRaw("only-players"));
            return true;
        }

        if (args.length == 0) {
            giveClaimTool(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "confirm" -> confirmClaim(player);
            case "subclaim" -> confirmSubClaim(player);
            case "view" -> viewClaim(player);
            case "rename" -> renameClaim(player, args);
            case "help" -> sendHelp(player);
            default -> giveClaimTool(player);
        }

        return true;
    }

    private void viewClaim(Player player) {
        Claim claim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-any-claim"));
            return;
        }

        // Creiamo due angoli basandoci sui dati del claim salvato
        // Usiamo la Y del giocatore per far apparire le particelle ad altezza vista
        Location loc1 = new Location(player.getWorld(), claim.getMinX(), player.getLocation().getY(), claim.getMinZ());
        Location loc2 = new Location(player.getWorld(), claim.getMaxX(), player.getLocation().getY(), claim.getMaxZ());

        plugin.spawnBorderParticles(player, loc1, loc2);
        player.sendMessage(plugin.getConfigManager().getMessage("prefix") + "§eConfini del territorio visualizzati!");
    }

    private void sendDisplay(Player player, String titleKey, String subKey, String placeholder, String replacement) {
        String title = plugin.getConfigManager().getMessageRaw(titleKey);
        String sub = plugin.getConfigManager().getMessageRaw(subKey);

        if (placeholder != null && replacement != null) {
            sub = sub.replace(placeholder, replacement);
        }

        player.sendTitle(title, sub, 10, 70, 20);
    }

    private void giveClaimTool(Player player) {
        if (!player.hasPermission("onlyclaim.command.tool")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        boolean hasSpace = false;
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                hasSpace = true;
                break;
            }
        }

        if (!hasSpace) {
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
    }

    private void confirmClaim(Player player) {
        Location[] selection = plugin.getSelection(player.getUniqueId());
        if (selection == null || selection[0] == null || selection[1] == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("select-points-first"));
            return;
        }

        int area = calculateArea(selection[0], selection[1]);
        if (!plugin.getClaimManager().canClaimMore(player, area)) {
            sendDisplay(player, "limit-exceeded", "limit-exceeded-sub", "{max}", String.valueOf(getPlayerMaxChunks(player)));
            return;
        }

        if (isAreaOverlapping(selection[0], selection[1])) {
            sendDisplay(player, "area-overlapping", "area-overlapping-sub", null, null);
            return;
        }

        Claim newClaim = new Claim(player.getUniqueId(), player.getName() + "_Territorio", selection[0], selection[1], false);
        plugin.getClaimManager().addClaim(newClaim);

        sendDisplay(player, "claim-created", "", null, null);
        plugin.clearSelection(player.getUniqueId());
    }

    private void confirmSubClaim(Player player) {
        Location[] selection = plugin.getSelection(player.getUniqueId());
        if (selection == null || selection[0] == null || selection[1] == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("select-points-first"));
            return;
        }

        Claim parentClaim = plugin.getClaimManager().getClaimAt(player.getLocation());
        if (parentClaim == null || !parentClaim.getOwner().equals(player.getUniqueId())) {
            sendDisplay(player, "not-in-your-claim", "not-in-your-claim-sub", null, null);
            return;
        }

        if (!checkSubClaimLimits(player, parentClaim)) {
            sendDisplay(player, "max-subclaims-reached", "", null, null);
            return;
        }

        if (!parentClaim.contains(selection[0]) || !parentClaim.contains(selection[1])) {
            player.sendMessage(plugin.getConfigManager().getMessage("subclaim-out-of-bounds"));
            return;
        }

        Claim subClaim = new Claim(player.getUniqueId(), "Sottoclaim", selection[0], selection[1], true);
        parentClaim.addSubClaim(subClaim);

        plugin.getClaimManager().saveClaims();
        sendDisplay(player, "subclaim-created", "", null, null);
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
    }

    private boolean checkSubClaimLimits(Player player, Claim parent) {
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        if (groups == null) return false;

        boolean canCreate = false;
        int maxSub = 0;

        for (String key : groups.getKeys(false)) {
            String perm = groups.getString(key + ".permission");
            if (perm != null && player.hasPermission(perm)) {
                if (groups.getBoolean(key + ".allow-subclaims", false)) canCreate = true;
                int m = groups.getInt(key + ".max-subclaims", 0);
                if (m > maxSub) maxSub = m;
            }
        }
        return canCreate && parent.getSubClaims().size() < maxSub;
    }

    private int getPlayerMaxChunks(Player player) {
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("groups");
        int max = 0;
        if (groups != null) {
            for (String key : groups.getKeys(false)) {
                String perm = groups.getString(key + ".permission");
                if (perm != null && player.hasPermission(perm)) {
                    int val = groups.getInt(key + ".max-chunks");
                    if (val > max) max = val;
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
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-confirm"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-subclaim"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-view"));
        player.sendMessage(plugin.getConfigManager().getMessageRaw("help-rename"));
    }
}