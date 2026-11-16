package com.wztwzt.voidfabricator.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.InteractionSyncHandler;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.wztwzt.voidfabricator.client.ClientPatternHover;
import com.wztwzt.voidfabricator.pattern.PatternEntry;
import com.wztwzt.voidfabricator.pattern.PatternGroupView;
import com.wztwzt.voidfabricator.pattern.PatternPoolData;

import gregtech.api.enums.GTValues;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.modularui2.GTGuiThemes;
import gregtech.api.modularui2.GTWidgetThemes;

/** One physical pattern, laid out like TenkeiTech's 325x309 template input bus. */
public final class PatternItemPanel {

    public static final int PANEL_WIDTH = 325;
    public static final int PANEL_HEIGHT = 309;
    private static final int CONTENT_X = 7;
    private static final int CONTENT_Y = 6;
    private static final int CONTENT_WIDTH = 311;
    private static final int CONTENT_HEIGHT = 217;
    private static final int VISIBLE_ROWS = PatternPoolData.VISIBLE_ROWS;
    private static final int COLUMNS = PatternPoolData.GRID_COLUMNS;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_Y = 38;
    private static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT;
    private static final int TIER_WIDTH = 40;
    private static final int CELL_START_X = 42;
    private static final int CELL_STEP = 20;
    private static final int CELL_SIZE = 20;
    private static final int SCROLL_X = 302;
    private static final int SEARCH_X = 204;
    private static final int RESTORE_X = 122;
    private static final int ENABLE_ALL_X = 144;
    private static final int DISABLE_ALL_X = 216;
    private static final int GT_LOGO_X = 288;
    private static final int BOTTOM_Y = LIST_Y + LIST_HEIGHT + 5;

    private PatternItemPanel() {}

