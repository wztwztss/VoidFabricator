package com.wztwzt.voidfabricator.ae;

import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.recipe.RecipeSnapshot;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.PatternHelper;
import appeng.util.Platform;

/** Processing-pattern projection of an immutable GregTech recipe snapshot. */
public final class VirtualPatternDetails implements ICraftingPatternDetails {

    private final RecipeSnapshot snapshot;
    private final ItemStack pattern;
    private final IAEStack<?>[] inputs;
    private final IAEStack<?>[] condensedInputs;
    private final IAEStack<?>[] outputs;
    private final IAEStack<?>[] condensedOutputs;
    private volatile int priority;

    public VirtualPatternDetails(RecipeSnapshot snapshot) {
        this(snapshot, 0);
    }

    public VirtualPatternDetails(RecipeSnapshot snapshot, int priority) {
        this.snapshot = snapshot;
        this.pattern = ItemMachineRecipePoolPattern.createToken(snapshot);
        this.priority = priority;
        this.inputs = toArray(snapshot.getInputs());
        this.condensedInputs = PatternHelper.convertToCondensedAEList(copy(inputs));
        this.outputs = toArray(snapshot.getOutputs());
        this.condensedOutputs = PatternHelper.convertToCondensedAEList(copy(outputs));
    }

    public RecipeSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public ItemStack getPattern() {
        return pattern.copy();
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        throw new IllegalStateException("Virtual GT recipes are processing patterns, not crafting-table patterns");
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, IAEStack<?> stack, World world) {
        throw new IllegalStateException("Virtual GT recipes are processing patterns, not crafting-table patterns");
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return toLegacyItems(inputs);
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return toLegacyItems(condensedInputs);
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return toLegacyItems(condensedOutputs);
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return toLegacyItems(outputs);
    }

    @Override
    public IAEStack<?>[] getAEInputs() {
        return copy(inputs);
    }

    @Override
    public IAEStack<?>[] getAEOutputs() {
        return copy(outputs);
    }

    @Override
    public IAEStack<?>[] getCondensedAEInputs() {
        return copy(condensedInputs);
    }

    @Override
    public IAEStack<?>[] getCondensedAEOutputs() {
        return copy(condensedOutputs);
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public boolean canBeSubstitute() {
        return false;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        throw new IllegalStateException("Virtual GT recipes are processing patterns, not crafting-table patterns");
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VirtualPatternDetails details && snapshot.getKey()
            .equals(details.snapshot.getKey());
    }

    @Override
    public int hashCode() {
        return snapshot.getKey()
            .hashCode();
    }

    private static IAEStack<?>[] toArray(List<IAEStack<?>> stacks) {
        return stacks.toArray(new IAEStack<?>[0]);
    }

    private static IAEStack<?>[] copy(IAEStack<?>[] stacks) {
        IAEStack<?>[] copies = new IAEStack<?>[stacks.length];
        for (int index = 0; index < stacks.length; index++) {
            copies[index] = stacks[index].copy();
        }
        return copies;
    }

    private static IAEItemStack[] toLegacyItems(IAEStack<?>[] stacks) {
        IAEItemStack[] converted = new IAEItemStack[stacks.length];
        for (int index = 0; index < stacks.length; index++) {
            converted[index] = Platform.stackConvert(stacks[index]);
        }
        return converted;
    }
}
