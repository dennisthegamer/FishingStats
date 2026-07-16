package de.dennisthegamer.fishingstats.hud.position;

import java.util.Locale;

/**
 * Ankerpunkt, an dem das HUD relativ zum Bildschirm klebt. Reine Integer-Mathematik,
 * keine Minecraft-Abhängigkeit -> headless testbar.
 */
public enum HudAnchor {
    TOP_LEFT(HAlign.LEFT, VAlign.TOP),
    TOP_CENTER(HAlign.CENTER, VAlign.TOP),
    TOP_RIGHT(HAlign.RIGHT, VAlign.TOP),
    MIDDLE_LEFT(HAlign.LEFT, VAlign.MIDDLE),
    MIDDLE_CENTER(HAlign.CENTER, VAlign.MIDDLE),
    MIDDLE_RIGHT(HAlign.RIGHT, VAlign.MIDDLE),
    BOTTOM_LEFT(HAlign.LEFT, VAlign.BOTTOM),
    BOTTOM_CENTER(HAlign.CENTER, VAlign.BOTTOM),
    BOTTOM_RIGHT(HAlign.RIGHT, VAlign.BOTTOM);

    private enum HAlign { LEFT, CENTER, RIGHT }
    private enum VAlign { TOP, MIDDLE, BOTTOM }

    private final HAlign h;
    private final VAlign v;

    HudAnchor(HAlign h, VAlign v) {
        this.h = h;
        this.v = v;
    }

    /** X der linken Box-Kante für diesen Anker (ungeklemmt). */
    public int baseX(int screenW, int boxW) {
        return switch (h) {
            case LEFT -> 0;
            case CENTER -> (screenW - boxW) / 2;
            case RIGHT -> screenW - boxW;
        };
    }

    /** Y der oberen Box-Kante für diesen Anker (ungeklemmt). */
    public int baseY(int screenH, int boxH) {
        return switch (v) {
            case TOP -> 0;
            case MIDDLE -> (screenH - boxH) / 2;
            case BOTTOM -> screenH - boxH;
        };
    }

    /** Anker, in dessen 3x3-Region die Box-Mitte (cx,cy) fällt. */
    public static HudAnchor fromCenter(int cx, int cy, int screenW, int screenH) {
        HAlign h = cx < screenW / 3 ? HAlign.LEFT : (cx < 2 * screenW / 3 ? HAlign.CENTER : HAlign.RIGHT);
        VAlign v = cy < screenH / 3 ? VAlign.TOP : (cy < 2 * screenH / 3 ? VAlign.MIDDLE : VAlign.BOTTOM);
        return of(h, v);
    }

    public String translationKey() {
        return "fishingstats.hud.anchor." + name().toLowerCase(Locale.ROOT);
    }

    private static HudAnchor of(HAlign h, VAlign v) {
        for (HudAnchor a : values()) {
            if (a.h == h && a.v == v) return a;
        }
        return TOP_LEFT;
    }
}
