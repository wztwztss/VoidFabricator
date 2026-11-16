package com.wztwzt.voidfabricator.client;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;

import codechicken.nei.KeyManager;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * NEI-compatible A/R/P bridge for the virtual recipe grid.
 *
 * <p>
 * The original implementation listened to FML's {@code KeyInputEvent}. That
 * event is fired after the vanilla screen has consumed the key, and MUI screens
 * and NEI overlays can leave the event listener with no reliable current screen
 * on some GTNH/LWJGL3ify combinations. Polling the physical key state from the
 * client tick gives the same edge-triggered behaviour without depending on
 * {@code GuiScreen.handleKeyboardInput()} or on MUI's wrapper being the screen
 * that NEI sees. Keys are resolved through NEI's own bindings first, then through
 * the historical literals A/R/P, so rebinding NEI does not break the grid.
 */
public final class ClientKeyHandler {

    private boolean aDown;
    private boolean rDown;
    private boolean pDown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        boolean a = isDown(Keyboard.KEY_A, "bookmark.add");
        boolean r = isDown(Keyboard.KEY_R, "recipe.recipe");
        // NEI's default usage key is U. The mod has always documented P for the
        // pattern page, so accept both the configured binding and literal P.
        boolean p = isDown(Keyboard.KEY_P, "recipe.usage");

        boolean aPressed = a && !aDown;
        boolean rPressed = r && !rDown;
        boolean pPressed = p && !pDown;
        aDown = a;
        rDown = r;
        pDown = p;

        if (!aPressed && !rPressed && !pPressed) {
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
        IWidget focused = modularScreen.getContext()
            .getFocusedWidget()
            .getElement();
        if (focused instanceof BaseTextFieldWidget) {
            return;
        }
        if (!PatternNeiActions.hasHover()) {
            return;
        }

        // A is an NEI-native toggle, so pressing it twice intentionally removes
        // the bookmark again. Priority is A > R > P when several keys are down.
        if (aPressed) {
            PatternNeiActions.bookmark();
        } else if (rPressed) {
            PatternNeiActions.openRecipe();
        } else {
            PatternNeiActions.openPattern();
        }
    }

    private static boolean isDown(int literalKey, String neiBinding) {
        if (Keyboard.isKeyDown(literalKey)) {
            return true;
        }
        int configured = KeyManager.getKeyCode(neiBinding);
        return configured > 0 && Keyboard.isKeyDown(configured);
    }
}
