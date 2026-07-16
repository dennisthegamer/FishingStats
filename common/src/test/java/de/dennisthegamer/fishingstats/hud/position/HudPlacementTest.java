package de.dennisthegamer.fishingstats.hud.position;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudPlacementTest {

    @Test
    void resolve_appliesAnchorPlusOffset() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 10, 10);
        assertEquals(10, p.resolveX(400, 100));
        assertEquals(10, p.resolveY(200, 20));
    }

    @Test
    void resolve_topRightWithNegativeOffset() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_RIGHT, -10, 10);
        // baseX = 400-100 = 300; +(-10) = 290
        assertEquals(290, p.resolveX(400, 100));
    }

    @Test
    void resolve_clampsRightEdge() {
        // Offset weit über den Rand hinaus -> auf screenW-boxW geklemmt
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 9999, 0);
        assertEquals(300, p.resolveX(400, 100));
    }

    @Test
    void resolve_clampsToZeroWhenNegative() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, -50, -50);
        assertEquals(0, p.resolveX(400, 100));
        assertEquals(0, p.resolveY(200, 20));
    }

    @Test
    void resolve_boxLargerThanScreen_clampsToZero() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 0, 0);
        assertEquals(0, p.resolveX(50, 100)); // boxW>screenW -> hi=0
    }

    @Test
    void fromAbsolute_roundTripsThroughResolve() {
        // Aus absoluten Wunschkoordinaten erzeugt, muss resolve dieselben (geklemmten) liefern.
        HudPlacement p = HudPlacement.fromAbsolute(HudAnchor.BOTTOM_RIGHT, 250, 150, 400, 200, 100, 20);
        assertEquals(250, p.resolveX(400, 100));
        assertEquals(150, p.resolveY(200, 20));
    }

    @Test
    void of_hasZeroOffset() {
        HudPlacement p = HudPlacement.of(HudAnchor.MIDDLE_CENTER);
        assertEquals(0, p.offsetX());
        assertEquals(0, p.offsetY());
    }
}
