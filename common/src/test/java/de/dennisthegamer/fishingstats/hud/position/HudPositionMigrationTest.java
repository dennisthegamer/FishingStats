package de.dennisthegamer.fishingstats.hud.position;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudPositionMigrationTest {

    @Test
    void topLeft_getsPositiveMargin() {
        HudPlacement p = HudPositionMigration.fromLegacy("TOP_LEFT");
        assertEquals(HudAnchor.TOP_LEFT, p.anchor());
        assertEquals(10, p.offsetX());
        assertEquals(10, p.offsetY());
    }

    @Test
    void topRight_getsNegativeXMargin() {
        HudPlacement p = HudPositionMigration.fromLegacy("TOP_RIGHT");
        assertEquals(HudAnchor.TOP_RIGHT, p.anchor());
        assertEquals(-10, p.offsetX());
        assertEquals(10, p.offsetY());
    }

    @Test
    void bottomRight_getsNegativeMargins() {
        HudPlacement p = HudPositionMigration.fromLegacy("BOTTOM_RIGHT");
        assertEquals(HudAnchor.BOTTOM_RIGHT, p.anchor());
        assertEquals(-10, p.offsetX());
        assertEquals(-10, p.offsetY());
    }

    @Test
    void nullOrUnknown_fallsBackToTopLeftMargin() {
        HudPlacement pn = HudPositionMigration.fromLegacy(null);
        assertEquals(HudAnchor.TOP_LEFT, pn.anchor());
        assertEquals(10, pn.offsetX());
        HudPlacement pu = HudPositionMigration.fromLegacy("GARBAGE");
        assertEquals(HudAnchor.TOP_LEFT, pu.anchor());
    }
}
