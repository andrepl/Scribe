package com.norcode.bukkit.scribe.v1_6_R3;

import net.minecraft.server.v1_6_R3.ContainerAnvil;
import net.minecraft.server.v1_6_R3.ContainerAnvilInventory;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;

public class ScribeAnvilInventory extends com.norcode.bukkit.scribe.api.ScribeAnvilInventory {

    private CraftInventoryAnvil craftAnvil;
    private ContainerAnvilInventory nmsInventory;
    private Field containerField;
    ContainerAnvil container;

    public ScribeAnvilInventory(AnvilInventory inv) {
        super(inv);
        craftAnvil = (CraftInventoryAnvil) inv;
        try {
            containerField = ContainerAnvilInventory.class.getDeclaredField("a");
            containerField.setAccessible(true);
            container = (ContainerAnvil) containerField.get(nmsInventory);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        nmsInventory = (ContainerAnvilInventory) craftAnvil.getInventory();
    }

    @Override
    public ItemStack getResult() {
        net.minecraft.server.v1_6_R3.ItemStack nmsResult = craftAnvil.getResultInventory().getItem(0);
        return nmsResult == null ? null : CraftItemStack.asCraftMirror(nmsResult);
    }

    @Override
    public void setResult(ItemStack stack) {
        craftAnvil.getResultInventory().setItem(0, CraftItemStack.asNMSCopy(stack));
    }

    @Override
    public void setCost(Player player, int cost) {
        ((CraftPlayer) player).getHandle().setContainerData(container, 0, cost);
    }
}
