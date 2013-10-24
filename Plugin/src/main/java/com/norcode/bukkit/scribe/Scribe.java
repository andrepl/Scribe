package com.norcode.bukkit.scribe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import com.norcode.bukkit.scribe.api.ScribeAnvilInventory;

import net.gravitydevelopment.updater.Updater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class Scribe extends JavaPlugin implements Listener {
    private Updater updater;

    private class ScribeResult {
        ItemStack stack;
        int cost = -1;
        Map<Enchantment, Integer> enchantments = new HashMap<Enchantment, Integer>();
        public ScribeResult(ItemStack stack) {
            cost = getConfig().getInt("base-cost", 0);
            enchantments.clear();
            List<Entry<Enchantment, Integer>> entrySet = new ArrayList<Entry<Enchantment, Integer>>(stack.getEnchantments().entrySet());
            Collections.shuffle(entrySet);
            for (Entry<Enchantment, Integer> e: entrySet) {
                int cap = getConfig().getInt("max-level." + e.getKey().getName(), -1);
                int lvl = e.getValue();
                if (cap != 0) {
                    if (cap == -1) { cap = lvl; }
                    if (lvl > cap) { lvl = cap; }
                    int cpl = getConfig().getInt("enchantment-cost-per-level." + e.getKey().getName(), 0);
                    int tmpCost = getConfig().getInt("cost-per-enchantment", 0) + (cpl * lvl);
                    if (cost + tmpCost < 40) {
                        cost += tmpCost;
                        enchantments.put(e.getKey(), lvl);
                    }
                }
            }
        }

        public int getCost() {
            return cost;
        }
        public Map<Enchantment, Integer> getEnchantments() {
            return enchantments;
        }
    }


    public void doUpdater() {
        String autoUpdate = getConfig().getString("auto-update", "notify-only").toLowerCase();
        if (autoUpdate.equals("true")) {
            updater = new Updater(this, 60420, this.getFile(), Updater.UpdateType.DEFAULT, true);
        } else if (autoUpdate.equals("false")) {
            getLogger().info("Auto-updater is disabled.  Skipping check.");
        } else {
            updater = new Updater(this, 60420, this.getFile(), Updater.UpdateType.NO_DOWNLOAD, true);
        }
    }

    public void debug(String s) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info(s);
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        doUpdater();
        try {
            ScribeAnvilInventory.initialize(getServer());
        } catch (ClassNotFoundException e) {
            this.getLogger().severe("Could not find support for this craftbukkit version " + getServer().getBukkitVersion() + ".");
            this.getLogger().info("Check for updates at http://dev.bukktit.org/bukkit-plugins/scribe/");
            this.setEnabled(false);
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GOLD + "[Scribe]" + ChatColor.GRAY + " Configuration Reloaded.");
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (updater != null) {
            if (event.getPlayer().hasPermission("scribe.admin")) {
                final String playerName = event.getPlayer().getName();
                getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                    public void run() {
                        Player player = getServer().getPlayer(playerName);
                        if (player != null && player.isOnline()) {
                            switch (updater.getResult()) {
                            case UPDATE_AVAILABLE:
                                player.sendMessage("A new version of Scribe is available at http://dev.bukkit.org/server-mods/scribe/");
                                break;
                            case SUCCESS:
                                player.sendMessage("A new version of Scribe has been downloaded and will take effect when the server restarts.");
                                break;
                            default:
                                // nothing
                            }
                        }
                    }
                }, 20);
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            public void run() {
                if (event.getInventory() instanceof AnvilInventory) {
                    Player player = (Player) event.getWhoClicked();
                    if (getConfig().getBoolean("use-permissions")) {
                        if (!player.hasPermission("scribe.use")) {
                            debug(player.getName() + " has no permission to use scribe.");
                            return;
                        }
                    }
                    AnvilInventory ai = (AnvilInventory) event.getInventory();
                    ScribeAnvilInventory sInv = ScribeAnvilInventory.wrap(ai);
                    ItemStack first = sInv.getFirst();
                    ItemStack second = sInv.getSecond();
                    ItemStack result = sInv.getResult();
                    if (first != null && first.getType().equals(Material.BOOK_AND_QUILL) && second != null && result == null) {
                        ItemStack resultStack = new ItemStack(Material.ENCHANTED_BOOK);
                        float pct = (second.getType().getMaxDurability() - second.getDurability()) / second.getType().getMaxDurability();
                        if ((int) Math.floor(pct * 100) > getConfig().getInt("max-durability", 100)) {
                            sInv.setCost(40);
                            String msg = getConfig().getString("messages.not-damaged-enough", "");
                            if (!msg.equals("")) {
                                player.sendMessage(msg);
                            }
                        } else if ((int) Math.floor(pct * 100) < getConfig().getInt("min-durability", 0)) {
                            sInv.setCost(40);
                            String msg = getConfig().getString("messages.too-damaged", "");
                            if (!msg.equals("")) {
                                player.sendMessage(msg);
                            }
                        } else {
                            ScribeResult scribeResult = new ScribeResult(second);
                            debug("Setting Scribe result: " + scribeResult.getEnchantments());
                            EnchantmentStorageMeta meta = ((EnchantmentStorageMeta) resultStack.getItemMeta());
                            for (Map.Entry<Enchantment, Integer> entry : scribeResult.getEnchantments().entrySet()) {
                                meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                            }
                            resultStack.setItemMeta(meta);
                            sInv.setCost(scribeResult.getCost());
                            sInv.setResult(resultStack);
                            sInv.updatePlayer(player);
                        }
                    }
                }
            }
        }, 0);
    }
}