    public static ModularPanel build(PatternGuiSession session, PanelSyncManager syncManager, EntityPlayer player) {
        session.register(syncManager, "voidfabricator_item_pattern");
        DoubleSyncValue scrollPosition = new DoubleSyncValue(session::scrollPosition, session::setScrollPosition)
            .allowC2S();
        InteractionSyncHandler listScroll = createListScrollHandler(session);

        ModularPanel panel = ModularPanel.defaultPanel("void_fabricator_pattern_browser", PANEL_WIDTH, PANEL_HEIGHT)
            .themeOverride(GTGuiThemes.STANDARD.getId())
            // GT panels reserve 6 px below the 76 px player inventory block.
            .bindPlayerInventory(6);
        panel.child(createPanelTitle());

        ParentWidget<?> content = new ParentWidget<>().pos(CONTENT_X, CONTENT_Y)
            .size(CONTENT_WIDTH, CONTENT_HEIGHT);
        panel.child(content);

        content.child(
            new ItemSlot().slot(new PatternMachineHandler(session), 0)
                .pos(0, 7)
                .size(18, 18)
                .tooltipDynamic(tooltip -> {
                    tooltip.addLine(IKey.lang("voidfabricator.pattern.machine.tooltip"));
                    tooltip.addLine(IKey.lang("voidfabricator.pattern.machine.single"));
                }))
            .child(
                new TextWidget<>(IKey.dynamic(() -> machineLine(session))).textAlign(Alignment.CenterLeft)
                    .pos(22, 7)
                    .size(172, 18)
                    .maxWidth(172))
            .child(
                new TextWidget<>(IKey.dynamic(() -> voltageLine(session))).textAlign(Alignment.CenterRight)
                    .pos(198, 7)
                    .size(CONTENT_WIDTH - 198, 18)
                    .maxWidth(CONTENT_WIDTH - 198))
            .child(
                new TextWidget<>(IKey.dynamic(() -> statusLine(session))).textAlign(Alignment.CenterLeft)
                    .pos(0, 27)
                    .size(56, 9)
                    .scale(0.55f)
                    .maxWidth(56))
            .child(
                compactButton(() -> filterModeLine(session), session::toggleFilterMode, 0.5f).pos(58, 26)
                    .size(38, 11)
                    .tooltipDynamic(
                        tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.browser.mode.tooltip"))))
            .child(
                compactButton(
                    () -> StatCollector.translateToLocal("voidfabricator.pattern.filter_same"),
                    session::toggleGroupSameOutput,
                    0.5f).pos(98, 26)
                        .size(44, 11)
                        .tooltipDynamic(
                            tooltip -> tooltip
                                .addLine(IKey.lang("voidfabricator.pattern.browser.filter_same.tooltip"))))
            .child(
                compactButton(
                    () -> StatCollector.translateToLocal("voidfabricator.pattern.browser.delete_short"),
                    session::deleteSelected,
                    0.5f).pos(144, 26)
                        .size(17, 11)
                        .tooltipDynamic(
                            tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.delete_selected.tooltip"))))
            .child(
                new ButtonWidget<>().overlay(
                    IKey.dynamic(() -> doublingLine(session))
                        .scale(0.45f))
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                        if (delta.isClient()) {
                            return;
                        }
                        if (delta.mouseButton == 0) {
                            session.toggleSmartDoubling();
                        } else if (delta.mouseButton == 1) {
                            session.cycleMaxRounds();
                        }
                    }))
                    .pos(163, 26)
                    .size(38, 11)
                    .tooltipDynamic(tooltip -> {
                        tooltip.addLine(IKey.lang("voidfabricator.pattern.doubling.tooltip"));
                        tooltip.addLine(IKey.lang("voidfabricator.pattern.browser.rounds.tooltip"));
                        tooltip.addLine(IKey.lang("voidfabricator.pattern.browser.rounds", session.getMaxRounds()));
                    }))
            .child(
                new TextFieldWidget().value(session.queryValue())
                    .autoUpdateOnChange(true)
                    .setMaxLength(64)
                    .hintText(StatCollector.translateToLocal("voidfabricator.pattern.search.hint"))
                    .hintColor(0xFFB0B0B0)
                    .setTextColor(0xFFFFFFFF)
                    .background(GTGuiTextures.BACKGROUND_TEXT_FIELD)
                    .pos(SEARCH_X, 26)
                    .size(CONTENT_WIDTH - SEARCH_X, 11));

        content.child(
            createListScrollArea(listScroll).pos(0, LIST_Y)
                .size(CONTENT_WIDTH, LIST_HEIGHT));

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            ParentWidget<?> tierRow = new ParentWidget<>()
                .background(new DynamicDrawable(() -> rowBackground(session, rowIndex)))
                .pos(0, LIST_Y + row * ROW_HEIGHT)
                .size(SCROLL_X - 2, ROW_HEIGHT - 1);
            tierRow.child(
                new ButtonWidget<>()
                    .background(
                        new DynamicDrawable(
                            () -> isTierButtonVisible(session, rowIndex) ? GuiTextures.MC_BUTTON : IDrawable.EMPTY))
                    .overlay(IKey.dynamic(() -> tierLabel(session, rowIndex)))
                    .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                        if (!delta.isClient() && delta.mouseButton == 0) {
                            int tier = rowTier(session, rowIndex);
                            if (tier >= 0) {
                                session.toggleTier(tier);
                            }
                        }
                    }))
                    .setEnabledIf(widget -> isTierButtonVisible(session, rowIndex))
                    .tooltipDynamic(
                        tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.browser.tier.tooltip")))
                    .pos(0, 1)
                    .size(TIER_WIDTH, CELL_SIZE));
            tierRow.child(
                new TextWidget<>(IKey.dynamic(() -> groupLabel(session, rowIndex))).textAlign(Alignment.Center)
                    .scale(0.62f)
                    .pos(0, 1)
                    .size(TIER_WIDTH, CELL_SIZE)
                    .setEnabledIf(widget -> session.isGroupSameOutput()));
            for (int column = 0; column < COLUMNS; column++) {
                tierRow.child(
                    recipeCell(session, row * COLUMNS + column).pos(CELL_START_X + column * CELL_STEP, 1)
                        .size(CELL_SIZE, CELL_SIZE));
            }
            content.child(tierRow);
        }

        content.child(
            new TextWidget<>(IKey.lang("voidfabricator.pattern.browser.search_empty")).textAlign(Alignment.Center)
                .setEnabledIf(
                    widget -> session.getView()
                        .getTotalEntries() > 0 && session.rowCount() == 0)
                .pos(CELL_START_X, LIST_Y + LIST_HEIGHT / 2 - 5)
                .size(SCROLL_X - CELL_START_X, 10));

        content.child(
            createScrollSlider(listScroll).value(scrollPosition)
                .bounds(0, 1)
                .setAxis(GuiAxis.Y)
                .sliderSize(5, 14)
                .background(GuiTextures.DISPLAY_SMALL)
                .setEnabledIf(widget -> session.rowCount() > VISIBLE_ROWS)
                .pos(SCROLL_X, LIST_Y)
                .size(7, LIST_HEIGHT));

        content
            .child(
                new TextWidget<>(
                    IKey.lang(
                        "voidfabricator.pattern.browser.count",
                        () -> new Object[] { session.getView()
                            .getEnabledCount(),
                            session.getView()
                                .getTotalEntries() })).pos(0, BOTTOM_Y + 5)
                                    .size(118, 9)
                                    .scale(0.55f)
                                    .maxWidth(118))
            .child(
                restoreButton(session).pos(RESTORE_X, BOTTOM_Y)
                    .size(18, 18))
            .child(
                setAllButton(session, true).pos(ENABLE_ALL_X, BOTTOM_Y)
                    .size(68, 18))
            .child(
                setAllButton(session, false).pos(DISABLE_ALL_X, BOTTOM_Y)
                    .size(68, 18))
            .child(
                new IDrawable.DrawableWidget(GTGuiTextures.PICTURE_GT_LOGO_STANDARD).pos(GT_LOGO_X, BOTTOM_Y)
                    .size(18, 18));

        return panel;
    }

    private static ParentWidget<?> createPanelTitle() {
        return new ParentWidget<>().size(86, 15)
            .bottomRel(1)
            .left(5)
            .widgetTheme(GTWidgetThemes.BACKGROUND_TITLE)
            .child(
                new TextWidget<>(IKey.lang("voidfabricator.pattern.browser.title")).pos(5, 3)
                    .size(76, 9)
                    .widgetTheme(GTWidgetThemes.TEXT_TITLE));
    }

    private static InteractionSyncHandler createListScrollHandler(PatternGuiSession session) {
        return new InteractionSyncHandler().setOnMouseScroll(mouseDelta -> {
            if (!mouseDelta.isClient()) {
                session.scroll(mouseDelta.mouseButton);
            }
        });
    }

    private static ButtonWidget<?> createListScrollArea(InteractionSyncHandler scrollHandler) {
        return new ButtonWidget<>().syncHandler(scrollHandler)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .background(
                new Rectangle().color(0x22101010)
                    .solid())
            .hoverBackground(IDrawable.EMPTY)
            .playClickSound(false);
    }

    private static SliderWidget createScrollSlider(InteractionSyncHandler scrollHandler) {
        return new SliderWidget() {

            @Override
            public boolean onMouseScroll(UpOrDown direction, int amount) {
                return scrollHandler.onMouseScroll((int) Math.copySign(amount, direction.modifier));
            }
        };
    }

    private static ButtonWidget<?> recipeCell(PatternGuiSession session, int localIndex) {
        ParentWidget<?> icon = new NoHoverParent().size(CELL_SIZE, CELL_SIZE);
        icon.child(new ItemDisplayWidget() {

            @Override
            public boolean canHover() {
                return false;
            }

            @Override
            public boolean canHoverThrough() {
                return true;
            }
        }.item(new EntryValue(session, localIndex))
            .displayAmount(false)
            .pos(1, 1)
            .size(CELL_SIZE - 2, CELL_SIZE - 2));
        icon.child(new IDrawable.DrawableWidget(new DynamicDrawable(() -> {
            FluidStack fluid = fluid(session, localIndex);
            return fluid == null ? IDrawable.EMPTY : new FluidDrawable(fluid);
        })) {

            @Override
            public boolean canHover() {
                return false;
            }

            @Override
            public boolean canHoverThrough() {
                return true;
            }
        }.pos(1, 1)
            .size(CELL_SIZE - 2, CELL_SIZE - 2));
        icon.child(
            new TextWidget<>(IKey.dynamic(() -> amountLine(session, localIndex))).pos(1, CELL_SIZE - 8)
                .size(CELL_SIZE - 2, 7)
                .scale(0.5f)
                .textAlign(Alignment.CenterRight)
                .color(0xFFFFFFFF));

        IDrawable hover = new DrawableStack(
            GuiTextures.SLOT_ITEM,
            new Rectangle().color(0x33FFFFFF)
                .solid());
        return new PatternCellButton(session, localIndex).disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .background(new DynamicDrawable(() -> cellBackground(session, localIndex)))
            .hoverBackground(new DynamicDrawable(() -> cellHover(session, localIndex, hover)))
            .child(icon)
            // WidgetTree draws children after overlay(), so a red cross placed in overlay()
            // was always covered by the 18x18 item icon. This child is appended after the
            // icon and therefore renders visibly above it.
            .child(new IDrawable.DrawableWidget(new DynamicDrawable(() -> cellOverlay(session, localIndex))) {

                @Override
                public boolean canHover() {
                    return false;
                }

                @Override
                public boolean canHoverThrough() {
                    return true;
                }
            }.disableThemeBackground(true)
                .disableHoverThemeBackground(true)
                .pos(0, 0)
                .size(CELL_SIZE, CELL_SIZE))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                int globalIndex = session.entryGlobalIndex(localIndex);
                if (globalIndex < 0) {
                    return;
                }
                // Selection is a server-side view write. Calling select() from the
                // client callback tries to send a C2S sync packet from a non-sync
                // thread and raises "Sync handler is unable to send packets to
                // server!" (the source of the intermittent double-GUI flicker).
                if (delta.isClient()) {
                    return;
                }
                session.select(globalIndex);
                if (delta.mouseButton == 0) {
                    if (session.isEntryFiltered(localIndex)) {
                        session.restoreEntry(globalIndex);
                    } else {
                        session.toggle(globalIndex);
                    }
                } else if (delta.mouseButton == 1) {
                    session.toggleSameOutputFilter(globalIndex);
                }
            })
                .setOnMouseScroll(delta -> {
                    if (!delta.isClient()) {
                        session.scroll(delta.mouseButton);
                    }
                }))
            .tooltipDynamic(tooltip -> appendRecipeTooltip(session, localIndex, tooltip));
    }

    private static IDrawable cellBackground(PatternGuiSession session, int localIndex) {
        return session.entryAt(localIndex) == null ? IDrawable.EMPTY : GuiTextures.SLOT_ITEM;
    }

    private static IDrawable cellHover(PatternGuiSession session, int localIndex, IDrawable hover) {
        return session.entryAt(localIndex) == null ? IDrawable.EMPTY : hover;
    }

    private static IDrawable cellOverlay(PatternGuiSession session, int localIndex) {
        PatternEntry entry = session.entryAt(localIndex);
        if (entry == null) {
            return IDrawable.EMPTY;
        }
        IDrawable base = IDrawable.EMPTY;
        if (!entry.isEnabled()) {
            base = new DrawableStack(disabledDim(), disabledBorder(), redCross());
        } else if (session.isEntryFiltered(localIndex)) {
            // Excluded entries must read as excluded, not merely darker: keep the
            // same red border and cross used by manually disabled entries.
            base = new DrawableStack(filteredDim(), disabledBorder(), redCross());
        }
        if (session.getSelectedIndex() == entry.getGlobalIndex()) {
            return new DrawableStack(
                base,
                new Rectangle().color(0x883879B5)
                    .hollow(1));
        }
        return base;
    }

    private static ButtonWidget<?> compactButton(java.util.function.Supplier<String> label, Runnable action,
        float scale) {
        return new ButtonWidget<>().overlay(
            IKey.dynamic(label)
                .scale(scale))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (!delta.isClient() && delta.mouseButton == 0) {
                    action.run();
                }
            }));
    }

    private static ButtonWidget<?> setAllButton(PatternGuiSession session, boolean enabled) {
        IKey label = IKey
            .lang(enabled ? "voidfabricator.pattern.browser.enable_all" : "voidfabricator.pattern.browser.disable_all");
        if (!enabled) {
            label = label.style(IKey.RED);
        }
        return new ButtonWidget<>().overlay(label)
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (!delta.isClient() && delta.mouseButton == 0) {
                    session.setAllEnabled(enabled);
                }
            }))
            .tooltipDynamic(
                tooltip -> tooltip.addLine(
                    IKey.lang(
                        enabled ? "voidfabricator.pattern.browser.enable_all.tooltip"
                            : "voidfabricator.pattern.browser.disable_all.tooltip")))
            .setEnabledIf(
                widget -> session.getView()
                    .getTotalEntries() > 0);
    }

    private static ButtonWidget<?> restoreButton(PatternGuiSession session) {
        return new ButtonWidget<>().overlay(
            IKey.lang("voidfabricator.pattern.browser.restore_short")
                .scale(0.55f))
            .syncHandler(new InteractionSyncHandler().setOnMousePressed(delta -> {
                if (!delta.isClient() && delta.mouseButton == 0) {
                    session.restoreDeleted();
                }
            }))
            .tooltipDynamic(
                tooltip -> tooltip.addLine(IKey.lang("voidfabricator.pattern.browser.restore_deleted.tooltip")))
            .setEnabledIf(
                widget -> session.getView()
                    .getDeletedCount() > 0);
    }

    private static IDrawable rowBackground(PatternGuiSession session, int row) {
        int tier = rowTier(session, row);
        if (tier < 0) {
            return IDrawable.EMPTY;
        }
        if (tier == session.getView()
            .getMaxTier()) {
            return new Rectangle().color(0x553879B5)
                .solid();
        }
        return new Rectangle().color(row % 2 == 0 ? 0x18101010 : 0x28101010)
            .solid();
    }

    private static IDrawable disabledDim() {
        return new Rectangle().color(0x99221616)
            .solid();
    }

    private static IDrawable filteredDim() {
        return new Rectangle().color(0x80101010)
            .solid();
    }

    private static IDrawable disabledBorder() {
        return new Rectangle().color(0xFF8B1E1E)
            .hollow(1);
    }

    private static IDrawable redCross() {
        return new RedCross();
    }

    private static final class RedCross implements IDrawable {

        @Override
        public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme theme) {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(2.2f);
            GL11.glColor4f(1.0f, 0.16f, 0.16f, 1.0f);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(x + 3, y + 3);
            GL11.glVertex2f(x + width - 3, y + height - 3);
            GL11.glVertex2f(x + width - 3, y + 3);
            GL11.glVertex2f(x + 3, y + height - 3);
            GL11.glEnd();
            GL11.glLineWidth(1.0f);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    private static FluidStack fluid(PatternGuiSession session, int localIndex) {
        PatternEntry entry = session.entryAt(localIndex);
        return entry == null ? null : entry.getDisplayFluid();
    }

    private static String amountLine(PatternGuiSession session, int localIndex) {
        FluidStack fluid = fluid(session, localIndex);
        if (fluid != null) {
            return Integer.toString(fluid.amount);
        }
        PatternEntry entry = session.entryAt(localIndex);
        ItemStack display = entry == null ? null : entry.getDisplay();
        return display == null ? "" : Integer.toString(display.stackSize);
    }

    private static String groupLabel(PatternGuiSession session, int row) {
        if (!session.isGroupSameOutput()) {
            return "";
        }
        PatternGroupView group = session.getView()
            .group(row);
        if (group == null) {
            return "";
        }
        int number = session.getView()
            .getGroupStartRow() + row
            + 1;
        String name = group.getName();
        if (name == null || name.isEmpty()) {
            name = Integer.toString(group.getTotalCount());
        }
        return EnumChatFormatting.DARK_GRAY + Integer.toString(number)
            + " "
            + EnumChatFormatting.DARK_AQUA
            + name
            + EnumChatFormatting.GRAY
            + " x"
            + group.getTotalCount();
    }

    private static int rowTier(PatternGuiSession session, int row) {
        PatternEntry first = session.entryAt(row * COLUMNS);
        return first == null ? -1 : first.getVoltageTier();
    }

    private static boolean isTierButtonVisible(PatternGuiSession session, int row) {
        if (session.isGroupSameOutput()) {
            return false;
        }
        PatternEntry first = session.entryAt(row * COLUMNS);
        if (first == null) {
            return false;
        }
        int tier = first.getVoltageTier();
        if (row == 0) {
            return session.getView()
                .isFirstRowNewTier();
        }
        for (int index = 0; index < row * COLUMNS; index++) {
            PatternEntry previous = session.entryAt(index);
            if (previous != null && previous.getVoltageTier() == tier) {
                return false;
            }
        }
        return true;
    }

    private static String tierLabel(PatternGuiSession session, int row) {
        if (session.isGroupSameOutput() || !isTierButtonVisible(session, row)) {
            return "";
        }
        PatternEntry first = session.entryAt(row * COLUMNS);
        int tier = first.getVoltageTier();
        return EnumChatFormatting.GREEN + tierName(tier) + EnumChatFormatting.RESET;
    }

    private static String tierName(int tier) {
        return tier >= 0 && tier < GTValues.VN.length ? GTValues.VN[tier] : "-";
    }

    private static String machineLine(PatternGuiSession session) {
        ItemStack machine = session.getMachine();
        return machine == null
            ? EnumChatFormatting.DARK_RED + StatCollector.translateToLocal("voidfabricator.pattern.machine.empty")
            : EnumChatFormatting.DARK_AQUA + StatCollector
                .translateToLocalFormatted("voidfabricator.pattern.machine_name", machine.getDisplayName());
    }

    private static String voltageLine(PatternGuiSession session) {
        int tier = session.getView()
            .getMaxTier();
        return EnumChatFormatting.GREEN
            + StatCollector.translateToLocalFormatted("voidfabricator.pattern.browser.voltage", tierName(tier));
    }

    private static String statusLine(PatternGuiSession session) {
        String status = session.getView()
            .getStatus();
        String key = "voidfabricator.pattern.browser.status.short." + status;
        if (!StatCollector.canTranslate(key)) {
            key = "voidfabricator.pattern.browser.status.short.unknown";
        }
        EnumChatFormatting color = "ready".equals(status) ? EnumChatFormatting.DARK_GREEN
            : ("scan_error".equals(status) ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GRAY);
        return color + StatCollector.translateToLocal(key);
    }

    private static String filterModeLine(PatternGuiSession session) {
        return StatCollector.translateToLocal(
            session.getFilterMode() == PatternPoolData.MODE_BLACKLIST ? "voidfabricator.pattern.blacklist"
                : "voidfabricator.pattern.whitelist");
    }

    private static String doublingLine(PatternGuiSession session) {
        return StatCollector.translateToLocal(
            session.isSmartDoubling() ? "voidfabricator.pattern.browser.doubling.on"
                : "voidfabricator.pattern.browser.doubling.off");
    }

    private static void appendRecipeTooltip(PatternGuiSession session, int localIndex, RichTooltip tooltip) {
        PatternEntry entry = session.entryAt(localIndex);
        if (entry == null) {
            return;
        }
        FluidStack fluid = entry.getDisplayFluid();
        if (fluid != null) {
            tooltip.addLine(
                IKey.lang("voidfabricator.pattern.recipe.fluid_amount", fluid.getLocalizedName(), fluid.amount));
            return;
        }
        ItemStack display = entry.getDisplay();
        tooltip.addLine(
            IKey.lang(
                "voidfabricator.pattern.recipe.name",
                display == null ? entry.getFilterName() : display.getDisplayName()));
        if (display != null) {
            tooltip.addLine(IKey.lang("voidfabricator.pattern.recipe.output_amount", display.stackSize));
        }
    }

    private static final class NoHoverParent extends ParentWidget<NoHoverParent> {

        @Override
        public boolean canHover() {
            return false;
        }

        /**
         * The icon subtree must not swallow the cell button's hover: NEI A/R/P
         * (and this cell's tooltip) depend on {@link PatternCellButton} receiving
         * onMouseStartHover().
         */
        @Override
        public boolean canHoverThrough() {
            return true;
        }
    }

    private static final class PatternCellButton extends ButtonWidget<PatternCellButton> {

        private final PatternGuiSession session;
        private final int localIndex;

        private PatternCellButton(PatternGuiSession session, int localIndex) {
            this.session = session;
            this.localIndex = localIndex;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            // MUI normally calls onMouseStartHover/onMouseEndHover, but keep the
            // bridge in sync with the actual hover state as a fallback. Some
            // widget stacks with canHoverThrough children do not deliver the
            // start callback reliably on every MUI build.
            if (isHovering()) {
                ClientPatternHover.set(session, localIndex);
            } else {
                ClientPatternHover.clear(session, localIndex);
            }
        }

        @Override
        public void onMouseStartHover() {
            super.onMouseStartHover();
            session.setHovered(localIndex);
            ClientPatternHover.set(session, localIndex);
        }

        @Override
        public void onMouseEndHover() {
            super.onMouseEndHover();
            session.clearHovered(localIndex);
            ClientPatternHover.clear(session, localIndex);
        }
    }

    private static final class EntryValue implements IValue<ItemStack> {

        private final PatternGuiSession session;
        private final int index;

        private EntryValue(PatternGuiSession session, int index) {
            this.session = session;
            this.index = index;
        }

        @Override
        public ItemStack getValue() {
            PatternEntry entry = session.entryAt(index);
            return entry == null || entry.getDisplayFluid() != null ? null : entry.getDisplay();
        }

        @Override
        public void setValue(ItemStack value) {
            // Display-only.
        }

        @Override
        public Class<ItemStack> getValueType() {
            return ItemStack.class;
        }
    }
}
