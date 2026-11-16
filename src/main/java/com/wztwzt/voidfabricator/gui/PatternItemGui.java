package com.wztwzt.voidfabricator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.wztwzt.voidfabricator.item.ItemMachineRecipePoolPattern;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.PatternView;

/** Opens the configuration center from the pool pattern item itself. */
public final class PatternItemGui {

    private PatternItemGui() {}

    public static ModularPanel build(ItemMachineRecipePoolPattern item, GuiData data, PanelSyncManager syncManager,
        UISettings settings) {
        EntityPlayer player = data.getPlayer();
        if (!(data instanceof PlayerInventoryGuiData)) {
            return errorPanel("voidfabricator.pattern.item.invalid");
        }
        ItemStack held = ((PlayerInventoryGuiData) data).getUsedItemStack();
        if (held == null || !ItemMachineRecipePoolPattern.isPoolPattern(held)) {
            return errorPanel("voidfabricator.pattern.item.invalid");
        }
        if (held.stackSize != 1) {
            return errorPanel("voidfabricator.pattern.item.stack");
        }

        if (data.isClient()) {
            PatternView view = PatternPoolData.fromStack(held)
                .toView();
            PatternGuiSession session = new PatternGuiSession(view, null, null, false);
            syncManager.addCloseListener(p -> com.wztwzt.voidfabricator.client.ClientPatternHover.clearAll());
            return PatternItemPanel.build(session, syncManager, player);
        }

        final PlayerInventoryGuiData inventoryData = (PlayerInventoryGuiData) data;
        PatternPoolData pool = PatternPoolData.fromStack(held);
        // MUI may hand the GUI a copied ItemStack. Every configuration write is
        // mirrored onto the player's current hand slot immediately, and again
        // when the panel closes, so machine insert/extract cannot be lost.
        pool.setOnSaved(() -> mirrorToHand(inventoryData, pool));
        pool.ensureScans();
        PatternGuiSession session = new PatternGuiSession(pool.toView(), pool, null, false);
        syncManager.addCloseListener(p -> {
            pool.persistNow();
            p.inventory.markDirty();
            if (p instanceof EntityPlayerMP) {
                ((EntityPlayerMP) p).sendContainerToPlayer(p.inventoryContainer);
            }
        });
        return PatternItemPanel.build(session, syncManager, player);
    }

    private static void mirrorToHand(PlayerInventoryGuiData data, PatternPoolData pool) {
        ItemStack current = data.getUsedItemStack();
        if (current == null || !ItemMachineRecipePoolPattern.isPoolPattern(current)) {
            return;
        }
        NBTTagCompound source = pool.getStack()
            .getTagCompound();
        current.setTagCompound(source == null ? null : (NBTTagCompound) source.copy());
        data.setUsedItemStack(current);
    }

    private static ModularPanel errorPanel(String key) {
        return ModularPanel.defaultPanel("void_fabricator_pattern_error", 220, 70)
            .child(
                new TextWidget<>(com.cleanroommc.modularui.api.drawable.IKey.lang(key)).pos(14, 18)
                    .size(192, 30));
    }
}
