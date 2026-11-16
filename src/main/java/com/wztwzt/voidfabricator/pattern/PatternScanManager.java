package com.wztwzt.voidfabricator.pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.wztwzt.voidfabricator.recipe.GTMachineScanner;
import com.wztwzt.voidfabricator.recipe.ScanResult;

/**
 * Server-thread cooperative recipe scanner.
 *
 * <p>
 * A GT assembler can expose more than 10,000 recipes. Converting all of them
 * synchronously while a pattern is being edited stalls the integrated server
 * for tens of seconds and makes the client sync handlers time out. This queue
 * consumes a small time slice on each server tick, keeping the GUI responsive.
 */
public final class PatternScanManager {

    private static final long TICK_BUDGET_NANOS = 3_000_000L;
    private static final int TICK_RECORD_BUDGET = 64;
    private static final List<Job> JOBS = new ArrayList<>();

    private PatternScanManager() {}

    public static void submit(RecipePoolSlot slot, GTMachineScanner.ScanSession session, int generation,
        ItemStack machine, boolean fillEmpty, Set<String> deleted, Map<String, Boolean> previousEnabled,
        Runnable onDone) {
        JOBS.add(
            new Job(
                slot,
                session,
                generation,
                machine == null ? null : machine.copy(),
                fillEmpty,
                new HashSet<>(deleted),
                previousEnabled,
                onDone));
    }

    public static void tick() {
        if (JOBS.isEmpty()) {
            return;
        }
        // One job per tick keeps the worst-case main-thread cost below the
        // per-scan budget even when several machine patterns are configured.
        Job job = JOBS.get(0);
        try {
            if (!job.session.step(TICK_BUDGET_NANOS, TICK_RECORD_BUDGET)) {
                JOBS.remove(0);
                JOBS.add(job);
                return;
            }
            ScanResult result = job.session.finishAndCache();
            JOBS.remove(0);
            if (job.slot
                .completeScan(job.generation, job.machine, result, job.fillEmpty, job.deleted, job.previousEnabled)) {
                if (job.onDone != null) {
                    job.onDone.run();
                }
            }
        } catch (RuntimeException exception) {
            JOBS.remove(0);
            job.slot.failScan(job.generation, "scan_error");
            if (job.onDone != null) {
                job.onDone.run();
            }
        }
    }

    private static final class Job {

        private final RecipePoolSlot slot;
        private final GTMachineScanner.ScanSession session;
        private final int generation;
        private final ItemStack machine;
        private final boolean fillEmpty;
        private final Set<String> deleted;
        private final Map<String, Boolean> previousEnabled;
        private final Runnable onDone;

        private Job(RecipePoolSlot slot, GTMachineScanner.ScanSession session, int generation, ItemStack machine,
            boolean fillEmpty, Set<String> deleted, Map<String, Boolean> previousEnabled, Runnable onDone) {
            this.slot = slot;
            this.session = session;
            this.generation = generation;
            this.machine = machine;
            this.fillEmpty = fillEmpty;
            this.deleted = deleted;
            this.previousEnabled = previousEnabled;
            this.onDone = onDone;
        }
    }
}
