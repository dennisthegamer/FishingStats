package de.dennisthegamer.fishingstats.hud.position;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudAnchorTest {

    @Test
    void baseX_left_isZero() {
        assertEquals(0, HudAnchor.TOP_LEFT.baseX(400, 100));
    }

    @Test
    void baseX_right_isScreenMinusBox() {
        assertEquals(300, HudAnchor.TOP_RIGHT.baseX(400, 100));
    }

    @Test
    void baseX_center_isCentered() {
        assertEquals(150, HudAnchor.TOP_CENTER.baseX(400, 100));
    }

    @Test
    void baseY_bottom_isScreenMinusBox() {
        assertEquals(180, HudAnchor.BOTTOM_LEFT.baseY(200, 20));
    }

    @Test
    void fromCenter_topRightRegion_returnsTopRight() {
        assertEquals(HudAnchor.TOP_RIGHT, HudAnchor.fromCenter(390, 5, 400, 200));
    }

    @Test
    void fromCenter_middleCenterRegion_returnsMiddleCenter() {
        assertEquals(HudAnchor.MIDDLE_CENTER, HudAnchor.fromCenter(200, 100, 400, 200));
    }

    @Test
    void translationKey_isLowercaseName() {
        assertEquals("fishingstats.hud.anchor.bottom_right", HudAnchor.BOTTOM_RIGHT.translationKey());
    }
}
