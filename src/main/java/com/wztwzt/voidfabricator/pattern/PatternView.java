package com.wztwzt.voidfabricator.pattern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client-safe projection of one machine pool and its visible recipe-icon window. */
public final class PatternView {

    private static final Logger LOGGER = LogManager.getLogger(PatternView.class);
    private static final int VIEW_FORMAT = 4;

    private ItemStack machine;
    private String machineName;
    private final List<PatternPoolView> pools = new ArrayList<>();
    private final List<PatternEntry> entries = new ArrayList<>();
    private final List<PatternGroupView> groups = new ArrayList<>();
    private final Map<String, NBTTagCompound> recipeSnapshots = new LinkedHashMap<>();
    private final List<PatternFilter> filters = new ArrayList<>();
    private boolean groupSameOutput;
    private int groupCount;
    private int groupStartRow;
    private int filterMode;
    private boolean smartDoubling = true;
    private int maxRounds;
    private int deletedCount;
    private int matchedCount;
    private int selected;
    private int selectedPool;
    private int page;
    private int pageCount = 1;
    private int totalEntries;
    private int configuredPools;
    private int maxTier = -1;
    private boolean firstRowNewTier = true;
    private String status = "pattern_required";
    private int published;
    private long pendingBatches;
    private int pendingTypes;
    private int skipped;
    private boolean online;
    private boolean qolAvailable;

    public ItemStack getMachine() {
        return machine == null ? null : machine.copy();
    }

