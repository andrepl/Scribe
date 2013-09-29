package com.norcode.bukkit.scribe.api;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

public abstract class ScribeAnvilInventory {

    protected static Class<? extends ScribeAnvilInventory> implementation;

    public static void initialize(Server server) throws ClassNotFoundException {
        String packageName = server.getClass().getPackage().getName();
        // Get full package string of CraftServer.
        // org.bukkit.craftbukkit.versionstring (or for pre-refactor, just org.bukkit.craftbukkit
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        final Class<?> clazz = Class.forName("com.norcode.bukkit.scribe." + version + ".ScribeAnvilInventory");
        if (ScribeAnvilInventory.class.isAssignableFrom(clazz)) { // Make sure it actually implements
            implementation = (Class<? extends ScribeAnvilInventory>) clazz;
        }
    }

    public static ScribeAnvilInventory wrap(AnvilInventory inv) {
        try {
            return implementation.getConstructor(AnvilInventory.class).newInstance(inv);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected AnvilInventory inv;

    public ScribeAnvilInventory(AnvilInventory inv) {
        this.inv = inv;
    }

    public abstract ItemStack getResult();

    public ItemStack getFirst() {
        return inv.getItem(0);
    }

    public ItemStack getSecond() {
        return inv.getItem(1);
    }

    public void setFirst(ItemStack stack) {
        inv.setItem(0, stack);
    };


    public void setSecond(ItemStack stack) {
        inv.setItem(1, stack);
    };

    public abstract void setResult(ItemStack stack);

    public abstract void setCost(int cost);

    public abstract void updatePlayer(Player player);

}
