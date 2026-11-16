package com.wztwzt.voidfabricator.recipe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEItemStack;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.ItemMachines;

/** Read-only adapter for ordinary GregTech recipe-map machines. */
public final class GTMachineScanner {

    private static final Logger LOGGER = LogManager.getLogger(GTMachineScanner.class);
    private static final String ADAPTER_VERSION = "gt-generic-v1";

    private GTMachineScanner() {}

    public static boolean isMachine(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemMachines;
    }

    public static String getFingerprint(ItemStack stack) {
        if (!isMachine(stack)) {
            return null;
        }
        IMetaTileEntity machine = ItemMachines.getMetaTileEntity(stack);
        if (machine == null) {
            return null;
        }
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        return String.valueOf(registryName) + ':'
            + stack.getItemDamage()
            + '@'
            + machine.getClass()
                .getName();
    }

    public static ScanResult scan(ItemStack stack) {
        if (!isMachine(stack)) {
            throw new IllegalArgumentException("The installed item is not a GregTech machine");
        }
        IMetaTileEntity machine = ItemMachines.getMetaTileEntity(stack);
        if (machine == null) {
            throw new IllegalArgumentException("The GregTech machine did not provide a meta tile entity");
        }

        Map<String, RecipeMap<?>> recipeMaps = discoverRecipeMaps(machine);
        Map<String, Integer> skipped = new TreeMap<>();
        Map<String, RecipeSnapshot> recipes = new LinkedHashMap<>();
        for (RecipeMap<?> recipeMap : recipeMaps.values()) {
            for (GTRecipe recipe : recipeMap.getAllRecipes()) {
                try {
                    RecipeSnapshot snapshot = convert(recipeMap, recipe, skipped);
                    if (snapshot != null) {
                        RecipeSnapshot previous = recipes.putIfAbsent(
                            snapshot.getKey()
                                .value(),
                            snapshot);
                        if (previous != null) {
                            increment(skipped, "duplicate");
                        }
                    }
                } catch (RuntimeException exception) {
                    increment(skipped, "conversion_error");
                    LOGGER.warn("Failed to convert GT recipe from {}", recipeMap.unlocalizedName, exception);
                }
            }
        }
        return new ScanResult(
            getFingerprint(stack),
            recipeMaps.size(),
            Collections.unmodifiableList(new ArrayList<>(recipes.values())),
            skipped);
    }

