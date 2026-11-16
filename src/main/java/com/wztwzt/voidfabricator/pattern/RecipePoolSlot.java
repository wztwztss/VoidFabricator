package com.wztwzt.voidfabricator.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.wztwzt.voidfabricator.recipe.GTMachineScanner;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;
import com.wztwzt.voidfabricator.recipe.ScanResult;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

/** One machine-backed recipe pool stored on a single physical pattern. */
public final class RecipePoolSlot {

    private final int index;
    private ItemStack machine;
    private final List<PatternEntry> entries = new ArrayList<>();
    private ScanResult scan;
    private String status = "no_machine";

    public RecipePoolSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public ItemStack getMachine() {
        return machine == null ? null : machine.copy();
    }

    public String getMachineName() {
        return machine == null ? null : machine.getDisplayName();
    }

    public void setMachine(ItemStack newMachine) {
        machine = newMachine == null ? null : newMachine.copy();
        if (machine != null) {
            machine.stackSize = 1;
        }
        entries.clear();
        scan = null;
        status = machine == null ? "no_machine" : "no_entries";
    }

    public List<PatternEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public PatternEntry getEntry(int entryIndex) {
        return entryIndex >= 0 && entryIndex < entries.size() ? entries.get(entryIndex) : null;
    }

    public void addEntry(PatternEntry entry) {
        if (entry != null) {
            PatternEntry copy = entry.copy();
            copy.setPoolIndex(index);
            entries.add(copy);
        }
    }

    public PatternEntry removeEntry(int entryIndex) {
        return entryIndex >= 0 && entryIndex < entries.size() ? entries.remove(entryIndex) : null;
    }

    public int getEnabledCount() {
        int count = 0;
        for (PatternEntry entry : entries) {
            if (entry.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public ItemStack getPreview() {
        if (!entries.isEmpty() && entries.get(0)
            .getDisplay() != null) {
            return entries.get(0)
                .getDisplay();
        }
        return getMachine();
    }

    public ScanResult getScan() {
        return scan;
    }

    public String getStatus() {
        return status;
    }

    public int getSkippedTotal() {
        return scan == null ? 0 : scan.skippedTotal();
    }

    public void rescan(boolean fillEmpty, Set<String> deleted) {
        if (machine == null) {
            entries.clear();
            scan = null;
            status = "no_machine";
            return;
        }
        try {
            scan = GTMachineScanner.scan(machine);
        } catch (RuntimeException exception) {
            scan = null;
            status = "scan_error";
            return;
        }

        Map<String, RecipeSnapshot> byKey = new LinkedHashMap<>();
        for (RecipeSnapshot snapshot : scan.recipes()) {
            byKey.put(
                snapshot.getKey()
                    .value(),
                snapshot);
        }

        List<PatternEntry> next = new ArrayList<>();
        Set<String> present = new HashSet<>();
        for (PatternEntry old : entries) {
            RecipeSnapshot snapshot = byKey.get(old.getKey());
            if (snapshot == null || deleted.contains(old.getKey())) {
                continue;
            }
            PatternEntry keep = old.copy();
            keep.setPoolIndex(index);
            present.add(keep.getKey());
            next.add(keep);
        }
        if (fillEmpty) {
            for (RecipeSnapshot snapshot : scan.recipes()) {
                String key = snapshot.getKey()
                    .value();
                if (deleted.contains(key) || present.contains(key)) {
                    continue;
                }
                PatternEntry entry = buildEntry(snapshot);
                if (entry != null) {
                    entry.setPoolIndex(index);
                    next.add(entry);
                    present.add(key);
                }
            }
        }
        entries.clear();
        entries.addAll(next);
        status = entries.isEmpty() ? "no_entries" : "ready";
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (machine != null) {
            tag.setTag("Machine", machine.writeToNBT(new NBTTagCompound()));
        }
        NBTTagList entryList = new NBTTagList();
        for (PatternEntry entry : entries) {
            entryList.appendTag(entry.toNBT());
        }
        tag.setTag("Entries", entryList);
        return tag;
    }

    public void load(NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        if (tag.hasKey("Machine", 10)) {
            machine = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Machine"));
            if (machine != null) {
                machine.stackSize = 1;
            }
        }
        NBTTagList entryList = tag.getTagList("Entries", 10);
        for (int entryIndex = 0; entryIndex < entryList.tagCount(); entryIndex++) {
            PatternEntry entry = PatternEntry.fromNBT(entryList.getCompoundTagAt(entryIndex));
            if (entry != null) {
                entry.setPoolIndex(index);
                entries.add(entry);
            }
        }
        status = machine == null ? "no_machine" : (entries.isEmpty() ? "no_entries" : "ready");
    }

    private static PatternEntry buildEntry(RecipeSnapshot snapshot) {
        if (snapshot.getOutputs()
            .isEmpty()) {
            return null;
        }
        IAEStack<?> first = snapshot.getOutputs()
            .get(0);
        PatternEntry entry = new PatternEntry();
        entry.setKey(
            snapshot.getKey()
                .value());
        entry.setEnabled(true);
        entry.setInputs(
            snapshot.getInputs()
                .size());
        entry.setVirtualInputs(
            snapshot.getVirtualInputs()
                .size());
        entry.setOutputs(
            snapshot.getOutputs()
                .size());
        entry.setEuPerTick(snapshot.getEuPerTick());
        entry.setDuration(snapshot.getDuration());
        entry.setSource(snapshot.getSourceId());
        if (first instanceof IAEItemStack item) {
            ItemStack source = item.getItemStack();
            if (source == null) {
                return null;
            }
            ItemStack display = source.copy();
            display.stackSize = (int) Math.max(1, Math.min(64, item.getStackSize()));
            entry.setDisplay(display);
            entry.setFilterKind(PatternFilter.KIND_ITEM);
            entry.setFilterKey(itemKey(display));
            entry.setFilterName(display.getDisplayName());
        } else if (first instanceof IAEFluidStack fluid) {
            FluidStack fluidStack = fluid.getFluidStack();
            if (fluidStack == null || fluidStack.getFluid() == null) {
                return null;
            }
            entry.setDisplayFluid(fluidStack.copy());
            entry.setFilterKind(PatternFilter.KIND_FLUID);
            entry.setFilterKey(fluidKey(fluidStack));
            entry.setFilterName(fluidStack.getLocalizedName());
        } else {
            return null;
        }
        return entry;
    }

    private static String itemKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        return "item:" + String.valueOf(name) + ':' + stack.getItemDamage();
    }

    private static String fluidKey(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            return null;
        }
        String name = FluidRegistry.getFluidName(stack);
        return name == null ? null : "fluid:" + name;
    }
}
