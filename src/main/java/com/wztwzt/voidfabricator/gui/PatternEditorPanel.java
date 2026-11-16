package com.wztwzt.voidfabricator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.api.value.sync.IServerMouseAction;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.wztwzt.voidfabricator.pattern.PatternEntry;
import com.wztwzt.voidfabricator.pattern.PatternFilter;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;
import com.wztwzt.voidfabricator.pattern.PatternView;
import com.wztwzt.voidfabricator.tile.TileVoidFabricator;

/** Modern AE-styled configuration center shared by the block and the pool pattern item. */
public final class PatternEditorPanel {

    public static final int PANEL_WIDTH = 360;
    public static final int PANEL_HEIGHT = 336;
    private static final int CONTENT_Y = 78;
    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;
    private static final String MACHINE_GROUP = "pattern_machine";

    private PatternEditorPanel() {}

    public static ModularPanel build(PatternGuiSession session, PanelSyncManager syncManager, boolean blockMode,
        EntityPlayer player) {
        session.register(syncManager, blockMode ? "voidfabricator_block_pattern" : "voidfabricator_item_pattern");
        syncManager.registerSlotGroup(MACHINE_GROUP, 1);

        ModularPanel panel = ModularPanel
            .defaultPanel(
                blockMode ? "void_fabricator_block_pattern" : "void_fabricator_item_pattern",
                PANEL_WIDTH,
                PANEL_HEIGHT)
            .background(
                new Rectangle().color(0xF007131B)
                    .cornerRadius(3),
                new Rectangle().color(0xFF2A8FA3)
                    .hollow(1))
            .bindPlayerInventory();

        panel
            .child(
                new TextWidget<>(
                    IKey.lang(blockMode ? "voidfabricator.pattern.title.block" : "voidfabricator.pattern.title.item"))
                        .pos(12, 6)
                        .size(164, 10)
                        .scale(0.8f)
                        .color(0xFF7EE8F5))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.subtitle")).pos(12, 17)
                    .size(164, 10)
                    .scale(0.7f)
                    .color(0xFF7897A3))
            .child(
                new TextWidget<>(IKey.dynamic(() -> publishedText(session))).pos(180, 6)
                    .size(168, 10)
                    .scale(0.7f)
                    .textAlign(Alignment.TopRight)
                    .color(0xFF9EDDE6))
            .child(
                new TextWidget<>(IKey.dynamic(() -> statusText(session))).pos(180, 18)
                    .size(168, 10)
                    .textAlign(Alignment.TopRight)
                    .color(() -> statusColor(session)))
            .child(
                new Rectangle().color(0xFF2F9EB2)
                    .asWidget()
                    .pos(11, 30)
                    .size(338, 1))
            .child(machineRow(session))
            .child(
                new TextWidget<>(IKey.dynamic(() -> machineName(session))).pos(34, 38)
                    .size(112, 10)
                    .color(0xFFB4D6DE))
            .child(
                new TextWidget<>(
                    IKey.lang(
                        blockMode ? "voidfabricator.pattern.machine.hint.block"
                            : "voidfabricator.pattern.machine.hint.item")).pos(34, 48)
                                .size(110, 8)
                                .scale(0.6f)
                                .color(0xFF6F8D98))
            .child(actionButton("voidfabricator.pattern.eject_machine", delta -> {
                if (!delta.isClient()) {
                    session.ejectMachine(player);
                }
            }).pos(248, 36)
                .size(96, 16));

        if (blockMode) {
            panel.child(actionButton("voidfabricator.pattern.eject_pattern", delta -> {
                if (!delta.isClient() && session.getData() != null) {
                    TileVoidFabricator tile = session.getTile();
                    if (tile != null && tile.ejectPattern(player)) {
                        player.closeScreen();
                    }
                }
            }).pos(148, 36)
                .size(94, 16));
        }

        PagedWidget.Controller controller = new PagedWidget.Controller();
        PagedWidget<?> pages = new PagedWidget<>().pos(10, CONTENT_Y)
            .size(340, 164)
            .controller(controller)
            .initialPage(0)
            .addPage(patternPage(session))
            .addPage(filterPage(session))
            .addPage(doublingPage(session, syncManager));
        panel.child(pages);

