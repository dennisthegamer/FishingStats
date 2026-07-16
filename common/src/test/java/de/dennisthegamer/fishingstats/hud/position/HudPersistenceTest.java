package de.dennisthegamer.fishingstats.hud.position;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Headless Gson round-trip check for the persisted HUD position types. {@link HudPlacement}
 * and {@link HudPreset} are Minecraft-free records, so (de)serialization can be verified
 * without a running client, using the same Gson configuration as the mod's own persistence
 * (see FishingStatsConfig / FishingDataStore).
 */
class HudPersistenceTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void hudPlacement_roundTripsThroughGson() {
        HudPlacement original = new HudPlacement(HudAnchor.BOTTOM_RIGHT, -10, 25);

        String json = GSON.toJson(original);
        HudPlacement roundTripped = GSON.fromJson(json, HudPlacement.class);

        assertEquals(original, roundTripped);
    }

    @Test
    void hudPreset_roundTripsThroughGson_withNestedPlacementPreserved() {
        HudPreset original = new HudPreset("My Slot", new HudPlacement(HudAnchor.MIDDLE_CENTER, 5, -7));

        String json = GSON.toJson(original);
        HudPreset roundTripped = GSON.fromJson(json, HudPreset.class);

        assertEquals(original, roundTripped);
        assertEquals(original.placement(), roundTripped.placement());
        assertEquals(original.name(), roundTripped.name());
    }

    @Test
    void hudPresetList_roundTripsThroughGson() {
        List<HudPreset> original = List.of(
                new HudPreset("Slot A", new HudPlacement(HudAnchor.TOP_LEFT, 0, 0)),
                new HudPreset("Slot B", new HudPlacement(HudAnchor.BOTTOM_RIGHT, -10, -10))
        );

        String json = GSON.toJson(original);
        Type listType = new TypeToken<List<HudPreset>>() {}.getType();
        List<HudPreset> roundTripped = GSON.fromJson(json, listType);

        assertEquals(original, roundTripped);
    }
}
