package de.dennisthegamer.fishingstats.hud.position;

/**
 * Frei gewählte HUD-Position als Anker + Pixel-Offset. Auflösungs-/Scale-robust:
 * {@link #resolveX}/{@link #resolveY} klemmen so, dass die Box nie aus dem Bild ragt.
 * Reine Mathematik, keine Minecraft-Abhängigkeit.
 */
public record HudPlacement(HudAnchor anchor, int offsetX, int offsetY) {

    public static HudPlacement of(HudAnchor anchor) {
        return new HudPlacement(anchor, 0, 0);
    }

    /**
     * Erzeugt ein Placement, das (im ungeklemmten Fall) die Box genau an (absX,absY) legt,
     * indem der Offset relativ zur Anker-Basis berechnet wird.
     */
    public static HudPlacement fromAbsolute(HudAnchor anchor, int absX, int absY,
                                            int screenW, int screenH, int boxW, int boxH) {
        return new HudPlacement(anchor,
                absX - anchor.baseX(screenW, boxW),
                absY - anchor.baseY(screenH, boxH));
    }

    public int resolveX(int screenW, int boxW) {
        return clamp(anchor.baseX(screenW, boxW) + offsetX, 0, Math.max(0, screenW - boxW));
    }

    public int resolveY(int screenH, int boxH) {
        return clamp(anchor.baseY(screenH, boxH) + offsetY, 0, Math.max(0, screenH - boxH));
    }

    private static int clamp(int value, int lo, int hi) {
        if (value < lo) return lo;
        return Math.min(value, hi);
    }
}
