package com.wztwzt.voidfabricator.ae;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.util.Platform;

/** Bounded, persistent hand-off buffer for accepted AE crafts. */
public final class OutputBuffer {

    public static final int MAX_BATCHES = 256;
    private static final int MAX_STACKS = 512;

    private final List<IAEStack<?>> outputs = new ArrayList<>();
    private long pendingBatches;

    public boolean canAccept(List<IAEStack<?>> newOutputs) {
        return canAccept(newOutputs, 1);
    }

    public boolean canAccept(List<IAEStack<?>> newOutputs, int batches) {
        if (batches <= 0 || newOutputs == null || newOutputs.isEmpty()) {
            return false;
        }
        if (pendingBatches + batches > MAX_BATCHES) {
            return false;
        }
        int newTypes = 0;
        for (IAEStack<?> output : newOutputs) {
            boolean known = false;
            for (IAEStack<?> existing : outputs) {
                if (existing.isSameType(output)) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                for (int index = 0; index < newOutputs.size(); index++) {
                    IAEStack<?> earlier = newOutputs.get(index);
                    if (earlier == output) {
                        break;
                    }
                    if (earlier.isSameType(output)) {
                        known = true;
                        break;
                    }
                }
            }
            if (!known) {
                newTypes++;
            }
        }
        return outputs.size() + newTypes <= MAX_STACKS;
    }

    public void add(List<IAEStack<?>> newOutputs) {
        add(newOutputs, 1);
    }

    public void add(List<IAEStack<?>> newOutputs, int batches) {
        if (!canAccept(newOutputs, batches)) {
            throw new IllegalStateException("Output buffer capacity was not reserved before accepting a craft");
        }
        for (IAEStack<?> output : newOutputs) {
            boolean merged = false;
            for (IAEStack<?> existing : outputs) {
                if (existing.isSameType(output)) {
                    existing.incStackSize(output.getStackSize() * batches);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                IAEStack<?> copy = output.copy();
                copy.setStackSize(output.getStackSize() * batches);
                outputs.add(copy);
            }
        }
        pendingBatches += batches;
    }

    public void flush(IStorageGrid storage, IActionHost host) {
        if (storage == null || outputs.isEmpty()) {
            return;
        }
        MachineSource source = new MachineSource(host);
        Iterator<IAEStack<?>> iterator = outputs.iterator();
        while (iterator.hasNext()) {
            IAEStack<?> pending = iterator.next();
            IAEStack<?> leftover = inject(storage, pending, source);
            if (leftover == null || leftover.getStackSize() <= 0) {
                iterator.remove();
            } else {
                pending.setStackSize(leftover.getStackSize());
            }
        }
        if (outputs.isEmpty()) {
            pendingBatches = 0;
        }
    }

    private static IAEStack<?> inject(IStorageGrid storage, IAEStack<?> pending, MachineSource source) {
        if (pending instanceof IAEItemStack item) {
            return storage.getItemInventory()
                .injectItems(item.copy(), Actionable.MODULATE, source);
        }
        if (pending instanceof IAEFluidStack fluid) {
            return storage.getFluidInventory()
                .injectItems(fluid.copy(), Actionable.MODULATE, source);
        }
        throw new IllegalArgumentException("Unsupported AE stack type in output buffer: " + pending.getClass());
    }

    public boolean isEmpty() {
        return outputs.isEmpty();
    }

    public boolean isSaturated() {
        // Keep one stack slot spare so a one-type craft can always be accepted.
        return pendingBatches >= MAX_BATCHES || outputs.size() + 1 >= MAX_STACKS;
    }

    public long getPendingBatches() {
        return pendingBatches;
    }

    public long remainingBatches() {
        return Math.max(0, MAX_BATCHES - pendingBatches);
    }

    public int remainingStackTypes() {
        return Math.max(0, MAX_STACKS - outputs.size());
    }

    /**
     * Conservative batch capacity for a craft that may introduce this many new
     * output stack types. Existing types can merge, so the real capacity may be
     * higher; this method deliberately under-estimates.
     */
    public int remainingBatchesFor(int newTypes) {
        int types = Math.max(1, newTypes);
        int free = remainingStackTypes();
        if (free <= 0) {
            return 0;
        }
        return (int) Math.min(remainingBatches(), free / types);
    }

    public int getPendingStackTypes() {
        return outputs.size();
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("PendingBatches", pendingBatches);
        NBTTagList list = new NBTTagList();
        for (IAEStack<?> output : outputs) {
            list.appendTag(Platform.writeStackNBT(output, new NBTTagCompound(), true));
        }
        tag.setTag("Outputs", list);
        return tag;
    }

    public void readNBT(NBTTagCompound tag) {
        outputs.clear();
        pendingBatches = 0;
        if (tag == null) {
            return;
        }
        NBTTagList list = tag.getTagList("Outputs", 10);
        if (list.tagCount() > MAX_STACKS) {
            throw new IllegalStateException("Saved output buffer exceeds safe stack limit");
        }
        for (int index = 0; index < list.tagCount(); index++) {
            IAEStack<?> stack = Platform.readStackNBT(list.getCompoundTagAt(index), true);
            if (stack == null || stack.getStackSize() <= 0) {
                throw new IllegalStateException("Saved output buffer contains an invalid stack");
            }
            outputs.add(stack.copy());
        }
        long savedBatches = tag.getLong("PendingBatches");
        if (savedBatches < 0 || savedBatches > MAX_BATCHES) {
            throw new IllegalStateException("Saved output buffer has an invalid batch count");
        }
        pendingBatches = savedBatches;
        if (outputs.isEmpty() && pendingBatches != 0) {
            throw new IllegalStateException("Saved output buffer has batches but no outputs");
        }
    }
}
