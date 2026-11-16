package com.wztwzt.voidfabricator.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.recipe.GTMachineScanner;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;
import com.wztwzt.voidfabricator.recipe.ScanResult;

import cpw.mods.fml.common.Loader;

/** Mutable server model: one machine-backed recipe pool per physical pattern. */
public final class PatternPoolData {

    /** Upper bound used only while reading the retired 27-pool configuration format. */
    public static final int MAX_POOLS = 27;
    public static final int GRID_COLUMNS = 13;
    public static final int VISIBLE_ROWS = 7;
    public static final int PREVIEW_PAGE_SIZE = GRID_COLUMNS * VISIBLE_ROWS;
    /** Legacy alias used by the retired 27-entry editor. */
    public static final int MAX_ENTRIES = PREVIEW_PAGE_SIZE;
    public static final int MAX_FILTERS = 27;
    public static final int MODE_BLACKLIST = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MAX_ROUNDS = 64;

    private static final String TAG_POOLS = "Pools";
    private static final String TAG_MACHINE = "Machine";
    private static final String TAG_ENTRIES = "Entries";
    private static final String TAG_FILTERS = "Filters";
    private static final String TAG_MODE = "FilterMode";
    private static final String TAG_DELETED = "Deleted";
    private static final String TAG_SMART = "SmartDoubling";
    private static final String TAG_MAX_ROUNDS = "MaxRounds";
    private static final String TAG_GROUP_SAME = "GroupSameOutput";
    private static final String TAG_FORMAT = "ConfigFormat";
    private static final int CONFIG_FORMAT = 4;

    private final ItemStack stack;
    private final List<RecipePoolSlot> pools = new ArrayList<>(MAX_POOLS);
    private final List<PatternFilter> filters = new ArrayList<>();
    private final Set<String> deleted = new HashSet<>();
    private int filterMode = MODE_BLACKLIST;
    private boolean smartDoubling = true;
    private boolean groupSameOutput;
    private int maxRounds;
    private String status = "pattern_required";
    private Runnable onChanged;
    private Runnable onViewChanged;
    private Runnable onSaved;

    private PatternPoolData(ItemStack stack) {
        this.stack = stack;
        pools.add(new RecipePoolSlot(0));
        load();
    }

    public static PatternPoolData fromStack(ItemStack stack) {
        return new PatternPoolData(stack);
    }

    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    public void setOnViewChanged(Runnable onViewChanged) {
        this.onViewChanged = onViewChanged;
    }

    /**
     * Called after each NBT write. The item GUI uses this to copy the configuration
     * from its captured stack onto the player's current hand slot immediately.
     */
    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    /** Forces a write and the matching hand-slot mirror. */
    public void persistNow() {
        save();
    }

    private void load() {
        if (!ItemMachineRecipePoolPattern.isPoolPattern(stack)) {
            status = "pattern_required";
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            status = "no_machine";
            return;
        }
        NBTTagList poolList = tag.getTagList(TAG_POOLS, 10);
        if (poolList.tagCount() > 0) {
            for (int index = 0; index < poolList.tagCount() && index < MAX_POOLS; index++) {
                while (pools.size() <= index) {
                    pools.add(new RecipePoolSlot(pools.size()));
                }
                pools.get(index)
                    .load(poolList.getCompoundTagAt(index));
            }
        } else {
            // ConfigFormat 2 migration: the old single machine/Entries root becomes pool 0.
            pools.get(0)
                .load(tag);
        }
        NBTTagList filterList = tag.getTagList(TAG_FILTERS, 10);
        for (int index = 0; index < filterList.tagCount() && filters.size() < MAX_FILTERS; index++) {
            PatternFilter filter = PatternFilter.fromNBT(filterList.getCompoundTagAt(index));
            if (filter != null) {
                filters.add(filter);
            }
        }
        filterMode = tag.getByte(TAG_MODE) == MODE_WHITELIST ? MODE_WHITELIST : MODE_BLACKLIST;
        smartDoubling = !tag.hasKey(TAG_SMART) || tag.getBoolean(TAG_SMART);
        groupSameOutput = tag.getBoolean(TAG_GROUP_SAME);
        maxRounds = tag.getInteger(TAG_MAX_ROUNDS);
        if (maxRounds < 0 || maxRounds > MAX_ROUNDS) {
            maxRounds = 0;
        }
        NBTTagList deletedList = tag.getTagList(TAG_DELETED, 8);
        for (int index = 0; index < deletedList.tagCount(); index++) {
            String key = deletedList.getStringTagAt(index);
            if (!key.isEmpty()) {
                deleted.add(key);
            }
        }
        migrateLegacyPools();
        updateStatus();
    }

