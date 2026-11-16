package com.wztwzt.voidfabricator.pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.GTValues;

/** One selected recipe reference stored in a pool pattern. */
public final class PatternEntry {

    private String key;
    private boolean enabled;
    private ItemStack display;
    private FluidStack displayFluid;
    private String filterKey;
    private int filterKind;
    private String filterName;
    private int inputs;
    private int virtualInputs;
    private int outputs;
    private int euPerTick;
    private int duration;
    private String source;
    private int poolIndex;
    private int globalIndex;

    public PatternEntry() {}

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ItemStack getDisplay() {
        return display == null ? null : display.copy();
    }

    public void setDisplay(ItemStack display) {
        this.display = display == null ? null : display.copy();
    }

    public FluidStack getDisplayFluid() {
        return displayFluid == null ? null : displayFluid.copy();
    }

    public void setDisplayFluid(FluidStack displayFluid) {
        this.displayFluid = displayFluid == null ? null : displayFluid.copy();
    }

    public String getFilterKey() {
        return filterKey;
    }

    public void setFilterKey(String filterKey) {
        this.filterKey = filterKey;
    }

    public int getFilterKind() {
        return filterKind;
    }

    public void setFilterKind(int filterKind) {
        this.filterKind = filterKind;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public int getInputs() {
        return inputs;
    }

    public void setInputs(int inputs) {
        this.inputs = inputs;
    }

    public int getVirtualInputs() {
        return virtualInputs;
    }

    public void setVirtualInputs(int virtualInputs) {
        this.virtualInputs = virtualInputs;
    }

    public int getOutputs() {
        return outputs;
    }

    public void setOutputs(int outputs) {
        this.outputs = outputs;
    }

    public int getEuPerTick() {
        return euPerTick;
    }

    public void setEuPerTick(int euPerTick) {
        this.euPerTick = euPerTick;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Voltage tier derived from the recipe's consumed EU/t. Used only for the
     * tier-column presentation copied from TenkeiTech; the stored recipe key and
     * execution semantics are unchanged.
     */
    public int getVoltageTier() {
        if (euPerTick <= 0) {
            return 0;
        }
        for (int tier = 0; tier < GTValues.V.length; tier++) {
            if (euPerTick <= GTValues.V[tier]) {
                return tier;
            }
        }
        return GTValues.V.length - 1;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getPoolIndex() {
        return poolIndex;
    }

    public void setPoolIndex(int poolIndex) {
        this.poolIndex = poolIndex;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex = globalIndex;
    }

    public PatternEntry copy() {
        PatternEntry copy = new PatternEntry();
        copy.key = key;
        copy.enabled = enabled;
        copy.display = display == null ? null : display.copy();
        copy.displayFluid = displayFluid == null ? null : displayFluid.copy();
        copy.filterKey = filterKey;
        copy.filterKind = filterKind;
        copy.filterName = filterName;
        copy.inputs = inputs;
        copy.virtualInputs = virtualInputs;
        copy.outputs = outputs;
        copy.euPerTick = euPerTick;
        copy.duration = duration;
        copy.source = source;
        copy.poolIndex = poolIndex;
        copy.globalIndex = globalIndex;
        return copy;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Key", key == null ? "" : key);
        tag.setBoolean("Enabled", enabled);
        if (display != null) {
            tag.setTag("Display", display.writeToNBT(new NBTTagCompound()));
        }
        if (displayFluid != null) {
            tag.setTag("DisplayFluid", displayFluid.writeToNBT(new NBTTagCompound()));
        }
        tag.setString("FilterKey", filterKey == null ? "" : filterKey);
        tag.setByte("FilterKind", (byte) filterKind);
        tag.setString("FilterName", filterName == null ? "" : filterName);
        tag.setInteger("Inputs", inputs);
        tag.setInteger("VirtualInputs", virtualInputs);
        tag.setInteger("Outputs", outputs);
        tag.setInteger("EUt", euPerTick);
        tag.setInteger("Duration", duration);
        tag.setString("Source", source == null ? "" : source);
        tag.setInteger("Pool", poolIndex);
        tag.setInteger("GlobalIndex", globalIndex);
        return tag;
    }

    public static PatternEntry fromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        String key = tag.getString("Key");
        if (key.isEmpty()) {
            return null;
        }
        PatternEntry entry = new PatternEntry();
        entry.key = key;
        entry.enabled = !tag.hasKey("Enabled") || tag.getBoolean("Enabled");
        if (tag.hasKey("Display", 10)) {
            entry.display = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Display"));
        }
        if (tag.hasKey("DisplayFluid", 10)) {
            entry.displayFluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("DisplayFluid"));
        }
        entry.filterKey = tag.getString("FilterKey");
        entry.filterKind = tag.getByte("FilterKind");
        entry.filterName = tag.getString("FilterName");
        entry.inputs = tag.getInteger("Inputs");
        entry.virtualInputs = tag.getInteger("VirtualInputs");
        entry.outputs = tag.getInteger("Outputs");
        entry.euPerTick = tag.getInteger("EUt");
        entry.duration = tag.getInteger("Duration");
        entry.source = tag.getString("Source");
        entry.poolIndex = tag.getInteger("Pool");
        entry.globalIndex = tag.getInteger("GlobalIndex");
        return entry;
    }
}
