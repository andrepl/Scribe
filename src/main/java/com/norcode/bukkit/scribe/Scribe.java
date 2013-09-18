package com.norcode.bukkit.scribe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.h31ix.updater.Updater;
import net.h31ix.updater.Updater.UpdateType;
import net.minecraft.server.v1_6_R2.ContainerAnvil;
import net.minecraft.server.v1_6_R2.ContainerAnvilInventory;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
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
            updater = new Updater(this, "scribe", this.getFile(), UpdateType.DEFAULT, true);
        } else if (autoUpdate.equals("false")) {
            getLogger().info("Auto-updater is disabled.  Skipping check.");
        } else {
            updater = new Updater(this, "scribe", this.getFile(), UpdateType.NO_DOWNLOAD, true);
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
                    ItemStack first = ai.getItem(0);
                    ItemStack second = ai.getItem(1);
                    net.minecraft.server.v1_6_R2.ItemStack nmsResult = ((CraftInventoryAnvil)ai).getResultInventory().getItem(0);
                    ItemStack result = nmsResult == null ? null : CraftItemStack.asCraftMirror(nmsResult);  
                    if (first != null && first.getType().equals(Material.BOOK_AND_QUILL) && second != null && result == null) {
                        
                        ContainerAnvilInventory nmsInv = (ContainerAnvilInventory) ((CraftInventoryAnvil) ai).getInventory();
                        ItemStack resultStack = new ItemStack(Material.ENCHANTED_BOOK);
                        try {
                            Field containerField = ContainerAnvilInventory.class.getDeclaredField("a");
                            containerField.setAccessible(true);
                            ContainerAnvil anvil = (ContainerAnvil) containerField.get(nmsInv);
                            float pct = (second.getType().getMaxDurability() - second.getDurability()) / second.getType().getMaxDurability();
                            if ((int) Math.floor(pct*100) > getConfig().getInt("max-durability", 100)) {
                                anvil.a = 40;
                                String msg = getConfig().getString("messages.not-damaged-enough", "");
                                if (!msg.equals("")) {
                                    player.sendMessage(msg);
                                }
                            } else if ((int) Math.floor(pct*100) < getConfig().getInt("min-durability", 0)) {
                                anvil.a = 40;
                                String msg = getConfig().getString("messages.too-damaged", "");
                                if (!msg.equals("")) {
                                    player.sendMessage(msg);
                                }
                            } else {
                                ScribeResult scribeResult = new ScribeResult(second);
                                debug("Setting Scribe result: " + scribeResult.getEnchantments());
                                anvil.a = scribeResult.getCost(); 
                                EnchantmentStorageMeta meta = ((EnchantmentStorageMeta)resultStack.getItemMeta());
                                for (Map.Entry<Enchantment, Integer> entry: scribeResult.getEnchantments().entrySet()) {
                                    meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
                                }
                                resultStack.setItemMeta(meta);
                                ((CraftInventoryAnvil)ai).getResultInventory().setItem(0, CraftItemStack.asNMSCopy(resultStack));
                                ((CraftPlayer) player).getHandle().setContainerData(anvil, 0, anvil.a);    
                            }
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, 0);
    }
}
