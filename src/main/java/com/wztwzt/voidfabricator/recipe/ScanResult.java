package com.wztwzt.voidfabricator.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ScanResult {

    private final String fingerprint;
    private final int recipeMapCount;
    private final List<RecipeSnapshot> recipes;
    private final Map<String, Integer> skipped;

    public ScanResult(String fingerprint, int recipeMapCount, List<RecipeSnapshot> recipes,
        Map<String, Integer> skipped) {
        this.fingerprint = fingerprint;
        this.recipeMapCount = recipeMapCount;
        this.recipes = Collections.unmodifiableList(new ArrayList<>(recipes));
        this.skipped = Collections.unmodifiableMap(new TreeMap<>(skipped));
    }

    public String fingerprint() {
        return fingerprint;
    }

    public int recipeMapCount() {
        return recipeMapCount;
    }

    public List<RecipeSnapshot> recipes() {
        return recipes;
    }

    public Map<String, Integer> skipped() {
        return skipped;
    }

    public int skippedTotal() {
        return skipped.values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();
    }
}