    /** Converts the retired 27-pools-in-one-item experiment to the current one-machine model. */
    private void migrateLegacyPools() {
        boolean legacy = false;
        for (int index = 1; index < pools.size(); index++) {
            RecipePoolSlot pool = pools.get(index);
            if (pool.getMachine() != null || !pool.getEntries()
                .isEmpty()) {
                legacy = true;
                break;
            }
        }
        if (!legacy) {
            trimToPrimaryPool();
            return;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && !root.hasKey("LegacyPools", 9)) {
            root.setTag("LegacyPools", root.getTagList(TAG_POOLS, 10));
        }
        Map<String, PatternEntry> merged = new LinkedHashMap<>();
        ItemStack primaryMachine = pools.get(0)
            .getMachine();
        for (RecipePoolSlot pool : pools) {
            if (primaryMachine == null && pool.getMachine() != null) {
                primaryMachine = pool.getMachine();
            }
            for (PatternEntry entry : pool.getEntries()) {
                if (entry.getKey() == null) {
                    continue;
                }
                PatternEntry previous = merged.get(entry.getKey());
                if (previous == null) {
                    merged.put(entry.getKey(), entry.copy());
                } else {
                    previous.setEnabled(previous.isEnabled() && entry.isEnabled());
                }
            }
        }
        RecipePoolSlot primary = pools.get(0);
        primary.setMachine(null);
        primary.setMachine(primaryMachine);
        primary.rescan(true, deleted);
        for (PatternEntry entry : primary.getEntries()) {
            PatternEntry previous = merged.get(entry.getKey());
            if (previous != null) {
                entry.setEnabled(previous.isEnabled());
            }
        }
        trimToPrimaryPool();
    }

    private void trimToPrimaryPool() {
        if (pools.size() > 1) {
            pools.subList(1, pools.size())
                .clear();
        }
    }

    public ItemStack getStack() {
        return stack;
    }

    public List<RecipePoolSlot> getPools() {
        return Collections.unmodifiableList(pools);
    }

    public RecipePoolSlot getPool(int index) {
        return index >= 0 && index < pools.size() ? pools.get(index) : null;
    }

    public int getConfiguredPoolCount() {
        int count = 0;
        for (RecipePoolSlot pool : pools) {
            if (pool.getMachine() != null) {
                count++;
            }
        }
        return count;
    }

    public boolean hasMachine() {
        return getConfiguredPoolCount() > 0;
    }

    /** Pool 0 is retained for old block automation and legacy migration. */
    public ItemStack getMachine() {
        return pools.get(0)
            .getMachine();
    }

    public ItemStack getMachine(int poolIndex) {
        RecipePoolSlot pool = getPool(poolIndex);
        return pool == null ? null : pool.getMachine();
    }

    public String getMachineName() {
        return pools.get(0)
            .getMachineName();
    }

    public String getMachineName(int poolIndex) {
        RecipePoolSlot pool = getPool(poolIndex);
        return pool == null ? null : pool.getMachineName();
    }

    public void setMachine(ItemStack newMachine) {
        setMachine(0, newMachine);
    }

    public void setMachine(int poolIndex, ItemStack newMachine) {
        RecipePoolSlot pool = getPool(poolIndex);
        if (pool == null) {
            return;
        }
        ItemStack normalized = newMachine == null ? null : newMachine.copy();
        if (normalized != null) {
            normalized.stackSize = 1;
        }
        ItemStack old = pool.getMachine();
        if (normalized == null ? old == null : ItemStack.areItemStacksEqual(old, normalized)) {
            return;
        }
        pool.setMachine(normalized);
        pool.rescan(true, deleted);
        updateStatus();
        save();
        changed();
    }

