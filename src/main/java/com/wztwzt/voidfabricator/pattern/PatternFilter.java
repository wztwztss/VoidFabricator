package com.wztwzt.voidfabricator.pattern;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** A blacklist/whitelist entry matched against recipe outputs. */
public final class PatternFilter {

    public static final int KIND_ITEM = 0;
    public static final int KIND_FLUID = 1;

    private String key;
    private String name;
    private int kind;
    private ItemStack icon;

    public PatternFilter() {}

    public PatternFilter(String key, String name, int kind, ItemStack icon) {
        this.key = key;
        this.name = name;
        this.kind = kind;
        this.icon = icon == null ? null : icon.copy();
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getKind() {
        return kind;
    }

    public ItemStack getIcon() {
        return icon == null ? null : icon.copy();
    }

    public PatternFilter copy() {
        return new PatternFilter(key, name, kind, icon);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Key", key == null ? "" : key);
        tag.setString("Name", name == null ? "" : name);
        tag.setByte("Kind", (byte) kind);
        if (icon != null) {
            tag.setTag("Icon", icon.writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    public static PatternFilter fromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        String key = tag.getString("Key");
        if (key.isEmpty()) {
            return null;
        }
        PatternFilter filter = new PatternFilter();
        filter.key = key;
        filter.name = tag.getString("Name");
        filter.kind = tag.getByte("Kind");
        if (tag.hasKey("Icon", 10)) {
            filter.icon = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Icon"));
        }
        return filter;
    }
}
