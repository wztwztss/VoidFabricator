package com.wztwzt.voidfabricator.gui;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.wztwzt.voidfabricator.recipe.GTMachineScanner;

/** A one-slot handler whose contents live inside the pool pattern's machine tag. */
public final class PatternMachineHandler implements IItemHandlerModifiable {

    private final PatternGuiSession session;

    public PatternMachineHandler(PatternGuiSession session) {
        this.session = session;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? session.getMachine() : null;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack == null || stack.stackSize <= 0 || getStackInSlot(0) != null || !isItemValid(0, stack)) {
            return stack;
        }
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (!simulate) {
            session.setMachine(single);
        }
        if (stack.stackSize <= 1) {
            return null;
        }
        ItemStack remainder = stack.copy();
        remainder.stackSize--;
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0) {
            return null;
        }
        ItemStack current = getStackInSlot(0);
        if (current == null) {
            return null;
        }
        ItemStack extracted = current.copy();
        extracted.stackSize = 1;
        if (!simulate) {
            session.setMachine(null);
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot != 0 || !GTMachineScanner.isMachine(stack) || GTMachineScanner.getFingerprint(stack) == null) {
            return false;
        }
        // A block without a pool pattern has nowhere to store a machine.
        return !"pattern_required".equals(
            session.getView()
                .getStatus());
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot == 0) {
            session.setMachine(stack == null || stack.stackSize <= 0 ? null : stack);
        }
    }
}