    /** Removes and returns the first configured machine for the old block eject button. */
    public ItemStack ejectFirstMachine() {
        for (RecipePoolSlot pool : pools) {
            ItemStack machine = pool.getMachine();
            if (machine != null) {
                pool.setMachine(null);
                updateStatus();
                save();
                changed();
                return machine;
            }
        }
        return null;
    }

    public List<PatternEntry> getEntries() {
        List<PatternEntry> result = new ArrayList<>();
        int globalIndex = 0;
        for (RecipePoolSlot pool : pools) {
            for (PatternEntry entry : pool.getEntries()) {
                PatternEntry copy = entry.copy();
                copy.setPoolIndex(pool.getIndex());
                copy.setGlobalIndex(globalIndex++);
                result.add(copy);
            }
        }
        return result;
    }

    public PatternEntry getEntry(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null) {
            return null;
        }
        PatternEntry copy = ref.entry.copy();
        copy.setPoolIndex(ref.pool.getIndex());
        copy.setGlobalIndex(globalIndex);
        return copy;
    }

    public List<PatternFilter> getFilters() {
        return new ArrayList<>(filters);
    }

    public int getFilterMode() {
        return filterMode;
    }

    public boolean isSmartDoubling() {
        return smartDoubling;
    }

    public boolean isGroupSameOutput() {
        return groupSameOutput;
    }

    public void setGroupSameOutput(boolean enabled) {
        if (groupSameOutput == enabled) {
            return;
        }
        groupSameOutput = enabled;
        save();
        changed();
    }

    public void toggleGroupSameOutput() {
        setGroupSameOutput(!groupSameOutput);
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public int getDeletedCount() {
        return deleted.size();
    }

    public int getMatchedCount() {
        return getTotalEntryCount();
    }

    public int getTotalEntryCount() {
        int count = 0;
        for (RecipePoolSlot pool : pools) {
            count += pool.getEntries()
                .size();
        }
        return count;
    }

    public int getEnabledCount() {
        int count = 0;
        for (RecipePoolSlot pool : pools) {
            count += pool.getEnabledCount();
        }
        return count;
    }

    public int getAllowedCount() {
        Set<String> keys = new HashSet<>();
        for (PatternEntry entry : getEntries()) {
            if (isEntryAllowed(entry) && entry.getKey() != null) {
                keys.add(entry.getKey());
            }
        }
        return keys.size();
    }

    public int getSkippedTotal() {
        int count = 0;
        for (RecipePoolSlot pool : pools) {
            count += pool.getSkippedTotal();
        }
        return count;
    }

    /** Pool 0 scan is retained for old status/tooltips. */
    public ScanResult getScan() {
        return pools.get(0)
            .getScan();
    }

    public ScanResult getScan(int poolIndex) {
        RecipePoolSlot pool = getPool(poolIndex);
        return pool == null ? null : pool.getScan();
    }

    public String getStatus() {
        return status == null ? "pattern_required" : status;
    }

    public void rescan(boolean fillEmpty) {
        rescanAll(fillEmpty);
    }

    public void rescanAll() {
        rescanAll(true);
    }

    public void rescanAll(boolean fillEmpty) {
        for (RecipePoolSlot pool : pools) {
            pool.rescan(fillEmpty, deleted);
        }
        updateStatus();
        save();
        changed();
    }

    public void rescanPool(int poolIndex, boolean fillEmpty) {
        RecipePoolSlot pool = getPool(poolIndex);
        if (pool == null) {
            return;
        }
        pool.rescan(fillEmpty, deleted);
        updateStatus();
        save();
        changed();
    }

    /** Restores runtime scans without recursively posting another AE pattern change. */
    public void ensureScans() {
        for (RecipePoolSlot pool : pools) {
            if (pool.getMachine() != null && pool.getScan() == null) {
                pool.rescan(
                    pool.getEntries()
                        .isEmpty(),
                    deleted);
            }
        }
        updateStatus();
        save();
    }

    public void applyFilters() {
        save();
        changed();
    }

    public boolean delete(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null) {
            return false;
        }
        PatternEntry removed = ref.pool.removeEntry(ref.localIndex);
        if (removed != null && removed.getKey() != null) {
            String key = removed.getKey();
            deleted.add(key);
            for (RecipePoolSlot pool : pools) {
                for (int index = pool.getEntries()
                    .size() - 1; index >= 0; index--) {
                    PatternEntry entry = pool.getEntry(index);
                    if (entry != null && key.equals(entry.getKey())) {
                        pool.removeEntry(index);
                    }
                }
            }
        }
        updateStatus();
        save();
        changed();
        return removed != null;
    }

    public boolean toggle(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null) {
            return false;
        }
        ref.entry.setEnabled(!ref.entry.isEnabled());
        save();
        changed();
        return true;
    }

    public boolean setAllEntriesEnabled(boolean enabled) {
        if (getTotalEntryCount() == 0) {
            return false;
        }
        for (RecipePoolSlot pool : pools) {
            for (PatternEntry entry : pool.getEntries()) {
                entry.setEnabled(enabled);
            }
        }
        save();
        changed();
        return true;
    }

    /** Enables or disables every scanned entry at one voltage tier. */
    public boolean setEntriesEnabledByTier(int tier, boolean enabled) {
        boolean matched = false;
        for (RecipePoolSlot pool : pools) {
            for (PatternEntry entry : pool.getEntries()) {
                if (entry.getVoltageTier() == tier) {
                    entry.setEnabled(enabled);
                    matched = true;
                }
            }
        }
        if (!matched) {
            return false;
        }
        save();
        changed();
        return true;
    }

    /** True when every entry at this voltage tier is enabled; an empty tier returns false. */
    public boolean isTierEnabled(int tier) {
        boolean matched = false;
        for (RecipePoolSlot pool : pools) {
            for (PatternEntry entry : pool.getEntries()) {
                if (entry.getVoltageTier() == tier) {
                    matched = true;
                    if (!entry.isEnabled()) {
                        return false;
                    }
                }
            }
        }
        return matched;
    }

    public boolean isEntryFiltered(PatternEntry entry) {
        if (entry == null) {
            return false;
        }
        String key = entry.getFilterKey();
        if (filters.isEmpty()) {
            return filterMode == MODE_WHITELIST;
        }
        boolean matched = false;
        if (key != null && !key.isEmpty()) {
            for (PatternFilter filter : filters) {
                if (key.equals(filter.getKey())) {
                    matched = true;
                    break;
                }
            }
        }
        return filterMode == MODE_WHITELIST ? !matched : matched;
    }

    public boolean isEntryAllowed(PatternEntry entry) {
        return entry != null && entry.isEnabled()
            && (entry.getKey() == null || !deleted.contains(entry.getKey()))
            && !isEntryFiltered(entry);
    }

    public boolean toggleSameOutputFilter(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null) {
            return false;
        }
        PatternEntry entry = ref.entry;
        String key = entry.getFilterKey();
        if (key == null || key.isEmpty()) {
            return false;
        }
        for (int index = 0; index < filters.size(); index++) {
            if (key.equals(
                filters.get(index)
                    .getKey())) {
                filters.remove(index);
                save();
                changed();
                return true;
            }
        }
        if (filters.size() >= MAX_FILTERS) {
            return false;
        }
        filters.add(new PatternFilter(key, entry.getFilterName(), entry.getFilterKind(), entry.getDisplay()));
        save();
        changed();
        return true;
    }

    public boolean clearSameOutputFilter(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null) {
            return false;
        }
        String key = ref.entry.getFilterKey();
        if (key == null || key.isEmpty()) {
            return false;
        }
        for (int index = 0; index < filters.size(); index++) {
            if (key.equals(
                filters.get(index)
                    .getKey())) {
                filters.remove(index);
                save();
                changed();
                return true;
            }
        }
        return false;
    }

    public boolean addFilterFromEntry(int globalIndex) {
        EntryRef ref = find(globalIndex);
        if (ref == null || filters.size() >= MAX_FILTERS) {
            return false;
        }
        PatternEntry entry = ref.entry;
        String key = entry.getFilterKey();
        if (key == null || key.isEmpty() || hasFilter(key)) {
            return false;
        }
        filters.add(new PatternFilter(key, entry.getFilterName(), entry.getFilterKind(), entry.getDisplay()));
        save();
        changed();
        return true;
    }

    public boolean removeFilter(int index) {
        if (index < 0 || index >= filters.size()) {
            return false;
        }
        filters.remove(index);
        save();
        changed();
        return true;
    }

    public void clearFilters() {
        if (filters.isEmpty()) {
            return;
        }
        filters.clear();
        save();
        changed();
    }

    public void setFilterMode(int mode) {
        int normalized = mode == MODE_WHITELIST ? MODE_WHITELIST : MODE_BLACKLIST;
        if (filterMode == normalized) {
            return;
        }
        filterMode = normalized;
        save();
        changed();
    }

    public void restoreDeleted() {
        if (deleted.isEmpty()) {
            return;
        }
        deleted.clear();
        rescanAll(true);
    }

    public void setSmartDoubling(boolean enabled) {
        if (smartDoubling == enabled) {
            return;
        }
        smartDoubling = enabled;
        save();
        changed();
    }

    public void setMaxRounds(int rounds) {
        int normalized = Math.max(0, Math.min(MAX_ROUNDS, rounds));
        if (maxRounds == normalized) {
            return;
        }
        maxRounds = normalized;
        save();
        changed();
    }

    private boolean hasFilter(String key) {
        for (PatternFilter filter : filters) {
            if (key.equals(filter.getKey())) {
                return true;
            }
        }
        return false;
    }

    private EntryRef find(int globalIndex) {
        if (globalIndex < 0) {
            return null;
        }
        int current = 0;
        for (RecipePoolSlot pool : pools) {
            int size = pool.getEntries()
                .size();
            if (globalIndex < current + size) {
                int localIndex = globalIndex - current;
                return new EntryRef(pool, localIndex, pool.getEntry(localIndex));
            }
            current += size;
        }
        return null;
    }

    private void updateStatus() {
        if (!hasMachine()) {
            status = "no_machine";
            return;
        }
        for (RecipePoolSlot pool : pools) {
            if ("scan_error".equals(pool.getStatus())) {
                status = "scan_error";
                return;
            }
        }
        status = getTotalEntryCount() == 0 ? "no_entries" : "ready";
    }

    public int getMatchedEntryCount(String query) {
        return searchedEntries(query).size();
    }

    public PatternEntry getSearchedEntry(int index, String query) {
        List<PatternEntry> matches = searchedEntries(query);
        return index >= 0 && index < matches.size() ? matches.get(index) : null;
    }

    private List<PatternEntry> searchedEntries(String query) {
        List<PatternEntry> all = getEntries();
        String normalized = query == null ? ""
            : query.trim()
                .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return all;
        }
        List<PatternEntry> matches = new ArrayList<>();
        for (PatternEntry entry : all) {
            String name = entry.getFilterName() == null ? "" : entry.getFilterName();
            String source = entry.getSource() == null ? "" : entry.getSource();
            ItemStack display = entry.getDisplay();
            String displayName = display == null ? "" : display.getDisplayName();
            if (name.toLowerCase(Locale.ROOT)
                .contains(normalized)
                || source.toLowerCase(Locale.ROOT)
                    .contains(normalized)
                || displayName.toLowerCase(Locale.ROOT)
                    .contains(normalized)) {
                matches.add(entry);
            }
        }
        matches.sort(
            Comparator.comparingInt(PatternEntry::getVoltageTier)
                .thenComparing(
                    entry -> entry.getFilterName() == null ? "" : entry.getFilterName(),
                    String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.getKey() == null ? "" : entry.getKey()));
        return matches;
    }

    public PatternView toView() {
        return toView(0, 0);
    }

    public PatternView toView(int requestedPage, int selectedPool) {
        return toView(requestedPage, selectedPool, "");
    }

    public PatternView toView(int requestedRow, int selectedPool, String query) {
        PatternView view = new PatternView();
        int poolIndex = 0;
        RecipePoolSlot selected = pools.get(poolIndex);
        view.setMachine(selected.getMachine());
        view.setMachineName(selected.getMachineName());
        int configured = getConfiguredPoolCount();
        for (RecipePoolSlot pool : Collections.singletonList(selected)) {
            PatternPoolView poolView = new PatternPoolView();
            poolView.setIndex(pool.getIndex());
            poolView.setMachine(pool.getMachine());
            poolView.setPreview(pool.getPreview());
            poolView.setMachineName(pool.getMachineName());
            poolView.setStatus(pool.getStatus());
            poolView.setPatternCount(
                pool.getEntries()
                    .size());
            poolView.setEnabledCount(pool.getEnabledCount());
            view.addPool(poolView);
        }
        List<PatternEntry> allEntries = getEntries();
        List<PatternEntry> all = searchedEntries(query);
        Map<String, RecipeSnapshot> snapshotsByKey = new LinkedHashMap<>();
        for (RecipePoolSlot pool : pools) {
            ScanResult scan = pool.getScan();
            if (scan == null) {
                continue;
            }
            for (RecipeSnapshot snapshot : scan.recipes()) {
                if (snapshot == null || snapshot.getKey() == null) {
                    continue;
                }
                snapshotsByKey.put(
                    snapshot.getKey()
                        .value(),
                    snapshot);
            }
        }
        int maxTier = -1;
        for (PatternEntry entry : all) {
            maxTier = Math.max(maxTier, entry.getVoltageTier());
        }
        view.setGroupSameOutput(groupSameOutput);
        view.setMaxTier(maxTier);
        if (groupSameOutput) {
            List<PatternGroupView> groups = buildGroups(all);
            int groupCount = groups.size();
            int pageCount = Math.max(1, (groupCount + VISIBLE_ROWS - 1) / VISIBLE_ROWS);
            int maxRow = Math.max(0, groupCount - VISIBLE_ROWS);
            int startGroup = Math.max(0, Math.min(maxRow, requestedRow));
            for (int index = startGroup; index < groups.size() && index < startGroup + VISIBLE_ROWS; index++) {
                PatternGroupView group = groups.get(index);
                view.addGroup(group);
                for (PatternEntry entry : group.getEntries()) {
                    addRecipeSnapshot(view, snapshotsByKey, entry);
                }
            }
            view.setGroupCount(groupCount);
            view.setGroupStartRow(startGroup);
            view.setFirstRowNewTier(false);
            view.setPageCount(pageCount);
            view.setPage(startGroup / VISIBLE_ROWS);
        } else {
            int rowCount = (all.size() + GRID_COLUMNS - 1) / GRID_COLUMNS;
            int pageCount = Math.max(1, (rowCount + VISIBLE_ROWS - 1) / VISIBLE_ROWS);
            int maxRow = Math.max(0, rowCount - VISIBLE_ROWS);
            int startRow = Math.max(0, Math.min(maxRow, requestedRow));
            int start = startRow * GRID_COLUMNS;
            int end = Math.min(all.size(), start + PREVIEW_PAGE_SIZE);
            boolean firstRowNewTier = start <= 0 || (start < all.size() && all.get(start)
                .getVoltageTier()
                != all.get(start - 1)
                    .getVoltageTier());
            for (int index = start; index < end; index++) {
                PatternEntry entry = all.get(index);
                view.addEntry(entry);
                addRecipeSnapshot(view, snapshotsByKey, entry);
            }
            view.setGroupCount(0);
            view.setGroupStartRow(0);
            view.setFirstRowNewTier(firstRowNewTier);
            view.setPageCount(pageCount);
            view.setPage(startRow / VISIBLE_ROWS);
        }
        for (PatternFilter filter : filters) {
            view.addFilter(filter);
        }
        view.setStatus(status);
        view.setPublished(getAllowedCount());
        view.setSkipped(getSkippedTotal());
        view.setQolAvailable(Loader.isModLoaded("ae2_qof"));
        view.setDeletedCount(deleted.size());
        view.setMatchedCount(all.size());
        view.setFilterMode(filterMode);
        view.setSmartDoubling(smartDoubling);
        view.setMaxRounds(maxRounds);
        view.setTotalEntries(allEntries.size());
        view.setSelectedPool(poolIndex);
        view.setConfiguredPools(configured);
        return view;
    }

    private static void addRecipeSnapshot(PatternView view, Map<String, RecipeSnapshot> snapshots, PatternEntry entry) {
        if (entry == null || entry.getKey() == null) {
            return;
        }
        RecipeSnapshot snapshot = snapshots.get(entry.getKey());
        if (snapshot != null) {
            view.addRecipeSnapshot(entry.getKey(), snapshot.toNBT());
        }
    }

    private List<PatternGroupView> buildGroups(List<PatternEntry> entries) {
        Map<String, PatternGroupView> byOutput = new LinkedHashMap<>();
        for (PatternEntry entry : entries) {
            String key = entry.getFilterKey();
            if (key == null || key.isEmpty()) {
                key = "recipe:" + (entry.getKey() == null ? "" : entry.getKey());
            }
            PatternGroupView group = byOutput.get(key);
            if (group == null) {
                group = new PatternGroupView(key, entry.getFilterName());
                byOutput.put(key, group);
            }
            group.addEntry(entry);
        }
        return new ArrayList<>(byOutput.values());
    }

    private void save() {
        if (stack == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(TAG_FORMAT, CONFIG_FORMAT);
        NBTTagList poolList = new NBTTagList();
        if (!pools.isEmpty()) {
            poolList.appendTag(
                pools.get(0)
                    .toNBT());
        }
        tag.setTag(TAG_POOLS, poolList);

        // Keep the old root projection for migration tools and older builds.
        RecipePoolSlot legacy = pools.get(0);
        ItemStack machine = legacy.getMachine();
        if (machine == null) {
            tag.removeTag(TAG_MACHINE);
            tag.removeTag("MachineFingerprint");
            tag.removeTag("MachineName");
        } else {
            tag.setTag(TAG_MACHINE, machine.writeToNBT(new NBTTagCompound()));
            String fingerprint = GTMachineScanner.getFingerprint(machine);
            if (fingerprint != null) {
                tag.setString("MachineFingerprint", fingerprint);
                tag.setString("MachineName", machine.getDisplayName());
            }
        }
        NBTTagList legacyEntries = new NBTTagList();
        for (PatternEntry entry : legacy.getEntries()) {
            legacyEntries.appendTag(entry.toNBT());
        }
        tag.setTag(TAG_ENTRIES, legacyEntries);
        NBTTagList filterList = new NBTTagList();
        for (PatternFilter filter : filters) {
            filterList.appendTag(filter.toNBT());
        }
        tag.setTag(TAG_FILTERS, filterList);
        tag.setByte(TAG_MODE, (byte) filterMode);
        tag.setBoolean(TAG_SMART, smartDoubling);
        tag.setBoolean(TAG_GROUP_SAME, groupSameOutput);
        tag.setInteger(TAG_MAX_ROUNDS, maxRounds);
        NBTTagList deletedList = new NBTTagList();
        for (String key : deleted) {
            deletedList.appendTag(new net.minecraft.nbt.NBTTagString(key));
        }
        tag.setTag(TAG_DELETED, deletedList);
        if (onSaved != null) {
            onSaved.run();
        }
    }

    private void viewChanged() {
        if (onViewChanged != null) {
            onViewChanged.run();
        }
    }

    private void changed() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private static final class EntryRef {

        private final RecipePoolSlot pool;
        private final int localIndex;
        private final PatternEntry entry;

        private EntryRef(RecipePoolSlot pool, int localIndex, PatternEntry entry) {
            this.pool = pool;
            this.localIndex = localIndex;
            this.entry = entry;
        }
    }
}
