package me.siryq.onlyclaim;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class GUIManager implements org.bukkit.event.Listener {
    private final OnlyClaim plugin;

    public GUIManager(OnlyClaim plugin) {
        this.plugin = plugin;
    }

    /**
     * Apre il menu principale con i territori (Grass Blocks)
     */
    public void openClaimsMenu(Player player) {
        String rawTitle = plugin.getConfigManager().getMessageRaw("gui-main-title");
        String title = ChatColor.translateAlternateColorCodes('&', rawTitle.isEmpty() ? "&8I tuoi Territori" : rawTitle);

        Inventory gui = Bukkit.createInventory(null, 18, title);

        List<Claim> myClaims = plugin.getClaimManager().getAllClaims().stream()
                .filter(c -> c.getOwner().equals(player.getUniqueId()) && !c.isSubClaim())
                .collect(Collectors.toList());

        // Calcolo chunk totali usati e limite massimo dal config
        int totalUsedChunks = myClaims.stream()
                .mapToInt(c -> (int) Math.ceil(c.getArea() / 256.0))
                .sum();

        int maxLimit = plugin.getClaimManager().getMaxChunksForPlayer(player);
        String maxLimitStr = (maxLimit == -1) ? "∞" : String.valueOf(maxLimit);

        for (int i = 0; i < Math.min(myClaims.size(), 9); i++) {
            Claim c = myClaims.get(i);
            int chunkCount = (int) Math.ceil(c.getArea() / 256.0);

            String itemName = plugin.getConfigManager().getMessageRaw("gui-item-claim-name")
                    .replace("{name}", c.getName());

            List<String> lore = plugin.getConfigManager().getMessageList("gui-item-claim-lore").stream()
                    .map(s -> ChatColor.translateAlternateColorCodes('&', s
                            .replace("{chunks}", String.valueOf(chunkCount))
                            .replace("{total_used}", String.valueOf(totalUsedChunks))
                            .replace("{max_chunks}", maxLimitStr)
                            .replace("{subs}", String.valueOf(c.getSubClaims().size()))))
                    .collect(Collectors.toList());

            gui.setItem(i, createItem(Material.GRASS_BLOCK, itemName, lore));
        }

        setupNavigation(gui, false);
        player.openInventory(gui);
    }

    /**
     * Apre il menu dei sottoclaim (Dirt)
     */
    public void openSubClaimsMenu(Player player, Claim parent) {
        String rawTitle = plugin.getConfigManager().getMessageRaw("gui-sub-title").replace("{parent}", parent.getName());
        String title = ChatColor.translateAlternateColorCodes('&', rawTitle.isEmpty() ? "&8Sottoclaim" : rawTitle);

        Inventory gui = Bukkit.createInventory(null, 18, title);

        List<Claim> subs = parent.getSubClaims();
        for (int i = 0; i < Math.min(subs.size(), 9); i++) {
            Claim sub = subs.get(i);
            int subChunks = (int) Math.ceil(sub.getArea() / 256.0);

            String itemName = plugin.getConfigManager().getMessageRaw("gui-item-sub-name")
                    .replace("{name}", sub.getName());

            List<String> lore = plugin.getConfigManager().getMessageList("gui-item-sub-lore").stream()
                    .map(s -> ChatColor.translateAlternateColorCodes('&', s
                            .replace("{chunks}", String.valueOf(subChunks))
                            .replace("{x}", String.valueOf(sub.getMinX()))
                            .replace("{z}", String.valueOf(sub.getMinZ()))))
                    .collect(Collectors.toList());

            gui.setItem(i, createItem(Material.DIRT, itemName, lore));
        }

        setupNavigation(gui, true);
        player.openInventory(gui);
    }

    /**
     * Configura la riga di navigazione (slot 9-17)
     */
    private void setupNavigation(Inventory gui, boolean isSubMenu) {
        gui.setItem(9, createItem(Material.ARROW, plugin.getConfigManager().getMessageRaw("gui-nav-prev"), null));

        if (isSubMenu) {
            gui.setItem(13, createItem(Material.DARK_OAK_DOOR, plugin.getConfigManager().getMessageRaw("gui-nav-back"), null));
        } else {
            gui.setItem(13, createItem(Material.BARRIER, plugin.getConfigManager().getMessageRaw("gui-nav-close"), null));
        }

        gui.setItem(17, createItem(Material.ARROW, plugin.getConfigManager().getMessageRaw("gui-nav-next"), null));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        String title = event.getView().getTitle();
        String mainT = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessageRaw("gui-main-title")));
        String subT = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessageRaw("gui-sub-title"))).split(" ")[0];

        // Controllo se l'inventario cliccato fa parte del plugin
        if (ChatColor.stripColor(title).contains(mainT) || ChatColor.stripColor(title).contains(subT)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;

            Player player = (Player) event.getWhoClicked();
            Material type = event.getCurrentItem().getType();

            if (type == Material.GRASS_BLOCK) {
                String claimName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
                Claim parent = plugin.getClaimManager().getClaimByName(claimName);
                if (parent != null) openSubClaimsMenu(player, parent);
            }
            else if (type == Material.BARRIER) {
                player.closeInventory();
            }
            else if (type == Material.DARK_OAK_DOOR) {
                openClaimsMenu(player);
            }
        }
    }
}