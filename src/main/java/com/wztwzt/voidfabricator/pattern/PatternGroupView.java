package com.wztwzt.voidfabricator.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** One visible "same output" row: every alternative recipe that produces the same output. */
public final class PatternGroupView {

    private String key;
    private String name;
    private int totalCount;
    private final List<PatternEntry> entries = new ArrayList<>(PatternPoolData.GRID_COLUMNS);

    public PatternGroupView() {}

    public PatternGroupView(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getVisibleCount() {
        return entries.size();
    }

    public List<PatternEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public PatternEntry entry(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    public void addEntry(PatternEntry entry) {
        if (entry == null) {
            return;
        }
        totalCount++;
        if (entries.size() >= PatternPoolData.GRID_COLUMNS) {
            return;
        }
        PatternEntry copy = entry.copy();
        if (name == null || name.isEmpty()) {
            name = copy.getFilterName();
        }
        entries.add(copy);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Key", key == null ? "" : key);
        tag.setString("Name", name == null ? "" : name);
        tag.setInteger("TotalCount", totalCount);
        NBTTagList list = new NBTTagList();
        for (PatternEntry entry : entries) {
            list.appendTag(entry.toNBT());
        }
        tag.setTag("Entries", list);
        return tag;
    }

    public static PatternGroupView fromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        PatternGroupView group = new PatternGroupView(tag.getString("Key"), tag.getString("Name"));
        group.totalCount = tag.getInteger("TotalCount");
        NBTTagList list = tag.getTagList("Entries", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            PatternEntry entry = PatternEntry.fromNBT(list.getCompoundTagAt(index));
            if (entry != null) {
                group.entries.add(entry);
            }
        }
        if (group.totalCount < group.entries.size()) {
            group.totalCount = group.entries.size();
        }
        return group;
    }
}
