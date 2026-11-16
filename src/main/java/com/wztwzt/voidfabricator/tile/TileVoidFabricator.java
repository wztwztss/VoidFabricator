package com.wztwzt.voidfabricator.tile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.item.IItemHandlerModifiable;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.wztwzt.voidfabricator.VoidFabricator;
import com.wztwzt.voidfabricator.ae.OutputBuffer;
import com.wztwzt.voidfabricator.ae.VirtualPatternDetails;
import com.wztwzt.voidfabricator.gui.VoidFabricatorGui;
import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.pattern.PatternEntry;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.RecipePoolSlot;
import com.wztwzt.voidfabricator.recipe.GTMachineScanner;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;
import com.wztwzt.voidfabricator.recipe.ScanResult;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import appeng.util.inv.MEInventoryCrafting;
import cpw.mods.fml.common.Loader;

public class TileVoidFabricator extends AENetworkTile
    implements ICraftingProvider, ICraftingMedium, IGuiHolder<PosGuiData>, IItemHandlerModifiable {

    public static final int PATTERN_SLOTS = 27;
    public static final int SLOT_PATTERN = 0;
    private static final int MAX_MULTIPLIER = 256;

    private static final Logger LOGGER = LogManager.getLogger(TileVoidFabricator.class);

    private final ItemStack[] patterns = new ItemStack[PATTERN_SLOTS];
    private final PatternPoolData[] patternData = new PatternPoolData[PATTERN_SLOTS];
    private final Map<VirtualPatternDetails, List<PatternPoolData>> patternOwners = new IdentityHashMap<>();
    private ItemStack legacyMachine;
    private volatile List<VirtualPatternDetails> publishedPatterns = Collections.emptyList();
    private final OutputBuffer outputBuffer = new OutputBuffer();
    private String lastError = "empty";
    private ICraftingMedium craftingMedium;
    private boolean mediumResolved;
    private boolean reloading;

    @Override
    protected AENetworkProxy createProxy() {
        AENetworkProxy proxy = super.createProxy();
        proxy.setIdlePowerUsage(0);
        return proxy;
    }

    @Override
    public void onReady() {
        super.onReady();
        if (worldObj != null && !worldObj.isRemote) {
            reloadPatterns();
        }
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings uiSettings) {
        return VoidFabricatorGui.build(this, data, syncManager, uiSettings);
    }

    @TileEvent(TileEventType.TICK)
    public void tickVoidFabricator() {
        if (worldObj == null || worldObj.isRemote || outputBuffer.isEmpty()) {
            return;
        }
        try {
            outputBuffer.flush(getProxy().getStorage(), this);
            if (outputBuffer.isEmpty()) {
                markDirty();
            }
        } catch (GridAccessException exception) {
            // Accepted outputs remain in the persistent buffer while disconnected.
        }
    }

    @TileEvent(TileEventType.WORLD_NBT_WRITE)
    public void writeToNBT_VoidFabricator(NBTTagCompound data) {
        NBTTagList list = new NBTTagList();
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            if (patterns[slot] == null) {
                continue;
            }
            NBTTagCompound entry = patterns[slot].writeToNBT(new NBTTagCompound());
            entry.setInteger("Slot", slot);
            list.appendTag(entry);
        }
        data.setTag("Patterns", list);
        if (patterns[0] != null) {
            data.setTag("PoolPattern", patterns[0].writeToNBT(new NBTTagCompound()));
        }
        if (legacyMachine != null) {
            data.setTag("Machine", legacyMachine.writeToNBT(new NBTTagCompound()));
        }
        data.setTag("OutputBuffer", outputBuffer.writeNBT());
    }

    @TileEvent(TileEventType.WORLD_NBT_READ)
    public void readFromNBT_VoidFabricator(NBTTagCompound data) {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            patterns[slot] = null;
            patternData[slot] = null;
        }
        NBTTagList list = data.getTagList("Patterns", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            int slot = entry.getInteger("Slot");
            if (slot >= 0 && slot < PATTERN_SLOTS) {
                patterns[slot] = ItemStack.loadItemStackFromNBT(entry);
            }
        }
        if (patterns[0] == null && data.hasKey("PoolPattern", 10)) {
            patterns[0] = ItemStack.loadItemStackFromNBT(data.getCompoundTag("PoolPattern"));
        }
        legacyMachine = data.hasKey("Machine", 10) ? ItemStack.loadItemStackFromNBT(data.getCompoundTag("Machine"))
            : null;
        outputBuffer.readNBT(data.getCompoundTag("OutputBuffer"));
        publishedPatterns = Collections.emptyList();
        patternOwners.clear();
        lastError = "awaiting_ready";
    }

    public PatternPoolData getPatternData(int slot) {
        if (slot < 0 || slot >= PATTERN_SLOTS || patterns[slot] == null) {
            return null;
        }
        if (patternData[slot] == null || patternData[slot].getStack() != patterns[slot]) {
            patternData[slot] = PatternPoolData.fromStack(patterns[slot]);
            patternData[slot].setOnChanged(this::onPatternEdited);
        }
        return patternData[slot];
    }

    /** Legacy accessor for old editor classes; the block GUI uses the indexed accessor. */
    public PatternPoolData getPatternData() {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    public int getConfiguredPatternCount() {
        int count = 0;
        for (ItemStack pattern : patterns) {
            if (pattern != null) {
                count++;
            }
        }
        return count;
    }

    public void onPatternEdited() {
        if (reloading || worldObj == null || worldObj.isRemote) {
            return;
        }
        markDirty();
        reloadPatterns();
    }

    @Override
    public void provideCrafting(ICraftingProviderHelper helper) {
        ICraftingMedium medium = getCraftingMedium();
        for (VirtualPatternDetails pattern : publishedPatterns) {
            helper.addCraftingOption(medium, pattern);
        }
    }

    private ICraftingMedium getCraftingMedium() {
        if (!mediumResolved) {
            mediumResolved = true;
            if (Loader.isModLoaded("ae2_qof")) {
                try {
                    Class<?> integration = Class.forName("com.wztwzt.voidfabricator.ae.SmartDoublingIntegration");
                    Method create = integration.getMethod("create", TileVoidFabricator.class);
                    craftingMedium = (ICraftingMedium) create.invoke(null, this);
                } catch (Throwable throwable) {
                    LOGGER.warn("AE2-QoL smart doubling bridge unavailable; using single-round provider", throwable);
                    craftingMedium = this;
                }
            } else {
                craftingMedium = this;
            }
        }
        return craftingMedium == null ? this : craftingMedium;
    }

    @Override
    public boolean isBusy() {
        if (outputBuffer.isSaturated() || !getProxy().isActive()) {
            return true;
        }
        return outputBuffer.remainingStackTypes() < maxPublishedOutputTypes();
    }

    private int maxPublishedOutputTypes() {
        int maximum = 1;
        for (VirtualPatternDetails pattern : publishedPatterns) {
            IAEStack<?>[] outputs = pattern.getOutputs();
            if (outputs != null && outputs.length > maximum) {
                maximum = outputs.length;
            }
        }
        return maximum;
    }

    public boolean isSmartDoublingEnabled() {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data != null && data.isSmartDoubling()) {
                return true;
            }
        }
        return false;
    }

    public void setSmartDoubling(boolean enabled) {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data != null) {
                data.setSmartDoubling(enabled);
            }
        }
    }

    public int getMaxMultiplier(ICraftingPatternDetails details) {
        int outputTypes = 1;
        if (details != null && details.getOutputs() != null && details.getOutputs().length > 0) {
            outputTypes = details.getOutputs().length;
        }
        long remaining = Math.min(outputBuffer.remainingBatches(), outputBuffer.remainingBatchesFor(outputTypes));
        int configured = 0;
        List<PatternPoolData> owners = patternOwners.get(details);
        if (owners != null && !owners.isEmpty()) {
            configured = Integer.MAX_VALUE;
            for (PatternPoolData owner : owners) {
                configured = Math.min(configured, owner.getMaxRounds());
            }
        } else {
            PatternPoolData data = getPatternData();
            configured = data == null ? 0 : data.getMaxRounds();
        }
        boolean doublingAllowed = owners == null || owners.isEmpty() ? isSmartDoublingEnabled() : true;
        if (owners != null) {
            for (PatternPoolData owner : owners) {
                doublingAllowed &= owner.isSmartDoubling();
            }
        }
        long cap = !doublingAllowed ? 1 : configured > 0 ? Math.min(remaining, configured) : remaining;
        return (int) Math.max(1, Math.min(MAX_MULTIPLIER, cap));
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting craftingInv) {
        if (worldObj == null || worldObj.isRemote
            || !getProxy().isActive()
            || !(details instanceof VirtualPatternDetails virtualDetails)
            || !(craftingInv instanceof MEInventoryCrafting meInventory)) {
            return false;
        }

        List<IAEStack<?>> outputs = virtualDetails.getSnapshot()
            .getOutputs();
        int multiplier = validateInputs(meInventory, virtualDetails.getAEInputs());
        if (multiplier <= 0 || multiplier > MAX_MULTIPLIER || !outputBuffer.canAccept(outputs, multiplier)) {
            return false;
        }

        for (int slot = 0; slot < virtualDetails.getAEInputs().length; slot++) {
            meInventory.setInventorySlotContents(slot, (IAEStack<?>) null);
        }
        outputBuffer.add(outputs, multiplier);
        markDirty();
        return true;
    }

    private static int validateInputs(MEInventoryCrafting inventory, IAEStack<?>[] expectedInputs) {
        if (inventory.getSizeInventory() != expectedInputs.length) {
            return 0;
        }
        int multiplier = -1;
        for (int slot = 0; slot < expectedInputs.length; slot++) {
            IAEStack<?> expected = expectedInputs[slot];
            IAEStack<?> actual = inventory.getAEStackInSlot(slot);
            if (expected == null || actual == null || !actual.isSameType(expected)) {
                return 0;
            }
            long expectedSize = expected.getStackSize();
            long actualSize = actual.getStackSize();
            if (expectedSize <= 0 || actualSize <= 0 || actualSize % expectedSize != 0) {
                return 0;
            }
            int ratio = (int) (actualSize / expectedSize);
            if (ratio < 1 || (multiplier != -1 && ratio != multiplier)) {
                return 0;
            }
            multiplier = ratio;
        }
        return multiplier;
    }

    @Override
    public ItemStack getCrafterIcon() {
        return new ItemStack(VoidFabricator.voidFabricatorBlock);
    }

    /**
     * Handles only the interactions owned by this block. Any other held item is
     * deliberately passed through so AE cables, parts, wrenches and building
     * items keep their normal right-click behavior.
     */
    public boolean onUse(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        boolean blockInteraction = held == null || GTMachineScanner.isMachine(held)
            || ItemMachineRecipePoolPattern.isPoolPattern(held);
        if (!blockInteraction) {
            return false;
        }
        // A normal right-click with a pattern belongs to the item's own UI.
        if (held != null && ItemMachineRecipePoolPattern.isPoolPattern(held) && !player.isSneaking()) {
            return false;
        }
        if (worldObj == null || worldObj.isRemote) {
            return true;
        }
        if (held != null && ItemMachineRecipePoolPattern.isPoolPattern(held)) {
            ItemStack single = held.copy();
            single.stackSize = 1;
            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                if (patterns[slot] == null && insertItem(slot, single, false) == null) {
                    player.inventory.decrStackSize(player.inventory.currentItem, 1);
                    send(player, "pattern_installed", publishedPatterns.size());
                    break;
                }
            }
        } else if (GTMachineScanner.isMachine(held)) {
            send(player, "machine_in_pattern");
        }
        openGui(player);
        return true;
    }

    private void openGui(EntityPlayer player) {
        if (!worldObj.isRemote) {
            GuiFactories.tileEntity()
                .open(player, this);
        }
    }

    private void reloadPatterns() {
        boolean previous = reloading;
        reloading = true;
        patternOwners.clear();
        try {
            Map<String, VirtualPatternDetails> uniquePatterns = new LinkedHashMap<>();
            int configuredPatterns = getConfiguredPatternCount();
            int machines = 0;
            boolean scanError = false;
            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                PatternPoolData data = getPatternData(slot);
                if (data == null) {
                    continue;
                }
                if (legacyMachine != null && !data.hasMachine()) {
                    data.setMachine(legacyMachine.copy());
                    legacyMachine = null;
                }
                if (!data.hasMachine()) {
                    continue;
                }
                machines++;
                try {
                    data.ensureScans();
                    ScanResult scan = data.getScan();
                    if (scan == null) {
                        scanError = true;
                        continue;
                    }
                    Map<String, RecipeSnapshot> byKey = new LinkedHashMap<>();
                    for (RecipeSnapshot snapshot : scan.recipes()) {
                        byKey.put(
                            snapshot.getKey()
                                .value(),
                            snapshot);
                    }
                    for (PatternEntry entry : data.getEntries()) {
                        if (!data.isEntryAllowed(entry)) {
                            continue;
                        }
                        RecipeSnapshot snapshot = byKey.get(entry.getKey());
                        if (snapshot == null) {
                            continue;
                        }
                        VirtualPatternDetails pattern = uniquePatterns.get(entry.getKey());
                        if (pattern == null) {
                            pattern = new VirtualPatternDetails(snapshot);
                            uniquePatterns.put(entry.getKey(), pattern);
                            patternOwners.put(pattern, new ArrayList<>());
                        }
                        List<PatternPoolData> owners = patternOwners.get(pattern);
                        if (!owners.contains(data)) {
                            owners.add(data);
                        }
                    }
                } catch (RuntimeException exception) {
                    scanError = true;
                    LOGGER.error("Could not scan pattern slot {} at {} {} {}", slot, xCoord, yCoord, zCoord, exception);
                }
            }
            publishedPatterns = Collections.unmodifiableList(new ArrayList<>(uniquePatterns.values()));
            if (configuredPatterns == 0) {
                lastError = "pattern_required";
            } else if (machines == 0) {
                lastError = "no_machine";
            } else if (scanError) {
                lastError = "scan_error";
            } else {
                lastError = publishedPatterns.isEmpty() ? "no_entries" : "ready";
            }
        } catch (RuntimeException exception) {
            lastError = "scan_error";
            publishedPatterns = Collections.emptyList();
            patternOwners.clear();
            LOGGER.error("Could not reload Void Fabricator patterns at {} {} {}", xCoord, yCoord, zCoord, exception);
        } finally {
            reloading = previous;
        }
        markDirty();
        postPatternChange();
    }

    public void rescan() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        boolean previous = reloading;
        reloading = true;
        try {
            for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
                PatternPoolData data = getPatternData(slot);
                if (data != null) {
                    data.rescanAll(true);
                }
            }
        } finally {
            reloading = previous;
        }
        reloadPatterns();
    }

    public boolean ejectPattern(EntityPlayer player, int slot) {
        if (worldObj == null || worldObj.isRemote || slot < 0 || slot >= PATTERN_SLOTS || patterns[slot] == null) {
            return false;
        }
        ItemStack detached = patterns[slot].copy();
        setStackInSlot(slot, null);
        giveOrDrop(player, detached, "pattern_inventory_full");
        return true;
    }

    public boolean ejectPattern(EntityPlayer player) {
        for (int slot = PATTERN_SLOTS - 1; slot >= 0; slot--) {
            if (ejectPattern(player, slot)) {
                return true;
            }
        }
        return false;
    }

    public boolean ejectMachine(EntityPlayer player) {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data != null && data.hasMachine()) {
                ItemStack detached = data.ejectFirstMachine();
                if (detached != null) {
                    giveOrDrop(player, detached, "machine_inventory_full");
                    reloadPatterns();
                    return true;
                }
            }
        }
        if (legacyMachine != null) {
            ItemStack detached = legacyMachine.copy();
            legacyMachine = null;
            giveOrDrop(player, detached, "machine_inventory_full");
            reloadPatterns();
            return true;
        }
        return false;
    }

    private void giveOrDrop(EntityPlayer player, ItemStack stack, String fullMessage) {
        if (player.inventory.addItemStackToInventory(stack)) {
            markDirty();
            return;
        }
        if (stack.stackSize > 0) {
            dropStack(stack);
        }
        send(player, fullMessage);
    }

    public String getStatusKey() {
        return lastError == null ? "ready" : lastError;
    }

    public int getPublishedPatternCount() {
        return publishedPatterns.size();
    }

    public long getPendingBatchCount() {
        return outputBuffer.getPendingBatches();
    }

    public int getPendingStackTypes() {
        return outputBuffer.getPendingStackTypes();
    }

    public int getSkippedRecipeCount() {
        int skipped = 0;
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data != null) {
                skipped += data.getSkippedTotal();
            }
        }
        return skipped;
    }

    public boolean isAeOnline() {
        return getProxy().isActive();
    }

    public boolean canExtractSlot(int slot) {
        return slot >= 0 && slot < PATTERN_SLOTS && patterns[slot] != null;
    }

    @Override
    public int getSlots() {
        return PATTERN_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < PATTERN_SLOTS ? patterns[slot] : null;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.stackSize <= 0) {
            return null;
        }
        ItemStack remainder = stack.copy();
        if (!isItemValid(slot, stack) || getStackInSlot(slot) != null) {
            return remainder;
        }
        remainder.stackSize--;
        if (!simulate) {
            ItemStack single = stack.copy();
            single.stackSize = 1;
            setStackInSlot(slot, single);
        }
        return remainder.stackSize > 0 ? remainder : null;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack current = getStackInSlot(slot);
        if (current == null || amount <= 0 || !canExtractSlot(slot)) {
            return null;
        }
        ItemStack extracted = current.copy();
        extracted.stackSize = Math.min(amount, current.stackSize);
        if (!simulate) {
            ItemStack remainder = current.copy();
            remainder.stackSize -= extracted.stackSize;
            setStackInSlot(slot, remainder.stackSize > 0 ? remainder : null);
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < PATTERN_SLOTS && ItemMachineRecipePoolPattern.isPoolPattern(stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= PATTERN_SLOTS) {
            return;
        }
        if (stack != null && stack.stackSize <= 0) {
            stack = null;
        }
        if (stack != null && !isItemValid(slot, stack)) {
            return;
        }
        ItemStack installed = stack == null ? null : stack.copy();
        if (installed != null) {
            installed.stackSize = 1;
        }
        patterns[slot] = installed;
        patternData[slot] = null;
        if (worldObj != null && worldObj.isRemote) {
            getPatternData(slot);
            return;
        }
        markDirty();
        reloadPatterns();
    }

    private void postPatternChange() {
        if (worldObj == null || worldObj.isRemote || !getProxy().isReady()) {
            return;
        }
        try {
            getProxy().getGrid()
                .postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
        } catch (GridAccessException exception) {
            LOGGER.debug(
                "Pattern update will be discovered when the AE grid becomes available: {}",
                exception.toString());
        }
    }

    private void sendStatus(EntityPlayer player) {
        player.addChatMessage(
            new ChatComponentTranslation(
                "voidfabricator.chat.status",
                publishedPatterns.size(),
                outputBuffer.getPendingBatches(),
                new ChatComponentTranslation("voidfabricator.gui.state." + getStatusKey())));
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            PatternPoolData data = getPatternData(slot);
            if (data == null) {
                continue;
            }
            for (RecipePoolSlot pool : data.getPools()) {
                ScanResult scan = pool.getScan();
                if (scan != null && !scan.skipped()
                    .isEmpty()) {
                    player.addChatMessage(new ChatComponentText("Pattern #" + (slot + 1) + ": " + scan.skipped()));
                }
            }
        }
    }

    private void send(EntityPlayer player, String suffix, Object... arguments) {
        player.addChatMessage(new ChatComponentTranslation("voidfabricator.chat." + suffix, arguments));
    }

    private void dropStack(ItemStack stack) {
        net.minecraft.entity.item.EntityItem entity = new net.minecraft.entity.item.EntityItem(
            worldObj,
            xCoord + 0.5,
            yCoord + 0.5,
            zCoord + 0.5,
            stack.copy());
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        worldObj.spawnEntityInWorld(entity);
        markDirty();
    }

    public void dropContents() {
        for (int slot = 0; slot < PATTERN_SLOTS; slot++) {
            if (patterns[slot] != null) {
                dropStack(patterns[slot]);
                patterns[slot] = null;
                patternData[slot] = null;
            }
        }
        if (legacyMachine != null) {
            dropStack(legacyMachine);
            legacyMachine = null;
        }
    }

    public boolean hasPendingOutputs() {
        return !outputBuffer.isEmpty();
    }
}
