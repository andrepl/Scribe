package com.norcode.bukkit.scribe.v1_7_R4;

import net.minecraft.server.v1_7_R4.ContainerAnvil;
import net.minecraft.server.v1_7_R4.ContainerAnvilInventory;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftInventoryAnvil;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
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
        nmsInventory = (ContainerAnvilInventory) craftAnvil.getInventory();

        try {
            containerField = ContainerAnvilInventory.class.getDeclaredField("a");
            containerField.setAccessible(true);
            container = (ContainerAnvil) containerField.get(nmsInventory);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public ItemStack getResult() {
        net.minecraft.server.v1_7_R4.ItemStack nmsResult = craftAnvil.getResultInventory().getItem(0);
        return nmsResult == null ? null : CraftItemStack.asCraftMirror(nmsResult);
    }

    @Override
    public void setResult(ItemStack stack) {
        craftAnvil.getResultInventory().setItem(0, CraftItemStack.asNMSCopy(stack));
    }

    @Override
    public void setCost(int cost) {
        container.a = cost;

    }

    @Override
    public void updatePlayer(Player player) {
        ((CraftPlayer) player).getHandle().setContainerData(container, 0, container.a);
    }

}
