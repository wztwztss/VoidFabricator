package com.wztwzt.voidfabricator.gui;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.PatternView;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

/** Block entry point for the shared pool pattern editor. */
public final class PatternEditorGui {

    private PatternEditorGui() {}

    public static ModularPanel build(TileVoidFabricator tile, PosGuiData data, PanelSyncManager syncManager,
        UISettings uiSettings) {
        PatternGuiSession session;
        if (data.isClient()) {
            PatternView view = PatternPoolData.fromStack(tile.getStackInSlot(TileVoidFabricator.SLOT_PATTERN))
                .toView();
            session = new PatternGuiSession(view, null, tile, true);
        } else {
            PatternPoolData pool = tile.getPatternData();
            PatternView view = pool == null ? new PatternView() : pool.toView();
            session = new PatternGuiSession(view, pool, tile, true);
        }
        return PatternEditorPanel.build(session, syncManager, true, data.getPlayer());
    }
}
