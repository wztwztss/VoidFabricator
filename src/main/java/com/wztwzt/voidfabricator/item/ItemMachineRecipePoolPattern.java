package com.wztwzt.voidfabricator.item;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiFactories;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.wztwzt.voidfabricator.VoidFabricator;
import com.wztwzt.voidfabricator.ae.VirtualPatternDetails;
import com.wztwzt.voidfabricator.gui.PatternItemGui;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.PatternView;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;

/** One physical pool configuration item and internal, self-contained virtual execution tokens. */
public class ItemMachineRecipePoolPattern extends Item implements ICraftingPatternItem, IGuiHolder<GuiData> {

    private static final Logger LOGGER = LogManager.getLogger(ItemMachineRecipePoolPattern.class);
    private static final String MACHINE_FINGERPRINT = "MachineFingerprint";
    private static final String MACHINE_NAME = "MachineName";
    private static final String TOKEN = "VirtualToken";
    private static final String SNAPSHOT = "Snapshot";

    public ItemMachineRecipePoolPattern() {
        setUnlocalizedName("voidfabricator.machine_recipe_pool_pattern");
        setTextureName("voidfabricator:machine_recipe_pool_pattern");
        setCreativeTab(CreativeTabs.tabTools);
        setMaxStackSize(1);
    }

    public static ItemStack createPoolPattern() {
        return new ItemStack(VoidFabricator.machineRecipePoolPattern);
    }

    public static ItemStack createToken(RecipeSnapshot snapshot) {
        ItemStack token = createPoolPattern();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(TOKEN, true);
        tag.setTag(SNAPSHOT, snapshot.toNBT());
        token.setTagCompound(tag);
        return token;
    }

    public static void bind(ItemStack pattern, String fingerprint) {
        bind(pattern, fingerprint, null);
    }

    public static void bind(ItemStack pattern, String fingerprint, String machineName) {
        if (pattern == null || fingerprint == null || isToken(pattern)) {
            throw new IllegalArgumentException("Cannot bind this pattern to the supplied machine");
        }
        NBTTagCompound tag = pattern.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            pattern.setTagCompound(tag);
        }
        tag.setString(MACHINE_FINGERPRINT, fingerprint);
        if (machineName == null || machineName.trim()
            .isEmpty()) {
            tag.removeTag(MACHINE_NAME);
        } else {
            tag.setString(MACHINE_NAME, machineName);
        }
    }

    public static String getBoundFingerprint(ItemStack pattern) {
        if (pattern == null || isToken(pattern) || !pattern.hasTagCompound()) {
            return null;
        }
        String fingerprint = pattern.getTagCompound()
            .getString(MACHINE_FINGERPRINT);
        return fingerprint.isEmpty() ? null : fingerprint;
    }

    public static String getBoundMachineName(ItemStack pattern) {
        if (getBoundFingerprint(pattern) == null) {
            return null;
        }
        String name = pattern.getTagCompound()
            .getString(MACHINE_NAME);
        return name.isEmpty() ? null : name;
    }

    public static boolean isPoolPattern(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemMachineRecipePoolPattern && !isToken(stack);
    }

    public static boolean isToken(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean(TOKEN);
    }

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        return PatternItemGui.build(this, data, syncManager, settings);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player != null && player.isSneaking()) {
            // Sneaking on the block belongs to the block's insert path. Opening both
            // screens from one click leaves two overlapping GUIs fighting for focus.
            return stack;
        }
        if (!world.isRemote && isPoolPattern(stack)) {
            GuiFactories.playerInventory()
                .openFromMainHand(player);
        }
        return stack;
    }

    @Override
    public ICraftingPatternDetails getPatternForItem(ItemStack stack, World world) {
        if (stack == null || stack.getItem() != this || !isToken(stack)) {
            return null;
        }
        try {
            NBTTagCompound snapshotTag = stack.getTagCompound()
                .getCompoundTag(SNAPSHOT);
            return new VirtualPatternDetails(RecipeSnapshot.fromNBT(snapshotTag));
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Could not restore virtual recipe pattern", exception);
            return null;
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        if (isToken(stack)) {
            NBTTagCompound snapshot = stack.getTagCompound()
                .getCompoundTag(SNAPSHOT);
            add(tooltip, "token.header", EnumChatFormatting.AQUA);
            add(tooltip, "token.description", EnumChatFormatting.GRAY);
            add(tooltip, "token.source", EnumChatFormatting.DARK_AQUA, snapshot.getString("Source"));
            add(tooltip, "token.key", EnumChatFormatting.DARK_GRAY, snapshot.getString("Key"));
            add(tooltip, "token.warning", EnumChatFormatting.RED);
            if (advanced) {
                add(
                    tooltip,
                    "token.counts",
                    EnumChatFormatting.DARK_GRAY,
                    snapshot.getTagList("Inputs", 10)
                        .tagCount(),
                    snapshot.getTagList("VirtualInputs", 10)
                        .tagCount(),
                    snapshot.getTagList("Outputs", 10)
                        .tagCount());
                add(
                    tooltip,
                    "token.stats",
                    EnumChatFormatting.DARK_GRAY,
                    snapshot.getInteger("EUt"),
                    snapshot.getInteger("Duration"),
                    snapshot.getInteger("SpecialValue"));
            }
            return;
        }

        String fingerprint = getBoundFingerprint(stack);
        PatternView view = PatternPoolData.fromStack(stack)
            .toView();
        add(tooltip, "header", EnumChatFormatting.AQUA);
        if (view.getConfiguredPools() == 0) {
            add(tooltip, "ui.machine.empty", EnumChatFormatting.YELLOW);
        } else {
            add(tooltip, "ui.machine", EnumChatFormatting.GREEN, view.getMachineName());
        }
        add(tooltip, "ui.entries", EnumChatFormatting.AQUA, view.getTotalEntries(), view.getPublished());
        add(
            tooltip,
            "ui.filters",
            EnumChatFormatting.GRAY,
            view.getFilters()
                .size(),
            view.isWhitelist() ? StatCollector.translateToLocal("voidfabricator.pattern.filter.mode.white")
                : StatCollector.translateToLocal("voidfabricator.pattern.filter.mode.black"));
        add(
            tooltip,
            view.isSmartDoubling() ? "ui.doubling.on" : "ui.doubling.off",
            view.isQolAvailable() ? EnumChatFormatting.GREEN : EnumChatFormatting.DARK_GRAY);
        add(tooltip, "ui.open", EnumChatFormatting.YELLOW);
        add(tooltip, "ui.install", EnumChatFormatting.GOLD);
        add(tooltip, "ui.binding", EnumChatFormatting.DARK_AQUA);
        if (advanced) {
            String value = fingerprint == null
                ? StatCollector.translateToLocal("voidfabricator.tooltip.pattern.not_bound")
                : fingerprint;
            add(tooltip, "advanced.fingerprint", EnumChatFormatting.DARK_GRAY, value);
        }
    }

    private static void add(List<String> tooltip, String suffix, EnumChatFormatting color, Object... arguments) {
        tooltip.add(
            color + StatCollector.translateToLocalFormatted("voidfabricator.tooltip.pattern." + suffix, arguments));
    }
}