    public void setMachine(ItemStack machine) {
        this.machine = machine == null ? null : machine.copy();
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public List<PatternPoolView> getPools() {
        return Collections.unmodifiableList(pools);
    }

    public void addPool(PatternPoolView pool) {
        if (pool != null && pools.size() < PatternPoolData.MAX_POOLS) {
            pools.add(pool);
        }
    }

    public PatternPoolView pool(int index) {
        return index >= 0 && index < pools.size() ? pools.get(index) : null;
    }

    public List<PatternEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void addEntry(PatternEntry entry) {
        if (entry != null && entries.size() < PatternPoolData.PREVIEW_PAGE_SIZE) {
            entries.add(entry.copy());
        }
    }

    public PatternEntry entry(int index) {
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    /** Resolves a visible grid cell in either the flat or same-output grouped projection. */
    public PatternEntry entryAt(int row, int column) {
        if (groupSameOutput) {
            PatternGroupView group = group(row);
            return group == null ? null : group.entry(column);
        }
        return entry(row * PatternPoolData.GRID_COLUMNS + column);
    }

    public List<PatternGroupView> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void addGroup(PatternGroupView group) {
        if (group != null && groups.size() < PatternPoolData.VISIBLE_ROWS) {
            groups.add(group);
        }
    }

    public PatternGroupView group(int row) {
        return row >= 0 && row < groups.size() ? groups.get(row) : null;
    }

    public boolean isGroupSameOutput() {
        return groupSameOutput;
    }

    public void setGroupSameOutput(boolean groupSameOutput) {
        this.groupSameOutput = groupSameOutput;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = Math.max(0, groupCount);
    }

    public int getGroupStartRow() {
        return groupStartRow;
    }

    public void setGroupStartRow(int groupStartRow) {
        this.groupStartRow = Math.max(0, groupStartRow);
    }

    public void addRecipeSnapshot(String key, NBTTagCompound snapshot) {
        if (key != null && !key.isEmpty()
            && snapshot != null
            && recipeSnapshots.size() < PatternPoolData.PREVIEW_PAGE_SIZE) {
            recipeSnapshots.put(key, (NBTTagCompound) snapshot.copy());
        }
    }

    public NBTTagCompound getRecipeSnapshot(String key) {
        NBTTagCompound tag = key == null ? null : recipeSnapshots.get(key);
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }

    public Map<String, NBTTagCompound> getRecipeSnapshots() {
        return Collections.unmodifiableMap(recipeSnapshots);
    }

    public List<PatternFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public void addFilter(PatternFilter filter) {
        if (filter != null && filters.size() < PatternPoolData.MAX_FILTERS) {
            filters.add(filter.copy());
        }
    }

    public PatternFilter filter(int index) {
        return index >= 0 && index < filters.size() ? filters.get(index) : null;
    }

    public PatternEntry selectedEntry() {
        for (PatternEntry entry : entries) {
            if (entry.getGlobalIndex() == selected) {
                return entry;
            }
        }
        for (PatternGroupView group : groups) {
            for (PatternEntry entry : group.getEntries()) {
                if (entry.getGlobalIndex() == selected) {
                    return entry;
                }
            }
        }
        return null;
    }

    public int getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(int filterMode) {
        this.filterMode = filterMode;
    }

    public boolean isWhitelist() {
        return filterMode == 1;
    }

    public void setSmartDoubling(boolean smartDoubling) {
        this.smartDoubling = smartDoubling;
    }

    public boolean isSmartDoubling() {
        return smartDoubling;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public void setMatchedCount(int matchedCount) {
        this.matchedCount = matchedCount;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected < 0 ? 0 : selected;
    }

    public int getSelectedPool() {
        return selectedPool;
    }

    public void setSelectedPool(int selectedPool) {
        this.selectedPool = Math.max(0, Math.min(PatternPoolData.MAX_POOLS - 1, selectedPool));
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = Math.max(1, pageCount);
    }

    public int getEnabledCount() {
        int count = 0;
        for (PatternPoolView pool : pools) {
            count += pool.getEnabledCount();
        }
        return count;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = Math.max(0, totalEntries);
    }

    public int getConfiguredPools() {
        return configuredPools;
    }

    public void setConfiguredPools(int configuredPools) {
        this.configuredPools = configuredPools;
    }

    public int getMaxTier() {
        return maxTier;
    }

    public void setMaxTier(int maxTier) {
        this.maxTier = maxTier;
    }

    public boolean isFirstRowNewTier() {
        return firstRowNewTier;
    }

    public void setFirstRowNewTier(boolean firstRowNewTier) {
        this.firstRowNewTier = firstRowNewTier;
    }

    public String getStatus() {
        return status;
    }

    public int getPublished() {
        return published;
    }

    public long getPendingBatches() {
        return pendingBatches;
    }

    public int getPendingTypes() {
        return pendingTypes;
    }

    public int getSkipped() {
        return skipped;
    }

    public boolean isOnline() {
        return online;
    }

    public void setQolAvailable(boolean qolAvailable) {
        this.qolAvailable = qolAvailable;
    }

    public boolean isQolAvailable() {
        return qolAvailable;
    }

    public void setPublished(int published) {
        this.published = published;
    }

    public void setPendingBatches(long pendingBatches) {
        this.pendingBatches = pendingBatches;
    }

    public void setPendingTypes(int pendingTypes) {
        this.pendingTypes = pendingTypes;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setStatus(String status) {
        this.status = status == null || status.isEmpty() ? "pattern_required" : status;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Format", VIEW_FORMAT);
        if (machine != null) {
            tag.setTag("Machine", machine.writeToNBT(new NBTTagCompound()));
        }
        tag.setString("MachineName", machineName == null ? "" : machineName);
        NBTTagList poolList = new NBTTagList();
        for (PatternPoolView pool : pools) {
            poolList.appendTag(pool.toNBT());
        }
        tag.setTag("Pools", poolList);
        NBTTagList entryList = new NBTTagList();
        for (PatternEntry entry : entries) {
            entryList.appendTag(entry.toNBT());
        }
        tag.setTag("Entries", entryList);
        NBTTagList groupList = new NBTTagList();
        for (PatternGroupView group : groups) {
            groupList.appendTag(group.toNBT());
        }
        tag.setTag("Groups", groupList);
        NBTTagList snapshotList = new NBTTagList();
        for (Map.Entry<String, NBTTagCompound> entry : recipeSnapshots.entrySet()) {
            NBTTagCompound wrapper = new NBTTagCompound();
            wrapper.setString("Key", entry.getKey());
            wrapper.setTag("Snapshot", entry.getValue());
            snapshotList.appendTag(wrapper);
        }
        tag.setTag("Snapshots", snapshotList);
        tag.setBoolean("GroupSameOutput", groupSameOutput);
        tag.setInteger("GroupCount", groupCount);
        tag.setInteger("GroupStartRow", groupStartRow);
        NBTTagList filterList = new NBTTagList();
        for (PatternFilter filter : filters) {
            filterList.appendTag(filter.toNBT());
        }
        tag.setTag("Filters", filterList);
        tag.setByte("FilterMode", (byte) filterMode);
        tag.setBoolean("SmartDoubling", smartDoubling);
        tag.setInteger("MaxRounds", maxRounds);
        tag.setInteger("DeletedCount", deletedCount);
        tag.setInteger("MatchedCount", matchedCount);
        tag.setInteger("Selected", selected);
        tag.setInteger("SelectedPool", selectedPool);
        tag.setInteger("Page", page);
        tag.setInteger("PageCount", pageCount);
        tag.setInteger("TotalEntries", totalEntries);
        tag.setInteger("ConfiguredPools", configuredPools);
        tag.setInteger("MaxTier", maxTier);
        tag.setBoolean("FirstRowNewTier", firstRowNewTier);
        tag.setString("Status", status == null ? "" : status);
        tag.setInteger("Published", published);
        tag.setLong("PendingBatches", pendingBatches);
        tag.setInteger("PendingTypes", pendingTypes);
        tag.setInteger("Skipped", skipped);
        tag.setBoolean("Online", online);
        tag.setBoolean("QolAvailable", qolAvailable);
        return tag;
    }

    public static PatternView fromNBT(NBTTagCompound tag) {
        PatternView view = new PatternView();
        if (tag == null || tag.getInteger("Format") != VIEW_FORMAT) {
            return view;
        }
        if (tag.hasKey("Machine", 10)) {
            view.machine = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Machine"));
        }
        view.machineName = tag.getString("MachineName");
        NBTTagList poolList = tag.getTagList("Pools", 10);
        for (int index = 0; index < poolList.tagCount(); index++) {
            view.pools.add(PatternPoolView.fromNBT(poolList.getCompoundTagAt(index)));
        }
        NBTTagList entryList = tag.getTagList("Entries", 10);
        for (int index = 0; index < entryList.tagCount(); index++) {
            PatternEntry entry = PatternEntry.fromNBT(entryList.getCompoundTagAt(index));
            if (entry != null) {
                view.entries.add(entry);
            }
        }
        NBTTagList groupList = tag.getTagList("Groups", 10);
        for (int index = 0; index < groupList.tagCount(); index++) {
            PatternGroupView group = PatternGroupView.fromNBT(groupList.getCompoundTagAt(index));
            if (group != null) {
                view.groups.add(group);
            }
        }
        NBTTagList snapshotList = tag.getTagList("Snapshots", 10);
        for (int index = 0; index < snapshotList.tagCount(); index++) {
            NBTTagCompound wrapper = snapshotList.getCompoundTagAt(index);
            String key = wrapper.getString("Key");
            if (!key.isEmpty() && wrapper.hasKey("Snapshot", 10)) {
                view.recipeSnapshots.put(key, wrapper.getCompoundTag("Snapshot"));
            }
        }
        view.groupSameOutput = tag.getBoolean("GroupSameOutput");
        view.groupCount = tag.getInteger("GroupCount");
        view.groupStartRow = tag.getInteger("GroupStartRow");
        NBTTagList filterList = tag.getTagList("Filters", 10);
        for (int index = 0; index < filterList.tagCount(); index++) {
            PatternFilter filter = PatternFilter.fromNBT(filterList.getCompoundTagAt(index));
            if (filter != null) {
                view.filters.add(filter);
            }
        }
        view.filterMode = tag.getByte("FilterMode");
        view.smartDoubling = !tag.hasKey("SmartDoubling") || tag.getBoolean("SmartDoubling");
        view.maxRounds = tag.getInteger("MaxRounds");
        view.deletedCount = tag.getInteger("DeletedCount");
        view.matchedCount = tag.getInteger("MatchedCount");
        view.selected = tag.getInteger("Selected");
        view.selectedPool = tag.getInteger("SelectedPool");
        view.page = tag.getInteger("Page");
        view.pageCount = Math.max(1, tag.getInteger("PageCount"));
        view.totalEntries = tag.getInteger("TotalEntries");
        view.configuredPools = tag.getInteger("ConfiguredPools");
        view.maxTier = tag.hasKey("MaxTier") ? tag.getInteger("MaxTier") : -1;
        view.firstRowNewTier = !tag.hasKey("FirstRowNewTier") || tag.getBoolean("FirstRowNewTier");
        view.status = tag.getString("Status");
        if (view.status.isEmpty()) {
            view.status = "pattern_required";
        }
        view.published = tag.getInteger("Published");
        view.pendingBatches = tag.getLong("PendingBatches");
        view.pendingTypes = tag.getInteger("PendingTypes");
        view.skipped = tag.getInteger("Skipped");
        view.online = tag.getBoolean("Online");
        view.qolAvailable = tag.getBoolean("QolAvailable");
        return view;
    }

    public byte[] toBytes() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(toNBT(), output);
            return output.toByteArray();
        } catch (IOException exception) {
            LOGGER.error("Could not encode pool pattern view", exception);
            return new byte[0];
        }
    }

    public void load(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try {
            NBTTagCompound tag = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            PatternView decoded = fromNBT(tag);
            apply(decoded);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not decode pool pattern view", exception);
        }
    }

    /** Replaces this view's projection while keeping the same instance for open GUI widgets. */
    public void copyFrom(PatternView decoded) {
        if (decoded != null) {
            apply(decoded);
        }
    }

    private void apply(PatternView decoded) {
        machine = decoded.machine;
        machineName = decoded.machineName;
        pools.clear();
        pools.addAll(decoded.pools);
        entries.clear();
        entries.addAll(decoded.entries);
        groups.clear();
        groups.addAll(decoded.groups);
        recipeSnapshots.clear();
        recipeSnapshots.putAll(decoded.recipeSnapshots);
        filters.clear();
        filters.addAll(decoded.filters);
        groupSameOutput = decoded.groupSameOutput;
        groupCount = decoded.groupCount;
        groupStartRow = decoded.groupStartRow;
        filterMode = decoded.filterMode;
        smartDoubling = decoded.smartDoubling;
        maxRounds = decoded.maxRounds;
        deletedCount = decoded.deletedCount;
        matchedCount = decoded.matchedCount;
        selected = decoded.selected;
        selectedPool = decoded.selectedPool;
        page = decoded.page;
        pageCount = decoded.pageCount;
        totalEntries = decoded.totalEntries;
        configuredPools = decoded.configuredPools;
        maxTier = decoded.maxTier;
        firstRowNewTier = decoded.firstRowNewTier;
        status = decoded.status;
        published = decoded.published;
        pendingBatches = decoded.pendingBatches;
        pendingTypes = decoded.pendingTypes;
        skipped = decoded.skipped;
        online = decoded.online;
        qolAvailable = decoded.qolAvailable;
    }
}
