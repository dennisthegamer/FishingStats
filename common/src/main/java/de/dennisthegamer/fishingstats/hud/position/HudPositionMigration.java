package de.dennisthegamer.fishingstats.hud.position;

/**
 * Einmalige Migration der alten 4-Ecken-Enum-Position (String) auf das freie
 * {@link HudPlacement}. Der 10px-Rand entspricht dem bisherigen {@code MARGIN} im HUD-Renderer.
 */
public final class HudPositionMigration {

    public static final int LEGACY_MARGIN = 10;

    private HudPositionMigration() {
    }

    public static HudPlacement fromLegacy(String legacy) {
        if (legacy == null) {
            return new HudPlacement(HudAnchor.TOP_LEFT, LEGACY_MARGIN, LEGACY_MARGIN);
        }
        return switch (legacy) {
            case "TOP_RIGHT" -> new HudPlacement(HudAnchor.TOP_RIGHT, -LEGACY_MARGIN, LEGACY_MARGIN);
            case "BOTTOM_LEFT" -> new HudPlacement(HudAnchor.BOTTOM_LEFT, LEGACY_MARGIN, -LEGACY_MARGIN);
            case "BOTTOM_RIGHT" -> new HudPlacement(HudAnchor.BOTTOM_RIGHT, -LEGACY_MARGIN, -LEGACY_MARGIN);
            default -> new HudPlacement(HudAnchor.TOP_LEFT, LEGACY_MARGIN, LEGACY_MARGIN);
        };
    }
}
