package com.wztwzt.voidfabricator.recipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;

/** Immutable, self-contained recipe data used both for planning and AE task restoration. */
public final class RecipeSnapshot {

    public static final int NBT_FORMAT = 1;
    private static final int MAX_STACKS = 256;
    private static final Field NBT_COMPOUND_MAP = findCompoundMapField();

    private final String sourceId;
    private final List<IAEStack<?>> inputs;
    private final List<IAEStack<?>> virtualInputs;
    private final List<IAEStack<?>> outputs;
    private final int euPerTick;
    private final int duration;
    private final int specialValue;
    private final RecipeKey key;

    public RecipeSnapshot(String sourceId, List<IAEStack<?>> inputs, List<IAEStack<?>> virtualInputs,
        List<IAEStack<?>> outputs, int euPerTick, int duration, int specialValue) {
        this(sourceId, inputs, virtualInputs, outputs, euPerTick, duration, specialValue, true);
    }

    /** Scanner-owned lists are never mutated after construction; avoid a second deep copy. */
    static RecipeSnapshot fromScan(String sourceId, List<IAEStack<?>> inputs, List<IAEStack<?>> virtualInputs,
        List<IAEStack<?>> outputs, int euPerTick, int duration, int specialValue) {
        return new RecipeSnapshot(sourceId, inputs, virtualInputs, outputs, euPerTick, duration, specialValue, false);
    }

    private RecipeSnapshot(String sourceId, List<IAEStack<?>> inputs, List<IAEStack<?>> virtualInputs,
        List<IAEStack<?>> outputs, int euPerTick, int duration, int specialValue, boolean defensiveCopy) {
        if (sourceId == null || sourceId.trim()
            .isEmpty() || sourceId.length() > 512) {
            throw new IllegalArgumentException("Invalid recipe source id");
        }
        this.sourceId = sourceId;
        this.inputs = copyStacks(inputs, "inputs", defensiveCopy);
        this.virtualInputs = copyStacks(virtualInputs, "virtualInputs", defensiveCopy);
        this.outputs = copyStacks(outputs, "outputs", defensiveCopy);
        if (this.inputs.isEmpty()) {
            throw new IllegalArgumentException("AE processing patterns require at least one real input");
        }
        if (this.outputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe must declare at least one output");
        }
        this.euPerTick = euPerTick;
        this.duration = duration;
        this.specialValue = specialValue;
        this.key = RecipeKey.fromCanonical(canonical());
    }

    public RecipeKey getKey() {
        return key;
    }

    public String getSourceId() {
        return sourceId;
    }

    public List<IAEStack<?>> getInputs() {
        return copyStacks(inputs, "inputs");
    }

    public int getInputCount() {
        return inputs.size();
    }

    /** Internal read-only view for pattern projection; callers must not mutate the stacks. */
    public List<IAEStack<?>> getInputsView() {
        return inputs;
    }

    public List<IAEStack<?>> getVirtualInputs() {
        return copyStacks(virtualInputs, "virtualInputs");
    }

    public int getVirtualInputCount() {
        return virtualInputs.size();
    }

    public List<IAEStack<?>> getOutputs() {
        return copyStacks(outputs, "outputs");
    }

    public int getOutputCount() {
        return outputs.size();
    }

    /** Internal read-only view for pattern projection; callers must not mutate the stacks. */
    public List<IAEStack<?>> getOutputsView() {
        return outputs;
    }

    public IAEStack<?> getFirstOutput() {
        return outputs.isEmpty() ? null
            : outputs.get(0)
                .copy();
    }

    public int getEuPerTick() {
        return euPerTick;
    }

    public int getDuration() {
        return duration;
    }

    public int getSpecialValue() {
        return specialValue;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Format", NBT_FORMAT);
        tag.setString("Key", key.value());
        tag.setString("Source", sourceId);
        tag.setTag("Inputs", writeStacks(inputs));
        tag.setTag("VirtualInputs", writeStacks(virtualInputs));
        tag.setTag("Outputs", writeStacks(outputs));
        tag.setInteger("EUt", euPerTick);
        tag.setInteger("Duration", duration);
        tag.setInteger("SpecialValue", specialValue);
        return tag;
    }

