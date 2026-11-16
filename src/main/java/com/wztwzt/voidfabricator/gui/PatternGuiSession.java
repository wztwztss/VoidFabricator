package com.wztwzt.voidfabricator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.value.sync.ByteArraySyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.wztwzt.voidfabricator.pattern.PatternEntry;
import com.wztwzt.voidfabricator.pattern.PatternFilter;
import com.wztwzt.voidfabricator.pattern.PatternGroupView;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.PatternPoolView;
import com.wztwzt.voidfabricator.pattern.PatternView;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

import cpw.mods.fml.common.FMLCommonHandler;

/** Shared state and server actions for one machine recipe pool and its visible scroll window. */
public final class PatternGuiSession {

    private final PatternView view;
    private final PatternPoolData data;
    private final TileVoidFabricator tile;
    private final boolean blockMode;
    private IntSyncValue selected;
    private IntSyncValue selectedPool;
    private ByteArraySyncValue viewSync;
    private int requestedPool;
    private int requestedRow;
    private double clientScrollPosition;
    private StringSyncValue queryValue;
    private IntSyncValue scrollValue;
    private String query = "";
    private int hoveredLocalIndex = -1;

    public PatternGuiSession(PatternView view, PatternPoolData data, TileVoidFabricator tile, boolean blockMode) {
        this.view = view;
        this.data = data;
        this.tile = tile;
        this.blockMode = blockMode;
        requestedPool = view.getSelectedPool();
        requestedRow = 0;
        if (data != null) {
            data.setOnChanged(this::afterChange);
            data.setOnViewChanged(this::afterViewChange);
        }
    }

    public void register(PanelSyncManager syncManager, String key) {
        viewSync = new ByteArraySyncValue(this::encodeView, this::decodeView);
        syncManager.syncValue(key + "_view", 0, viewSync);
        selected = new IntSyncValue(view::getSelected, view::setSelected);
        syncManager.syncValue(key + "_selected", 0, selected);
        selectedPool = new IntSyncValue(() -> requestedPool, this::setRequestedPool);
        syncManager.syncValue(key + "_pool", 0, selectedPool);
        queryValue = new StringSyncValue(() -> query, this::setQuery);
        queryValue.allowC2S();
        syncManager.syncValue(key + "_query", 0, queryValue);
        scrollValue = new IntSyncValue(() -> requestedRow, this::setRequestedRow);
        scrollValue.allowC2S();
        syncManager.syncValue(key + "_scroll", 0, scrollValue);
    }

