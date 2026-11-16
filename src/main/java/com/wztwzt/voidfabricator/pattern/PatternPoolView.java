package com.wztwzt.voidfabricator.pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Compact client projection of one independent recipe pool. */
public final class PatternPoolView {

    private int index;
    private ItemStack machine;
    private ItemStack preview;
    private String machineName = "";
    private String status = "no_machine";
    private int patternCount;
    private int enabledCount;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ItemStack getMachine() {
        return machine == null ? null : machine.copy();
    }

    public void setMachine(ItemStack machine) {
        this.machine = machine == null ? null : machine.copy();
    }

    public ItemStack getPreview() {
        return preview == null ? getMachine() : preview.copy();
    }

    public void setPreview(ItemStack preview) {
        this.preview = preview == null ? null : preview.copy();
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName == null ? "" : machineName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null || status.isEmpty() ? "no_machine" : status;
    }

    public int getPatternCount() {
        return patternCount;
    }

    public void setPatternCount(int patternCount) {
        this.patternCount = patternCount;
    }

    public int getEnabledCount() {
        return enabledCount;
    }

    public void setEnabledCount(int enabledCount) {
        this.enabledCount = enabledCount;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Index", index);
        if (machine != null) {
            tag.setTag("Machine", machine.writeToNBT(new NBTTagCompound()));
        }
        if (preview != null) {
            tag.setTag("Preview", preview.writeToNBT(new NBTTagCompound()));
        }
        tag.setString("MachineName", machineName);
        tag.setString("Status", status);
        tag.setInteger("PatternCount", patternCount);
        tag.setInteger("EnabledCount", enabledCount);
        return tag;
    }

    public static PatternPoolView fromNBT(NBTTagCompound tag) {
        PatternPoolView view = new PatternPoolView();
        if (tag == null) {
            return view;
        }
        view.index = tag.getInteger("Index");
        if (tag.hasKey("Machine", 10)) {
            view.machine = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Machine"));
        }
        if (tag.hasKey("Preview", 10)) {
            view.preview = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Preview"));
        }
        view.machineName = tag.getString("MachineName");
        view.status = tag.getString("Status");
        if (view.status.isEmpty()) {
            view.status = "no_machine";
        }
        view.patternCount = tag.getInteger("PatternCount");
        view.enabledCount = tag.getInteger("EnabledCount");
        return view;
    }
}
