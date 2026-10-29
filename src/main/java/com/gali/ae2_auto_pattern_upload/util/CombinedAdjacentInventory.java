package com.gali.ae2_auto_pattern_upload.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

public class CombinedAdjacentInventory implements ISidedInventory {

    private final List<Entry> entries = new ArrayList<>();
    private final List<IInventory> inventories = new ArrayList<>();

    public void addInventory(IInventory inventory, int accessSide) {
        if (inventory == null) return;

        if (!inventories.contains(inventory)) {
            inventories.add(inventory);
        }

        if (inventory instanceof ISidedInventory sided) {
            for (int slot : sided.getAccessibleSlotsFromSide(accessSide)) {
                entries.add(new Entry(inventory, slot, accessSide));
            }
        } else {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                entries.add(new Entry(inventory, slot, accessSide));
            }
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public int getSizeInventory() {
        return entries.size();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        Entry entry = getEntry(index);
        return entry == null ? null : entry.inventory.getStackInSlot(entry.slot);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        Entry entry = getEntry(index);
        return entry == null ? null : entry.inventory.decrStackSize(entry.slot, count);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        Entry entry = getEntry(index);
        return entry == null ? null : entry.inventory.getStackInSlotOnClosing(entry.slot);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        Entry entry = getEntry(index);
        if (entry != null) {
            entry.inventory.setInventorySlotContents(entry.slot, stack);
        }
    }

    @Override
    public String getInventoryName() {
        return inventories.isEmpty() ? "container.inventory"
            : inventories.get(0)
                .getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return !inventories.isEmpty() && inventories.get(0)
            .hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        int limit = 64;
        for (IInventory inventory : inventories) {
            limit = Math.min(limit, inventory.getInventoryStackLimit());
        }
        return limit;
    }

    @Override
    public void markDirty() {
        for (IInventory inventory : inventories) {
            inventory.markDirty();
        }
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        for (IInventory inventory : inventories) {
            if (!inventory.isUseableByPlayer(player)) return false;
        }
        return true;
    }

    @Override
    public void openInventory() {
        for (IInventory inventory : inventories) {
            inventory.openInventory();
        }
    }

    @Override
    public void closeInventory() {
        for (IInventory inventory : inventories) {
            inventory.closeInventory();
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        Entry entry = getEntry(index);
        return entry != null && entry.inventory.isItemValidForSlot(entry.slot, stack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        int[] slots = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, int side) {
        Entry entry = getEntry(index);
        if (entry == null) return false;
        if (entry.inventory instanceof ISidedInventory sided) {
            return sided.canInsertItem(entry.slot, stack, entry.accessSide);
        }
        return entry.inventory.isItemValidForSlot(entry.slot, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, int side) {
        Entry entry = getEntry(index);
        if (entry == null) return false;
        if (entry.inventory instanceof ISidedInventory sided) {
            return sided.canExtractItem(entry.slot, stack, entry.accessSide);
        }
        return true;
    }

    private Entry getEntry(int index) {
        if (index < 0 || index >= entries.size()) return null;
        return entries.get(index);
    }

    private static final class Entry {

        private final IInventory inventory;
        private final int slot;
        private final int accessSide;

        private Entry(IInventory inventory, int slot, int accessSide) {
            this.inventory = inventory;
            this.slot = slot;
            this.accessSide = accessSide;
        }
    }
}