    private static Map<String, RecipeMap<?>> discoverRecipeMaps(IMetaTileEntity machine) {
        Map<String, RecipeMap<?>> maps = new LinkedHashMap<>();
        if (machine instanceof RecipeMapWorkable workable) {
            addMaps(maps, workable.getAvailableRecipeMaps());
            addMap(maps, workable.getRecipeMap());
        } else {
            try {
                Method available = machine.getClass()
                    .getMethod("getAvailableRecipeMaps");
                Object result = available.invoke(machine);
                if (result instanceof Collection<?>collection) {
                    addMaps(maps, collection);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.debug(
                    "Machine {} has no readable getAvailableRecipeMaps method: {}",
                    machine.getClass()
                        .getName(),
                    exception.toString());
            }
            try {
                Method current = machine.getClass()
                    .getMethod("getRecipeMap");
                Object result = current.invoke(machine);
                if (result instanceof RecipeMap<?>recipeMap) {
                    addMap(maps, recipeMap);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.debug(
                    "Machine {} has no readable getRecipeMap method: {}",
                    machine.getClass()
                        .getName(),
                    exception.toString());
            }
        }
        return maps;
    }

    private static void addMaps(Map<String, RecipeMap<?>> maps, Collection<?> candidates) {
        if (candidates == null) {
            return;
        }
        for (Object candidate : candidates) {
            if (candidate instanceof RecipeMap<?>recipeMap) {
                addMap(maps, recipeMap);
            }
        }
    }

    private static void addMap(Map<String, RecipeMap<?>> maps, RecipeMap<?> recipeMap) {
        if (recipeMap != null) {
            maps.putIfAbsent(recipeMap.unlocalizedName, recipeMap);
        }
    }

    private static RecipeSnapshot convert(RecipeMap<?> recipeMap, GTRecipe recipe, Map<String, Integer> skipped) {
        if (recipe == null || !recipe.mEnabled || recipe.mFakeRecipe) {
            increment(skipped, "disabled_or_fake");
            return null;
        }
        if (hasChance(recipe.mInputChances) || hasChance(recipe.mOutputChances)
            || hasChance(recipe.mFluidInputChances)
            || hasChance(recipe.mFluidOutputChances)) {
            increment(skipped, "chance_semantics");
            return null;
        }
        if (hasAlternativeFluids(recipe.mAltFluidInputs)) {
            increment(skipped, "alternative_fluids");
            return null;
        }

        List<IAEStack<?>> inputs = new ArrayList<>();
        List<IAEStack<?>> virtualInputs = new ArrayList<>();
        List<IAEStack<?>> outputs = new ArrayList<>();
        Set<GTUtility.ItemId> nonConsumed = recipeMap.getNonConsumedInputItemIds();

        if (recipe.mInputs != null) {
            for (ItemStack input : recipe.mInputs) {
                if (input == null) {
                    continue;
                }
                boolean nonConsuming = input.stackSize <= 0 || isNonConsumed(nonConsumed, input);
                if (nonConsuming) {
                    ItemStack copy = input.copy();
                    copy.stackSize = 1;
                    addStack(virtualInputs, AEItemStack.create(copy));
                } else {
                    addStack(inputs, AEItemStack.create(input.copy()));
                }
            }
        }
        if (recipe.mFluidInputs != null) {
            for (FluidStack input : recipe.mFluidInputs) {
                if (input == null) {
                    continue;
                }
                if (input.amount <= 0) {
                    FluidStack copy = input.copy();
                    copy.amount = 1;
                    addStack(
                        virtualInputs,
                        AEApi.instance()
                            .storage()
                            .createFluidStack(copy));
                } else {
                    addStack(
                        inputs,
                        AEApi.instance()
                            .storage()
                            .createFluidStack(input.copy()));
                }
            }
        }

        if (!appendSpecialItems(recipe.mSpecialItems, virtualInputs)) {
            increment(skipped, "special_items");
            return null;
        }
        if (inputs.isEmpty()) {
            increment(skipped, "no_real_inputs");
            return null;
        }

        if (recipe.mOutputs != null) {
            for (ItemStack output : recipe.mOutputs) {
                if (output != null && output.stackSize > 0) {
                    addStack(outputs, AEItemStack.create(output.copy()));
                }
            }
        }
        if (recipe.mFluidOutputs != null) {
            for (FluidStack output : recipe.mFluidOutputs) {
                if (output != null && output.amount > 0) {
                    addStack(
                        outputs,
                        AEApi.instance()
                            .storage()
                            .createFluidStack(output.copy()));
                }
            }
        }
        if (outputs.isEmpty()) {
            increment(skipped, "no_outputs");
            return null;
        }

        String source = recipeMap.unlocalizedName + '#' + ADAPTER_VERSION;
        return new RecipeSnapshot(
            source,
            inputs,
            virtualInputs,
            outputs,
            recipe.mEUt,
            recipe.mDuration,
            recipe.mSpecialValue);
    }

    private static boolean appendSpecialItems(Object special, List<IAEStack<?>> virtualInputs) {
        if (special == null || special instanceof Number || special instanceof String) {
            return true;
        }
        if (special instanceof ItemStack stack) {
            ItemStack copy = stack.copy();
            copy.stackSize = 1;
            addStack(virtualInputs, AEItemStack.create(copy));
            return true;
        }
        if (special instanceof ItemStack[]stacks) {
            for (ItemStack stack : stacks) {
                if (stack != null) {
                    ItemStack copy = stack.copy();
                    copy.stackSize = 1;
                    addStack(virtualInputs, AEItemStack.create(copy));
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isNonConsumed(Set<GTUtility.ItemId> nonConsumed, ItemStack input) {
        if (nonConsumed == null || nonConsumed.isEmpty()) {
            return false;
        }
        try {
            return nonConsumed.contains(GTUtility.ItemId.create(input));
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not create non-consumed item id for {}: {}", input, exception.toString());
            return false;
        }
    }

    private static boolean hasAlternativeFluids(FluidStack[][] alternatives) {
        if (alternatives == null) {
            return false;
        }
        for (FluidStack[] entry : alternatives) {
            if (entry != null && entry.length > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasChance(int[] chances) {
        if (chances == null) {
            return false;
        }
        for (int chance : chances) {
            if (chance != 10_000) {
                return true;
            }
        }
        return false;
    }

    private static void addStack(List<IAEStack<?>> stacks, IAEStack<?> stack) {
        if (stack == null || stack.getStackSize() <= 0) {
            throw new IllegalArgumentException("GregTech recipe contains an invalid AE stack");
        }
        stacks.add(stack);
    }

    private static void increment(Map<String, Integer> skipped, String reason) {
        skipped.merge(reason, 1, Integer::sum);
    }
}
