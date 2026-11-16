package com.wztwzt.voidfabricator.client;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.pattern.PatternEntry;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;

import codechicken.nei.ItemPanels;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;

/** Bridges the hovered virtual recipe cell to NEI's normal A, R and P pages. */
public final class PatternNeiActions {

    private PatternNeiActions() {}

    public static boolean hasHover() {
        return ClientPatternHover.entry() != null;
    }

    /**
     * Adds or removes the hovered output in NEI's bookmark panel.
     *
     * <p>
     * {@code BookmarkPanel.addOrRemoveItem} is NEI's public item-bookmark toggle
     * and does not depend on the physical NEI key being held, so literal A works
     * even if the player has rebound NEI's {@code bookmark.add} key. Fluid outputs
     * are wrapped in NEI's own fluid display stack, which preserves the fluid
     * texture in the bookmark panel instead of substituting a bucket.
     */
    public static void bookmark() {
        PatternEntry entry = ClientPatternHover.entry();
        if (entry == null || ItemPanels.bookmarkPanel == null) {
            return;
        }
        ItemStack display = entry.getDisplay();
        if (display == null) {
            FluidStack fluid = entry.getDisplayFluid();
            if (fluid != null) {
                FluidStack copy = fluid.copy();
                copy.amount = Math.max(1, copy.amount);
                display = ItemFluidDisplay.createStack(copy);
            }
        }
        if (display != null) {
            ItemPanels.bookmarkPanel.addOrRemoveItem(display.copy());
        }
    }

    public static void openRecipe() {
        PatternEntry entry = ClientPatternHover.entry();
        if (entry == null) {
            return;
        }
        FluidStack fluid = entry.getDisplayFluid();
        if (fluid != null) {
            FluidStack copy = fluid.copy();
            copy.amount = Math.max(1, copy.amount);
            GuiCraftingRecipe.openRecipeGui("liquid", copy);
            return;
        }
        ItemStack output = entry.getDisplay();
        if (output != null) {
            GuiCraftingRecipe.openRecipeGui("item", output.copy());
        }
    }

    public static void openPattern() {
        PatternEntry entry = ClientPatternHover.entry();
        if (entry == null || entry.getKey() == null) {
            return;
        }
        PatternGuiSessionAccess access = new PatternGuiSessionAccess();
        NBTTagCompound snapshotTag = access.snapshot(entry.getKey());
        if (snapshotTag == null) {
            return;
        }
        try {
            RecipeSnapshot snapshot = RecipeSnapshot.fromNBT(snapshotTag);
            ItemStack token = ItemMachineRecipePoolPattern.createToken(snapshot);
            if (!GuiUsageRecipe.openRecipeGui("void_pattern", token)) {
                // A normal AE pattern view is still a useful fallback when the custom
                // handler has not been registered yet.
                GuiUsageRecipe.openRecipeGui("pattern", token);
            }
        } catch (IllegalArgumentException ignored) {
            // A malformed snapshot must not take down the game GUI.
        }
    }

    private static final class PatternGuiSessionAccess {

        private NBTTagCompound snapshot(String key) {
            if (ClientPatternHover.session() == null) {
                return null;
            }
            return ClientPatternHover.session()
                .recipeSnapshot(key);
        }
    }
}