    public StringSyncValue queryValue() {
        return queryValue;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.equals(query)) {
            return;
        }
        query = normalized;
        requestedRow = 0;
        clientScrollPosition = 0;
        afterViewChange();
    }

    private void refreshView() {
        if (data == null) {
            return;
        }
        PatternView snapshot = data.toView(requestedRow, requestedPool, query);
        requestedPool = snapshot.getSelectedPool();
        snapshot.setSelected(view.getSelected());
        view.copyFrom(snapshot);
    }

    private byte[] encodeView() {
        if (data != null) {
            refreshView();
            if (tile != null) {
                view.setPublished(tile.getPublishedPatternCount());
                view.setPendingBatches(tile.getPendingBatchCount());
                view.setPendingTypes(tile.getPendingStackTypes());
                view.setSkipped(tile.getSkippedRecipeCount());
                view.setOnline(tile.isAeOnline());
                view.setStatus(tile.getStatusKey());
            }
        }
        return view.toBytes();
    }

    private void decodeView(byte[] bytes) {
        if (data == null) {
            view.load(bytes);
            requestedPool = view.getSelectedPool();
            requestedRow = view.isGroupSameOutput() ? view.getGroupStartRow()
                : view.getPage() * PatternPoolData.VISIBLE_ROWS;
            int max = maxRow();
            clientScrollPosition = max == 0 ? 0 : (double) Math.max(0, Math.min(max, requestedRow)) / max;
        }
    }

    private void afterViewChange() {
        if (data != null) {
            refreshView();
            if (viewSync != null && !isClientSide()) {
                viewSync.notifyUpdate();
            }
        }
        if (tile != null && !isClientSide()) {
            tile.markDirty();
        }
    }

    private void afterChange() {
        afterViewChange();
        if (tile != null) {
            tile.onPatternEdited();
        }
    }

    public PatternView getView() {
        return view;
    }

    public boolean isBlockMode() {
        return blockMode;
    }

    public PatternPoolData getData() {
        return data;
    }

    public TileVoidFabricator getTile() {
        return tile;
    }

    public void select(int globalIndex) {
        if (globalIndex < 0) {
            globalIndex = 0;
        }
        view.setSelected(globalIndex);
        if (selected != null) {
            // A client must never call sync() on a value that is not marked
            // allowC2S: MUI2 throws "Sync handler is unable to send packets to
            // server!" and the GUI packet storm shows up as flickering panels.
            selected.setIntValue(globalIndex, true, !isClientSide());
        }
    }

    public int getSelectedIndex() {
        return view.getSelected();
    }

    public PatternEntry selectedEntry() {
        return view.selectedEntry();
    }

    public void selectPool(int index) {
        setRequestedPool(index);
        if (selectedPool != null) {
            selectedPool.setIntValue(requestedPool, true, !isClientSide());
        }
    }

    private static boolean isClientSide() {
        return FMLCommonHandler.instance()
            .getEffectiveSide()
            .isClient();
    }

    private void setRequestedPool(int index) {
        requestedPool = 0;
        view.setSelectedPool(0);
        PatternPoolView pool = view.pool(0);
        if (pool != null) {
            view.setMachine(pool.getMachine());
            view.setMachineName(pool.getMachineName());
        }
    }

    public int getSelectedPool() {
        return requestedPool;
    }

    public PatternPoolView pool(int index) {
        return view.pool(index);
    }

    private void setRequestedRow(int row) {
        requestedRow = Math.max(0, Math.min(maxRow(), row));
        afterViewChange();
    }

    public int rowCount() {
        if (view.isGroupSameOutput()) {
            return view.getGroupCount();
        }
        int matched = view.getMatchedCount();
        return (matched + PatternPoolData.GRID_COLUMNS - 1) / PatternPoolData.GRID_COLUMNS;
    }

    private int maxRow() {
        return Math.max(0, rowCount() - PatternPoolData.VISIBLE_ROWS);
    }

    public int startRow() {
        return data == null ? (int) Math.round(clientScrollPosition * maxRow()) : requestedRow;
    }

    public double scrollPosition() {
        if (data == null) {
            return clientScrollPosition;
        }
        int max = maxRow();
        return max == 0 ? 0 : (double) requestedRow / max;
    }

    public void setScrollPosition(double position) {
        double clamped = Math.max(0, Math.min(1, position));
        int target = (int) Math.round(clamped * maxRow());
        if (data == null) {
            clientScrollPosition = clamped;
            if (scrollValue != null) {
                scrollValue.setIntValue(target, true, true);
            }
            return;
        }
        setRequestedRow(target);
    }

    public void scroll(int amount) {
        if (amount == 0) {
            return;
        }
        if (data == null) {
            int max = maxRow();
            if (max == 0) {
                clientScrollPosition = 0;
                return;
            }
            int row = (int) Math.round(clientScrollPosition * max) + (amount > 0 ? -1 : 1);
            row = Math.max(0, Math.min(max, row));
            clientScrollPosition = (double) row / max;
            if (scrollValue != null) {
                scrollValue.setIntValue(row, true, true);
            }
            return;
        }
        setRequestedRow(requestedRow + (amount > 0 ? -1 : 1));
    }

    public void previousPage() {
        if (data != null) {
            setRequestedRow(requestedRow - PatternPoolData.VISIBLE_ROWS);
        }
    }

    public void nextPage() {
        if (data != null) {
            setRequestedRow(requestedRow + PatternPoolData.VISIBLE_ROWS);
        }
    }

    /** Resolves a visible cell from the current server or client projection. */
    public PatternEntry entryAt(int localIndex) {
        int row = localIndex / PatternPoolData.GRID_COLUMNS;
        int column = localIndex % PatternPoolData.GRID_COLUMNS;
        return view.entryAt(row, column);
    }

    public int entryGlobalIndex(int localIndex) {
        PatternEntry entry = entryAt(localIndex);
        return entry == null ? -1 : entry.getGlobalIndex();
    }

    public void setHovered(int localIndex) {
        hoveredLocalIndex = localIndex;
    }

    public void clearHovered(int localIndex) {
        if (hoveredLocalIndex == localIndex) {
            hoveredLocalIndex = -1;
        }
    }

    public PatternEntry hoveredEntry() {
        return hoveredLocalIndex < 0 ? null : entryAt(hoveredLocalIndex);
    }

    public NBTTagCompound recipeSnapshot(String key) {
        return view.getRecipeSnapshot(key);
    }

    public int groupSizeAt(int row) {
        PatternGroupView group = view.group(row);
        return group == null ? 0 : group.getTotalCount();
    }

    public void rescan() {
        if (data != null) {
            data.rescanPool(requestedPool, true);
        }
    }

    public void rescanAll() {
        if (data != null) {
            data.rescanAll(true);
        }
    }

    public void toggle(int globalIndex) {
        if (data != null) {
            data.toggle(globalIndex);
        }
    }

    public void setAllEnabled(boolean enabled) {
        if (data != null) {
            data.setAllEntriesEnabled(enabled);
        }
    }

    /** Toggles all entries in one voltage tier; enables when any entry is disabled. */
    public void toggleTier(int tier) {
        if (data == null || tier < 0) {
            return;
        }
        data.setEntriesEnabledByTier(tier, !data.isTierEnabled(tier));
    }

    public boolean isEntryFiltered(int localIndex) {
        PatternEntry entry = entryAt(localIndex);
        if (entry == null) {
            return false;
        }
        if (data != null) {
            return data.isEntryFiltered(entry);
        }
        if (view.getFilters()
            .isEmpty()) {
            return view.getFilterMode() == PatternPoolData.MODE_WHITELIST;
        }
        String key = entry.getFilterKey();
        boolean matched = false;
        for (PatternFilter filter : view.getFilters()) {
            if (key != null && key.equals(filter.getKey())) {
                matched = true;
                break;
            }
        }
        return view.getFilterMode() == PatternPoolData.MODE_WHITELIST ? !matched : matched;
    }

    public boolean isEntryActive(int localIndex) {
        PatternEntry entry = entryAt(localIndex);
        return entry != null && entry.isEnabled() && !isEntryFiltered(localIndex);
    }

    public boolean isGroupSameOutput() {
        return view.isGroupSameOutput();
    }

    public void toggleGroupSameOutput() {
        if (data != null) {
            data.toggleGroupSameOutput();
        }
    }

    public void toggleSameOutputFilter(int globalIndex) {
        if (data != null) {
            data.toggleSameOutputFilter(globalIndex);
        }
    }

    public void clearSameOutputFilter(int globalIndex) {
        if (data != null) {
            data.clearSameOutputFilter(globalIndex);
        }
    }

    public void restoreEntry(int globalIndex) {
        if (data == null) {
            return;
        }
        if (data.getFilterMode() == PatternPoolData.MODE_WHITELIST) {
            data.addFilterFromEntry(globalIndex);
        } else {
            data.clearSameOutputFilter(globalIndex);
        }
    }

    public void toggleSameOutputFilterSelected() {
        PatternEntry entry = selectedEntry();
        if (entry != null) {
            toggleSameOutputFilter(entry.getGlobalIndex());
        }
    }

    public void toggleSelected() {
        if (data != null) {
            data.toggle(view.getSelected());
        }
    }

    public void delete(int globalIndex) {
        if (data != null && data.delete(globalIndex)) {
            int size = data.getTotalEntryCount();
            if (view.getSelected() >= size) {
                select(Math.max(0, size - 1));
            }
            setRequestedRow(requestedRow);
        }
    }

    public void deleteSelected() {
        PatternEntry entry = selectedEntry();
        if (entry != null) {
            delete(entry.getGlobalIndex());
        }
    }

    public void addFilterFromEntry(int globalIndex) {
        if (data != null) {
            data.addFilterFromEntry(globalIndex);
        }
    }

    public void addFilterFromSelected() {
        addFilterFromEntry(view.getSelected());
    }

    public void removeFilter(int index) {
        if (data != null) {
            data.removeFilter(index);
        }
    }

    public void clearFilters() {
        if (data != null) {
            data.clearFilters();
            data.applyFilters();
        }
    }

    public void applyFilters() {
        if (data != null) {
            data.applyFilters();
        }
    }

    public void restoreDeleted() {
        if (data != null) {
            data.restoreDeleted();
        }
    }

    public void toggleFilterMode() {
        if (data != null) {
            data.setFilterMode(
                data.getFilterMode() == PatternPoolData.MODE_BLACKLIST ? PatternPoolData.MODE_WHITELIST
                    : PatternPoolData.MODE_BLACKLIST);
        }
    }

    public void setSmartDoubling(boolean enabled) {
        if (data != null) {
            data.setSmartDoubling(enabled);
        }
    }

    public void toggleSmartDoubling() {
        if (data != null) {
            data.setSmartDoubling(!data.isSmartDoubling());
        }
    }

    public void cycleMaxRounds() {
        int current = getMaxRounds();
        int next = 0;
        int[] steps = { 0, 1, 2, 4, 8, 16, 32, 64 };
        for (int step : steps) {
            if (step > current) {
                next = step;
                break;
            }
        }
        setMaxRounds(next);
    }

    public void setMaxRounds(int rounds) {
        if (data != null) {
            data.setMaxRounds(rounds);
        } else {
            view.setMaxRounds(Math.max(0, Math.min(PatternPoolData.MAX_ROUNDS, rounds)));
        }
    }

    public void setMachine(ItemStack stack) {
        ItemStack single = stack == null ? null : stack.copy();
        if (single != null) {
            single.stackSize = 1;
        }
        ItemStack current = getMachine();
        if (single == null ? current == null : ItemStack.areItemStacksEqual(current, single)) {
            return;
        }
        if (data != null) {
            data.setMachine(requestedPool, single);
        } else {
            view.setMachine(single);
            view.setMachineName(single == null ? null : single.getDisplayName());
            view.setStatus(single == null ? "no_machine" : "ready");
        }
    }

    public ItemStack getMachine() {
        return data == null ? view.getMachine() : data.getMachine(requestedPool);
    }

    public boolean ejectMachine(EntityPlayer player) {
        if (data != null) {
            ItemStack machine = data.getMachine(requestedPool);
            if (machine != null) {
                data.setMachine(requestedPool, null);
                if (!player.inventory.addItemStackToInventory(machine)) {
                    player.dropPlayerItemWithRandomChoice(machine, false);
                }
                return true;
            }
        }
        return tile != null && tile.ejectMachine(player);
    }

    public PatternFilter filter(int index) {
        return view.filter(index);
    }

    public int getMaxRounds() {
        return data == null ? view.getMaxRounds() : data.getMaxRounds();
    }

    public boolean isSmartDoubling() {
        return data == null ? view.isSmartDoubling() : data.isSmartDoubling();
    }

    public int getFilterMode() {
        return data == null ? view.getFilterMode() : data.getFilterMode();
    }

    public boolean isQolAvailable() {
        return view.isQolAvailable();
    }
}
