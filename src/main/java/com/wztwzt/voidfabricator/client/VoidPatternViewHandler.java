package com.wztwzt.voidfabricator.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import com.wztwzt.voidfabricator.ae.VirtualPatternDetails;
import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.api.IRecipeOverlayRenderer;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IUsageHandler;

/**
 * NEI usage page for a virtual GT recipe. It reuses the normal pattern-page
 * geometry but additionally prints EU/t, duration and the recipe source.
 */
public final class VoidPatternViewHandler implements IUsageHandler {

    private static final String INPUT_ID = "void_pattern";

    private final VirtualPatternDetails details;
    private final List<PositionedStack> inputs = new ArrayList<>();
    private final List<PositionedStack> otherOutputs = new ArrayList<>();
    private PositionedStack result;
    private String recipeName = "Void Fabricator";

    public VoidPatternViewHandler(ItemStack token) {
        VirtualPatternDetails resolved = null;
        if (token != null && ItemMachineRecipePoolPattern.isToken(token)) {
            NBTTagCompound tag = token.getTagCompound();
            if (tag != null && tag.hasKey("Snapshot", 10)) {
                try {
                    RecipeSnapshot snapshot = RecipeSnapshot.fromNBT(tag.getCompoundTag("Snapshot"));
                    resolved = new VirtualPatternDetails(snapshot);
                } catch (IllegalArgumentException ignored) {
                    // Malformed NBT is treated as an unavailable recipe page.
                }
            }
        }
        this.details = resolved;
        if (details != null) {
            RecipeSnapshot snapshot = details.getSnapshot();
            recipeName = snapshot.getOutputs()
                .isEmpty() ? "Void Fabricator"
                    : "Void Fabricator: " + displayName(
                        snapshot.getOutputs()
                            .get(0));
            buildStacks(details.getAEInputs(), details.getAEOutputs());
        }
    }

    private void buildStacks(IAEStack<?>[] recipeInputs, IAEStack<?>[] recipeOutputs) {
        int index = 0;
        for (IAEStack<?> stack : recipeInputs) {
            Object display = toNeiStack(stack);
            if (display != null) {
                inputs.add(new PositionedStack(display, 8 + (index % 9) * 18, 8 + (index / 9) * 18));
                index++;
            }
        }
        if (recipeOutputs.length > 0) {
            Object display = toNeiStack(recipeOutputs[0]);
            if (display != null) {
                result = new PositionedStack(display, 138, 40);
            }
        }
        for (int output = 1; output < recipeOutputs.length; output++) {
            Object display = toNeiStack(recipeOutputs[output]);
            if (display != null) {
                otherOutputs.add(new PositionedStack(display, 112 + (output - 1) * 18, 40));
            }
        }
    }

    private static String displayName(IAEStack<?> stack) {
        if (stack instanceof IAEItemStack item && item.getItemStack() != null) {
            return item.getItemStack()
                .getDisplayName();
        }
        if (stack instanceof IAEFluidStack fluid && fluid.getFluidStack() != null) {
            return fluid.getFluidStack()
                .getLocalizedName();
        }
        return "Output";
    }

    private static Object toNeiStack(IAEStack<?> stack) {
        if (stack instanceof IAEItemStack item && item.getItemStack() != null) {
            return item.getItemStack()
                .copy();
        }
        if (stack instanceof IAEFluidStack fluid && fluid.getFluidStack() != null) {
            FluidStack copy = fluid.getFluidStack()
                .copy();
            copy.amount = Math.max(1, copy.amount);
            return copy;
        }
        return null;
    }

    public static void register() {
        GuiUsageRecipe.registerUsageHandler(new VoidPatternViewHandler(null));
    }

    @Override
    public IUsageHandler getUsageHandler(String inputId, Object... ingredients) {
        if (!INPUT_ID.equals(inputId) || ingredients.length == 0) {
            return null;
        }
        Object ingredient = ingredients[0];
        ItemStack token = null;
        if (ingredient instanceof ItemStack) {
            token = (ItemStack) ingredient;
        } else if (ingredient instanceof ItemStack[]) {
            ItemStack[] array = (ItemStack[]) ingredient;
            token = array.length == 0 ? null : array[0];
        }
        if (token == null || !ItemMachineRecipePoolPattern.isToken(token)) {
            return null;
        }
        return new VoidPatternViewHandler(token);
    }

    @Override
    public String getRecipeName() {
        return recipeName;
    }

    @Override
    public int getRecipeHeight(int recipe) {
        return 118;
    }

    @Override
    public int numRecipes() {
        return details == null ? 0 : 1;
    }

    @Override
    public void drawBackground(int recipe) {
        // NEI supplies the normal recipe background.
    }

    @Override
    public void drawForeground(int recipe) {
        if (details == null) {
            return;
        }
        RecipeSnapshot snapshot = details.getSnapshot();
        Minecraft minecraft = Minecraft.getMinecraft();
        int tier = gregtech.api.util.GTUtility.getTier(Math.max(1L, snapshot.getEuPerTick()));
        String voltage = tier >= 0 && tier < gregtech.api.enums.GTValues.VN.length
            ? gregtech.api.enums.GTValues.VN[tier]
            : "?";
        int y = 78;
        minecraft.fontRenderer
            .drawString("Voltage: " + voltage + " / EU/t: " + snapshot.getEuPerTick(), 8, y, 0x404040);
        minecraft.fontRenderer.drawString("Duration: " + snapshot.getDuration() + " ticks", 8, y + 10, 0x404040);
        String source = snapshot.getSourceId();
        if (source != null && source.length() > 34) {
            source = source.substring(0, 31) + "...";
        }
        minecraft.fontRenderer.drawString("Source: " + (source == null ? "-" : source), 8, y + 20, 0x606060);
        if (result != null) {
            minecraft.fontRenderer.drawString("Output:", 112, 28, 0x404040);
        }
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        return details == null ? Collections.emptyList() : inputs;
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return details == null ? Collections.emptyList() : otherOutputs;
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        return details == null ? null : result;
    }

    @Override
    public void onUpdate() {
        // Static recipe page; nothing changes while open.
    }

    @Override
    public boolean hasOverlay(GuiContainer gui, Container container, int recipe) {
        return false;
    }

    @Override
    public IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe) {
        return null;
    }

    @Override
    public IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe) {
        return null;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currentTip, int recipe) {
        return currentTip;
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> currentTip, int recipe) {
        return currentTip;
    }

    @Override
    public boolean keyTyped(GuiRecipe<?> gui, char keyChar, int keyCode, int recipe) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        return false;
    }
}
