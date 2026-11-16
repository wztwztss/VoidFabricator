package com.wztwzt.voidfabricator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;

/** Minimal NEI-compatible R/P bridge for the virtual recipe grid. */
public final class ClientKeyHandler {

    private static long lastActionAt;
    private static int lastKey;

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        int key = Keyboard.getEventKey();
        if (key != Keyboard.KEY_R && key != Keyboard.KEY_P) {
            return;
        }
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null || !screen.getClass()
            .getName()
            .startsWith("com.cleanroommc.modularui.screen.ModularScreen")) {
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
        if (key == Keyboard.KEY_R) {
            PatternNeiActions.openRecipe();
        } else {
            PatternNeiActions.openPattern();
        }
    }
}
