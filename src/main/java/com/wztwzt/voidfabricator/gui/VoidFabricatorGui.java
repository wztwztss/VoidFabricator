package com.wztwzt.voidfabricator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

/** AE-status header and 27 physical pattern slots; crafting remains in normal AE terminals. */
public final class VoidFabricatorGui {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 286;
    private static final String SLOT_GROUP = "void_fabricator_patterns";

    private VoidFabricatorGui() {}

    public static ModularPanel build(TileVoidFabricator tile, PosGuiData data, PanelSyncManager syncManager,
        UISettings uiSettings) {
        StringSyncValue state = new StringSyncValue(tile::getStatusKey);
        IntSyncValue published = new IntSyncValue(tile::getPublishedPatternCount);
        LongSyncValue pendingBatches = new LongSyncValue(tile::getPendingBatchCount);
        IntSyncValue pendingTypes = new IntSyncValue(tile::getPendingStackTypes);
        IntSyncValue skipped = new IntSyncValue(tile::getSkippedRecipeCount);
        BooleanSyncValue online = new BooleanSyncValue(tile::isAeOnline);

        syncManager.syncValue("voidfabricator_state", 0, state);
        syncManager.syncValue("voidfabricator_published", 0, published);
        syncManager.syncValue("voidfabricator_pending_batches", 0, pendingBatches);
        syncManager.syncValue("voidfabricator_pending_types", 0, pendingTypes);
        syncManager.syncValue("voidfabricator_skipped", 0, skipped);
        syncManager.syncValue("voidfabricator_online", 0, online);
        syncManager.registerSlotGroup(SLOT_GROUP, 9);

        ModularPanel panel = ModularPanel.defaultPanel("void_fabricator", PANEL_WIDTH, PANEL_HEIGHT)
            .background(
                new Rectangle().color(0xF00A141A)
                    .cornerRadius(3),
                new Rectangle().color(0xFF2B8290)
                    .hollow(1))
            .bindPlayerInventory();

        panel.child(
            new TextWidget<>(IKey.lang("voidfabricator.gui.title")).pos(9, 7)
                .color(0xFF79E6F2))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.gui.subtitle")).pos(9, 18)
                    .scale(0.8f)
                    .color(0xFF72909A))
            .child(
                new Rectangle().color(0xFF38CFDF)
                    .asWidget()
                    .pos(8, 28)
                    .size(204, 1))
            .child(statusPanel(state, published, pendingBatches, pendingTypes, skipped, online))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.gui.pattern_slots")).pos(9, 94)
                    .scale(0.85f)
                    .color(0xFFA8C4CB));

        for (int slot = 0; slot < TileVoidFabricator.PATTERN_SLOTS; slot++) {
            int x = 29 + (slot % 9) * 18;
            int y = 108 + (slot / 9) * 18;
            panel.child(
                new ItemSlot().slot(new ContractSlot(tile, slot).slotGroup(SLOT_GROUP))
                    .pos(x, y)
                    .size(18, 18));
        }

        panel.child(
            new TextWidget<>(IKey.lang("voidfabricator.gui.pattern_hint")).pos(9, 168)
                .maxWidth(202)
                .scale(0.8f)
                .color(0xFF718D96))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.gui.pattern_hint2")).pos(9, 181)
                    .maxWidth(202)
                    .scale(0.8f)
                    .color(0xFF718D96));
        return panel;
    }

    private static ParentWidget<?> statusPanel(StringSyncValue state, IntSyncValue published,
        LongSyncValue pendingBatches, IntSyncValue pendingTypes, IntSyncValue skipped, BooleanSyncValue online) {
        ParentWidget<?> status = new ParentWidget<>().pos(8, 33)
            .size(204, 56)
            .background(
                new Rectangle().color(0xFF071015)
                    .cornerRadius(2),
                new Rectangle().color(0xFF1D4752)
                    .hollow(1));

        TextWidget<?> publishedLine = new TextWidget<>(
            IKey.dynamic(
                () -> StatCollector
                    .translateToLocalFormatted("voidfabricator.gui.published", published.getIntValue())));
        publishedLine.pos(5, 4)
            .maxWidth(194)
            .color(0xFF9FDDE6);
        status.child(publishedLine);

        TextWidget<?> pendingLine = new TextWidget<>(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted(
                    "voidfabricator.gui.pending",
                    pendingBatches.getLongValue(),
                    pendingTypes.getIntValue())));
        pendingLine.pos(5, 16)
            .maxWidth(194)
            .color(0xFF9FDDE6);
        status.child(pendingLine);

        TextWidget<?> filteredLine = new TextWidget<>(
            IKey.dynamic(
                () -> StatCollector.translateToLocalFormatted("voidfabricator.gui.filtered", skipped.getIntValue())));
        filteredLine.pos(5, 28)
            .maxWidth(194)
            .color(0xFF809AA2);
        status.child(filteredLine);

        TextWidget<?> stateLine = new TextWidget<>(IKey.dynamic(() -> stateText(state)));
        stateLine.pos(5, 41)
            .maxWidth(128)
            .color(() -> stateColor(state));
        status.child(stateLine);

        TextWidget<?> networkLine = new TextWidget<>(
            IKey.dynamic(
                () -> StatCollector.translateToLocal(
                    online.getBoolValue() ? "voidfabricator.gui.network.online"
                        : "voidfabricator.gui.network.offline")));
        networkLine.pos(132, 41)
            .size(65, 10)
            .textAlign(Alignment.TopRight)
            .color(() -> online.getBoolValue() ? 0xFF55DFA6 : 0xFFF08B78);
        status.child(networkLine);
        return status;
    }

    private static String stateText(StringSyncValue state) {
        String key = state.getStringValue();
        if (key == null || key.isEmpty()) {
            key = "awaiting_ready";
        }
        return StatCollector.translateToLocal("voidfabricator.gui.state." + key);
    }

    private static int stateColor(StringSyncValue state) {
        String key = state.getStringValue();
        if ("ready".equals(key)) {
            return 0xFF55DFA6;
        }
        if ("empty".equals(key) || "awaiting_ready".equals(key)) {
            return 0xFF809AA2;
        }
        return 0xFFF0C06A;
    }

    private static final class ContractSlot extends ModularSlot {

        private final TileVoidFabricator tile;
        private final int slot;

        private ContractSlot(TileVoidFabricator tile, int slot) {
            super(tile, slot);
            this.tile = tile;
            this.slot = slot;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return super.canTakeStack(player) && tile.canExtractSlot(slot);
        }
    }
}