        panel.child(tabButton("voidfabricator.pattern.tab.patterns", 12, 58, controller, 0))
            .child(tabButton("voidfabricator.pattern.tab.filters", 126, 58, controller, 1))
            .child(tabButton("voidfabricator.pattern.tab.doubling", 240, 58, controller, 2));
        return panel;
    }

    private static ParentWidget<?> machineRow(PatternGuiSession session) {
        PatternMachineHandler handler = new PatternMachineHandler(session);
        ModularSlot slot = new ModularSlot(handler, 0).slotGroup(MACHINE_GROUP)
            .filter(stack -> handler.isItemValid(0, stack))
            .canTake(true)
            .canPut(true);
        return new ParentWidget<>().pos(12, 36)
            .size(18, 18)
            .child(
                new ItemSlot().slot(slot)
                    .size(18, 18))
            .tooltipDynamic(tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.machine.tooltip")));
    }

    private static ParentWidget<?> patternPage(PatternGuiSession session) {
        ParentWidget<?> page = new ParentWidget<>().size(340, 164);
        for (int index = 0; index < PatternPoolData.MAX_ENTRIES; index++) {
            int column = index % 9;
            int row = index / 9;
            page.child(
                patternSlot(session, index, 6 + column * (SLOT_SIZE + SLOT_GAP), 4 + row * (SLOT_SIZE + SLOT_GAP)));
        }
        page.child(
            new TextWidget<>(IKey.dynamic(() -> selectionSummary(session))).pos(6, 72)
                .size(198, 34)
                .scale(0.8f)
                .color(0xFF8FB4BF))
            .child(actionButton("voidfabricator.pattern.rescan", delta -> {
                if (!delta.isClient()) {
                    session.rescan();
                }
            }).pos(6, 144)
                .size(96, 16))
            .child(actionButton("voidfabricator.pattern.restore_deleted", delta -> {
                if (!delta.isClient()) {
                    session.restoreDeleted();
                }
            }).pos(106, 144)
                .size(98, 16))
            .child(selectionDock(session));
        return page;
    }

    private static ParentWidget<?> selectionDock(PatternGuiSession session) {
        ParentWidget<?> dock = new ParentWidget<>().pos(210, 2)
            .size(126, 158)
            .background(
                new Rectangle().color(0xFF081820)
                    .cornerRadius(2),
                new Rectangle().color(0xFF1B4A56)
                    .hollow(1));
        dock.child(
            new ItemDisplayWidget().item(new EntryValue(session, () -> session.getSelectedIndex()))
                .displayAmount(false)
                .pos(5, 5)
                .size(18, 18))
            .child(
                new TextWidget<>(IKey.dynamic(() -> selectedName(session))).pos(27, 5)
                    .size(94, 10)
                    .color(0xFFC2E5EC))
            .child(
                new TextWidget<>(IKey.dynamic(() -> selectedSummary(session))).pos(5, 26)
                    .size(116, 46)
                    .scale(0.75f)
                    .color(0xFF8EAFB9))
            .child(actionButton("voidfabricator.pattern.toggle_selected", delta -> {
                if (!delta.isClient()) {
                    session.toggleSelected();
                }
            }).pos(5, 78)
                .size(56, 16))
            .child(actionButton("voidfabricator.pattern.delete_selected", delta -> {
                if (!delta.isClient()) {
                    session.deleteSelected();
                }
            }).pos(65, 78)
                .size(56, 16))
            .child(actionButton("voidfabricator.pattern.add_filter_selected", delta -> {
                if (!delta.isClient()) {
                    session.addFilterFromSelected();
                }
            }).pos(5, 98)
                .size(116, 16))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.selection.hint")).pos(5, 120)
                    .size(116, 32)
                    .scale(0.7f)
                    .color(0xFF6E8C96));
        return dock;
    }

    private static ButtonWidget<?> patternSlot(PatternGuiSession session, int index, int x, int y) {
        ButtonWidget<?> button = new ButtonWidget<>().pos(x, y)
            .size(SLOT_SIZE, SLOT_SIZE)
            .background(
                new Rectangle().color(0xFF0A1B24)
                    .cornerRadius(2),
                new Rectangle().color(0xFF245764)
                    .hollow(1))
            .hoverBackground(
                new Rectangle().color(0xFF123640)
                    .cornerRadius(2),
                new Rectangle().color(0xFF58D6E5)
                    .hollow(1))
            .child(
                new ItemDisplayWidget().item(new EntryValue(session, index))
                    .displayAmount(true)
                    .pos(1, 1)
                    .size(18, 18))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (delta.mouseButton == 1) {
                    if (!delta.isClient()) {
                        session.toggle(index);
                    }
                } else {
                    session.select(index);
                }
            }))
            .tooltipDynamic(tooltip -> appendSlotTooltip(session, index, tooltip));
        return button;
    }

    private static ParentWidget<?> filterPage(PatternGuiSession session) {
        ParentWidget<?> page = new ParentWidget<>().size(340, 164);
        page.child(
            actionButtonDynamic(
                () -> StatCollector.translateToLocal(
                    session.getFilterMode() == PatternPoolData.MODE_WHITELIST
                        ? "voidfabricator.pattern.filter.mode.white"
                        : "voidfabricator.pattern.filter.mode.black"),
                delta -> {
                    if (!delta.isClient()) {
                        session.toggleFilterMode();
                    }
                }).pos(6, 4)
                    .size(148, 16)
                    .tooltipDynamic(
                        tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.filter.mode.tooltip"))))
            .child(actionButton("voidfabricator.pattern.filter.clear", delta -> {
                if (!delta.isClient()) {
                    session.clearFilters();
                }
            }).pos(158, 4)
                .size(76, 16))
            .child(actionButton("voidfabricator.pattern.filter.apply", delta -> {
                if (!delta.isClient()) {
                    session.applyFilters();
                }
            }).pos(238, 4)
                .size(96, 16));
        for (int index = 0; index < PatternPoolData.MAX_FILTERS; index++) {
            int column = index % 3;
            int row = index / 3;
            page.child(filterEntry(session, index, 6 + column * 112, 22 + row * 13));
        }
        page.child(
            new TextWidget<>(IKey.dynamic(() -> filterHint(session))).pos(6, 150)
                .size(328, 10)
                .scale(0.7f)
                .color(0xFF75949E));
        return page;
    }

    private static ButtonWidget<?> filterEntry(PatternGuiSession session, int index, int x, int y) {
        return new ButtonWidget<>().pos(x, y)
            .size(108, 12)
            .overlay(
                IKey.dynamic(() -> filterLabel(session, index))
                    .scale(0.75f)
                    .color(0xFFB6D7DF))
            .background(
                new Rectangle().color(0xFF0B2029)
                    .cornerRadius(2),
                new Rectangle().color(0xFF204C57)
                    .hollow(1))
            .hoverBackground(
                new Rectangle().color(0xFF163943)
                    .cornerRadius(2),
                new Rectangle().color(0xFF56C9D9)
                    .hollow(1))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (!delta.isClient() && delta.mouseButton == 0) {
                    session.removeFilter(index);
                }
            }))
            .tooltipDynamic(tooltip -> {
                PatternFilter filter = session.filter(index);
                if (filter == null) {
                    tooltip.addLine(IKey.lang("voidfabricator.pattern.filter.empty.tooltip"));
                } else {
                    tooltip.addLine(IKey.lang("voidfabricator.pattern.filter.remove.tooltip"));
                }
            });
    }

    private static ParentWidget<?> doublingPage(PatternGuiSession session, PanelSyncManager syncManager) {
        ParentWidget<?> page = new ParentWidget<>().size(340, 164);
        IntSyncValue rounds = new IntSyncValue(session::getMaxRounds, session::setMaxRounds);
        syncManager.syncValue("voidfabricator_pattern_rounds", 0, rounds);
        page.child(
            actionButtonDynamic(
                () -> StatCollector.translateToLocal(
                    session.isSmartDoubling() ? "voidfabricator.pattern.doubling.on"
                        : "voidfabricator.pattern.doubling.off"),
                delta -> {
                    if (!delta.isClient()) {
                        session.toggleSmartDoubling();
                    }
                }).pos(6, 6)
                    .size(168, 18)
                    .tooltipDynamic(tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.doubling.tooltip"))))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.doubling.rounds")).pos(6, 34)
                    .size(110, 10)
                    .color(0xFFA9CAD3))
            .child(
                new TextFieldWidget().value(rounds)
                    .numbersInt(0, PatternPoolData.MAX_ROUNDS)
                    .pos(122, 31)
                    .size(44, 16))
            .child(
                new TextWidget<>(IKey.dynamic(() -> qolText(session))).pos(6, 54)
                    .size(328, 10)
                    .color(() -> session.isQolAvailable() ? 0xFF55DFA6 : 0xFFF0C06A))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.doubling.description")).pos(6, 72)
                    .size(328, 56)
                    .scale(0.75f)
                    .color(0xFF8EAFB9))
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.doubling.warning")).pos(6, 132)
                    .size(328, 24)
                    .scale(0.7f)
                    .color(0xFFB99A62));
        return page;
    }

    private static ButtonWidget<?> tabButton(String key, int x, int y, PagedWidget.Controller controller, int page) {
        return new ButtonWidget<>().pos(x, y)
            .size(108, 14)
            .overlay(
                IKey.lang(key)
                    .color(0xFFD4F1F6))
            .background(
                new Rectangle().color(0xFF102A33)
                    .cornerRadius(2),
                new Rectangle().color(0xFF245463)
                    .hollow(1))
            .hoverBackground(
                new Rectangle().color(0xFF1A414B)
                    .cornerRadius(2),
                new Rectangle().color(0xFF57D1E1)
                    .hollow(1))
            .onMousePressed(button -> {
                controller.setPage(page);
                return true;
            });
    }

    private static ButtonWidget<?> actionButton(String key, IServerMouseAction action) {
        return actionButtonDynamic(() -> StatCollector.translateToLocal(key), action)
            .tooltipDynamic(tooltip -> tooltip.addLine(IKey.lang(key + ".tooltip")));
    }

    private static ButtonWidget<?> actionButtonDynamic(java.util.function.Supplier<String> text,
        IServerMouseAction action) {
        return new ButtonWidget<>().overlay(
            IKey.dynamic(text)
                .color(0xFFD7F7FB))
            .background(
                new Rectangle().color(0xFF123039)
                    .cornerRadius(2),
                new Rectangle().color(0xFF2B7C88)
                    .hollow(1))
            .hoverBackground(
                new Rectangle().color(0xFF1B4A54)
                    .cornerRadius(2),
                new Rectangle().color(0xFF58D6E5)
                    .hollow(1))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (!delta.isClient() && delta.mouseButton == 0) {
                    action.onServerMouseAction(delta);
                }
            }));
    }

    private static void appendSlotTooltip(PatternGuiSession session, int index, RichTooltip tooltip) {
        tooltip.addLine(IKey.lang("voidfabricator.pattern.slot.title", index + 1));
        PatternEntry entry = session.getView()
            .entry(index);
        if (entry == null) {
            tooltip.addLine(IKey.lang("voidfabricator.pattern.slot.empty"));
            return;
        }
        if (entry.getDisplay() != null) {
            tooltip.addFromItem(entry.getDisplay());
        }
        tooltip.addLine(
            IKey.lang(
                entry.isEnabled() ? "voidfabricator.pattern.slot.enabled" : "voidfabricator.pattern.slot.disabled"));
        tooltip.addLine(
            IKey.lang(
                "voidfabricator.pattern.slot.stats",
                entry.getInputs(),
                entry.getVirtualInputs(),
                entry.getOutputs(),
                entry.getEuPerTick(),
                entry.getDuration()));
        tooltip.addLine(IKey.lang("voidfabricator.pattern.slot.help"));
    }

    private static String publishedText(PatternGuiSession session) {
        PatternView view = session.getView();
        return StatCollector.translateToLocalFormatted(
            "voidfabricator.pattern.published",
            view.getPublished(),
            view.getEntries()
                .size(),
            view.getMatchedCount());
    }

    private static String statusText(PatternGuiSession session) {
        String key = session.getView()
            .getStatus();
        if (key == null || key.isEmpty()) {
            key = "pattern_required";
        }
        return StatCollector.translateToLocal("voidfabricator.pattern.state." + key);
    }

    private static int statusColor(PatternGuiSession session) {
        String key = session.getView()
            .getStatus();
        if ("ready".equals(key)) {
            return 0xFF55DFA6;
        }
        if ("no_entries".equals(key)) {
            return 0xFFF0C06A;
        }
        if ("pattern_required".equals(key) || "no_machine".equals(key) || "machine_missing".equals(key)) {
            return 0xFF809AA2;
        }
        return 0xFFF08B78;
    }

    private static String machineName(PatternGuiSession session) {
        String name = session.getView()
            .getMachineName();
        if (name == null || name.isEmpty()) {
            return StatCollector.translateToLocal("voidfabricator.pattern.machine.empty");
        }
        return StatCollector.translateToLocalFormatted("voidfabricator.pattern.machine.name", name);
    }

    private static String selectionSummary(PatternGuiSession session) {
        PatternEntry entry = session.selectedEntry();
        if (entry == null) {
            return StatCollector.translateToLocal("voidfabricator.pattern.selection.empty");
        }
        return StatCollector.translateToLocalFormatted(
            "voidfabricator.pattern.selection.summary",
            entry.getInputs(),
            entry.getVirtualInputs(),
            entry.getOutputs(),
            entry.getEuPerTick(),
            entry.getDuration());
    }

    private static String selectedName(PatternGuiSession session) {
        PatternEntry entry = session.selectedEntry();
        if (entry == null) {
            return StatCollector.translateToLocal("voidfabricator.pattern.selection.none");
        }
        return entry.getFilterName() == null
            ? StatCollector.translateToLocal("voidfabricator.pattern.selection.unknown")
            : entry.getFilterName();
    }

    private static String selectedSummary(PatternGuiSession session) {
        PatternEntry entry = session.selectedEntry();
        if (entry == null) {
            return StatCollector.translateToLocal("voidfabricator.pattern.selection.hint.empty");
        }
        return StatCollector.translateToLocalFormatted(
            "voidfabricator.pattern.selection.detail",
            entry.isEnabled() ? StatCollector.translateToLocal("voidfabricator.pattern.slot.enabled")
                : StatCollector.translateToLocal("voidfabricator.pattern.slot.disabled"),
            entry.getSource() == null ? "" : entry.getSource());
    }

    private static String filterLabel(PatternGuiSession session, int index) {
        PatternFilter filter = session.filter(index);
        if (filter == null) {
            return StatCollector.translateToLocalFormatted("voidfabricator.pattern.filter.entry.empty", index + 1);
        }
        return StatCollector.translateToLocalFormatted(
            "voidfabricator.pattern.filter.entry",
            index + 1,
            filter.getName() == null ? filter.getKey() : filter.getName());
    }

    private static String filterHint(PatternGuiSession session) {
        if (session.getFilterMode() == PatternPoolData.MODE_WHITELIST && session.getView()
            .getFilters()
            .isEmpty()) {
            return StatCollector.translateToLocal("voidfabricator.pattern.filter.hint.whitelist_empty");
        }
        return StatCollector.translateToLocalFormatted(
            "voidfabricator.pattern.filter.hint",
            session.getFilterMode() == PatternPoolData.MODE_WHITELIST
                ? StatCollector.translateToLocal("voidfabricator.pattern.filter.mode.white")
                : StatCollector.translateToLocal("voidfabricator.pattern.filter.mode.black"),
            session.getView()
                .getFilters()
                .size(),
            session.getView()
                .getDeletedCount());
    }

    private static String qolText(PatternGuiSession session) {
        return StatCollector.translateToLocal(
            session.isQolAvailable() ? "voidfabricator.pattern.doubling.qol.present"
                : "voidfabricator.pattern.doubling.qol.absent");
    }

    private static final class EntryValue implements IValue<ItemStack> {

        private final PatternGuiSession session;
        private final int index;
        private final java.util.function.IntSupplier dynamicIndex;

        private EntryValue(PatternGuiSession session, int index) {
            this.session = session;
            this.index = index;
            this.dynamicIndex = null;
        }

        private EntryValue(PatternGuiSession session, java.util.function.IntSupplier dynamicIndex) {
            this.session = session;
            this.index = -1;
            this.dynamicIndex = dynamicIndex;
        }

        @Override
        public ItemStack getValue() {
            int slot = dynamicIndex == null ? index : dynamicIndex.getAsInt();
            PatternEntry entry = session.getView()
                .entry(slot);
            return entry == null ? null : entry.getDisplay();
        }

        @Override
        public void setValue(ItemStack value) {
            // Display-only value.
        }

        @Override
        public Class<ItemStack> getValueType() {
            return ItemStack.class;
        }
    }
}
