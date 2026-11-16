package com.wztwzt.voidfabricator.client;

import com.wztwzt.voidfabricator.gui.PatternGuiSession;
import com.wztwzt.voidfabricator.pattern.PatternEntry;

/** Client-side hover bridge between the pattern panel and NEI R/P key handling. */
public final class ClientPatternHover {

    private static PatternGuiSession session;
    private static int localIndex = -1;

    private ClientPatternHover() {}

    public static void set(PatternGuiSession source, int index) {
        session = source;
        localIndex = index;
    }

    public static void clear(PatternGuiSession source, int index) {
        if (session == source && localIndex == index) {
            session = null;
            localIndex = -1;
        }
    }

    public static PatternGuiSession session() {
        return session;
    }

    public static int localIndex() {
        return localIndex;
    }

    public static PatternEntry entry() {
        PatternGuiSession current = session();
        if (current == null || localIndex < 0) {
            return null;
        }
        return current.entryAt(localIndex);
    }

    public static void clearAll() {
        session = null;
        localIndex = -1;
    }
}
