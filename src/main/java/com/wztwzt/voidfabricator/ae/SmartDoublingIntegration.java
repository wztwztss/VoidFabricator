package com.wztwzt.voidfabricator.ae;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import com.wztwzt.ae2_qof.api.ISmartDoublingMedium;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Optional bridge to AE2-QoL's CPU-side smart doubling. This class is only
 * loaded reflectively when the ae2_qof mod is present, so the base mod has no
 * hard runtime dependency on AE2-QoL.
 */
public final class SmartDoublingIntegration {

    private SmartDoublingIntegration() {}

    public static ICraftingMedium create(TileVoidFabricator tile) {
        return new Bridge(tile);
    }

    private static final class Bridge implements ICraftingMedium, ISmartDoublingMedium {

        private final TileVoidFabricator tile;

        private Bridge(TileVoidFabricator tile) {
            this.tile = tile;
        }

        @Override
        public boolean pushPattern(ICraftingPatternDetails details, InventoryCrafting craftingInv) {
            return tile.pushPattern(details, craftingInv);
        }

        @Override
        public boolean isBusy() {
            return tile.isBusy();
        }

        @Override
        public boolean isSmartDoublingEnabled() {
            return tile.isSmartDoublingEnabled();
        }

        @Override
        public void setSmartDoubling(boolean enabled) {
            tile.setSmartDoubling(enabled);
        }

        @Override
        public int getMaxMultiplier(ICraftingPatternDetails details) {
            return tile.getMaxMultiplier(details);
        }

        @Override
        public ItemStack getCrafterIcon() {
            return tile.getCrafterIcon();
        }
    }
}