    public static RecipeSnapshot fromNBT(NBTTagCompound tag) {
        if (tag == null || tag.getInteger("Format") != NBT_FORMAT) {
            throw new IllegalArgumentException("Unsupported recipe snapshot format");
        }
        String source = tag.getString("Source");
        List<IAEStack<?>> inputs = readStacks(tag.getTagList("Inputs", 10));
        List<IAEStack<?>> virtualInputs = readStacks(tag.getTagList("VirtualInputs", 10));
        List<IAEStack<?>> outputs = readStacks(tag.getTagList("Outputs", 10));
        RecipeSnapshot snapshot = new RecipeSnapshot(
            source,
            inputs,
            virtualInputs,
            outputs,
            tag.getInteger("EUt"),
            tag.getInteger("Duration"),
            tag.getInteger("SpecialValue"));
        String storedKey = tag.getString("Key");
        if (!storedKey.isEmpty() && !snapshot.key.value()
            .equals(storedKey)) {
            throw new IllegalArgumentException("Recipe snapshot key does not match its contents");
        }
        return snapshot;
    }

    private String canonical() {
        StringBuilder canonical = new StringBuilder(512);
        append(canonical, RecipeKey.FORMAT);
        append(canonical, sourceId);
        canonical.append("|eu:")
            .append(euPerTick);
        canonical.append("|duration:")
            .append(duration);
        canonical.append("|special:")
            .append(specialValue);
        appendStacks(canonical, "in", inputs);
        appendStacks(canonical, "virtual", virtualInputs);
        appendStacks(canonical, "out", outputs);
        return canonical.toString();
    }

    private static void appendStacks(StringBuilder canonical, String name, List<IAEStack<?>> stacks) {
        canonical.append('|')
            .append(name)
            .append(':')
            .append(stacks.size());
        for (IAEStack<?> stack : stacks) {
            canonical.append('[')
                .append(stack.getStackSize())
                .append(':');
            NBTTagCompound tag = new NBTTagCompound();
            Platform.writeStackNBT(stack, tag, true);
            appendNbt(canonical, tag);
            canonical.append(']');
        }
    }

    private static void appendNbt(StringBuilder canonical, NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            List<String> keys = new ArrayList<>(compoundMap(compound).keySet());
            Collections.sort(keys);
            canonical.append('{');
            for (String key : keys) {
                append(canonical, key);
                canonical.append('=');
                appendNbt(canonical, compound.getTag(key));
            }
            canonical.append('}');
        } else {
            canonical.append(tag.toString());
        }
    }

    private static void append(StringBuilder canonical, String value) {
        canonical.append(value.length())
            .append(':')
            .append(value)
            .append(';');
    }

    private static NBTTagList writeStacks(List<IAEStack<?>> stacks) {
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> stack : stacks) {
            list.appendTag(Platform.writeStackNBT(stack, new NBTTagCompound(), true));
        }
        return list;
    }

    private static List<IAEStack<?>> readStacks(NBTTagList list) {
        if (list.tagCount() > MAX_STACKS) {
            throw new IllegalArgumentException("Recipe stack list exceeds safe limit");
        }
        List<IAEStack<?>> stacks = new ArrayList<>(list.tagCount());
        for (int index = 0; index < list.tagCount(); index++) {
            IAEStack<?> stack = Platform.readStackNBT(list.getCompoundTagAt(index), true);
            if (stack == null || stack.getStackSize() <= 0) {
                throw new IllegalArgumentException("Invalid stack in recipe snapshot");
            }
            stacks.add(stack.copy());
        }
        return Collections.unmodifiableList(new ArrayList<>(stacks));
    }

    private static List<IAEStack<?>> copyStacks(List<IAEStack<?>> stacks, String name) {
        return copyStacks(stacks, name, true);
    }

    private static List<IAEStack<?>> copyStacks(List<IAEStack<?>> stacks, String name, boolean defensiveCopy) {
        if (stacks == null || stacks.size() > MAX_STACKS) {
            throw new IllegalArgumentException("Invalid recipe " + name + " list");
        }
        List<IAEStack<?>> copies = new ArrayList<>(stacks.size());
        for (IAEStack<?> stack : stacks) {
            if (stack == null || stack.getStackSize() <= 0) {
                throw new IllegalArgumentException("Invalid stack in recipe " + name);
            }
            copies.add(defensiveCopy ? stack.copy() : stack);
        }
        return Collections.unmodifiableList(copies);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, NBTBase> compoundMap(NBTTagCompound compound) {
        try {
            return (Map<String, NBTBase>) NBT_COMPOUND_MAP.get(compound);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read NBT compound contents", exception);
        }
    }

    private static Field findCompoundMapField() {
        for (String name : new String[] { "tagMap", "field_74784_a" }) {
            try {
                Field field = NBTTagCompound.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                // Try the other mapping name used by the production runtime.
            }
        }
        throw new ExceptionInInitializerError("No NBT compound map field found");
    }
}
