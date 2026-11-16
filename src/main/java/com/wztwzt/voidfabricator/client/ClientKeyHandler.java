package com.wztwzt.voidfabricator.client;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.screen.ModularScreen;

import codechicken.nei.KeyManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;

/**
 * NEI-compatible A/R/P bridge for the virtual recipe grid.
 *
 * <p>
 * The keys are resolved through NEI's own {@link KeyManager} bindings first, then
 * through the historical literals A/R/P. This keeps the grid working when the
 * player changes NEI's key configuration while preserving the keys the mod has
 * always advertised.
 */
public final class ClientKeyHandler {

    private static long lastActionAt;
    private static int lastKey;

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        if (Keyboard.isRepeatEvent()) {
            return;
        }

        int key = Keyboard.getEventKey();
        int neiBookmarkKey = KeyManager.getKeyCode("bookmark.add");
        int neiRecipeKey = KeyManager.getKeyCode("recipe.recipe");
        int neiUsageKey = KeyManager.getKeyCode("recipe.usage");
        boolean bookmark = key == Keyboard.KEY_A || key == neiBookmarkKey;
        boolean recipe = key == Keyboard.KEY_R || key == neiRecipeKey;
        // NEI's default usage key is U. The mod has always documented P for the
        // pattern page, so accept both the configured binding and literal P.
        boolean usage = key == Keyboard.KEY_P || key == neiUsageKey;
        if (!bookmark && !recipe && !usage) {
            return;
        }

        // MUI opens a GuiScreenWrapper implementing IMuiScreen, not the
        // ModularScreen itself. ModularScreen.getCurrent() unwraps the wrapper
        // without forcing the missing optional animation/input interfaces into
        // this mod's compile classpath.
        ModularScreen modularScreen = ModularScreen.getCurrent();
        if (modularScreen == null) {
            return;
        }
        // A/R/P belong to the hovered cell. While the search field (or any text
        // widget) has focus, NEI never treats those keys as shortcuts.
        if (modularScreen.getContext()
            .isFocused()) {
            return;
        }
        if (!PatternNeiActions.hasHover()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (key == lastKey && now - lastActionAt < 350L) {
            return;
        }
        lastActionAt = now;
        lastKey = key;

        if (bookmark) {
            PatternNeiActions.bookmark();
        } else if (recipe) {
            PatternNeiActions.openRecipe();
        } else {
            PatternNeiActions.openPattern();
        }
    }
}
