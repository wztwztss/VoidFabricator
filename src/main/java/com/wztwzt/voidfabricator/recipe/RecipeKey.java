package com.wztwzt.voidfabricator.recipe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable, versioned identity for an immutable recipe snapshot. */
public final class RecipeKey {

    public static final String FORMAT = "vf1";
    private final String value;

    public RecipeKey(String value) {
        if (value == null || !value.startsWith(FORMAT + ":") || value.length() != FORMAT.length() + 1 + 64) {
            throw new IllegalArgumentException("Invalid Void Fabricator recipe key");
        }
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static RecipeKey fromCanonical(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return new RecipeKey(FORMAT + ":" + hex);
        } catch (NoSuchAlgorithmException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RecipeKey key && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
