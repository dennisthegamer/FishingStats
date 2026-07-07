package de.dennisthegamer.fishingstats.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class FishingStatsConfigScreen {

    public static Screen create(Screen parent) {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        FishingStatsConfig defaults = new FishingStatsConfig();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("config.fishingstats.title"))
                .category(createHudCategory(config, defaults))
                .category(createTrackingCategory(config, defaults))
                .save(config::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory createHudCategory(FishingStatsConfig config, FishingStatsConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("config.fishingstats.category.hud"))
                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_enabled"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_enabled.tooltip")))
                        .binding(defaults.hudEnabled, () -> config.hudEnabled, v -> config.hudEnabled = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_compact"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_compact.tooltip")))
                        .binding(defaults.hudCompact, () -> config.hudCompact, v -> config.hudCompact = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<FishingStatsConfig.HudPosition>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_position"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_position.tooltip")))
                        .binding(defaults.getHudPosition(), config::getHudPosition, v -> config.hudPosition = v.name())
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(FishingStatsConfig.HudPosition.class)
                                .formatValue(FishingStatsConfigScreen::positionName))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_visible_always"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_visible_always.tooltip")))
                        .binding(defaults.hudVisibleAlways, () -> config.hudVisibleAlways, v -> config.hudVisibleAlways = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_opacity"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_opacity.tooltip")))
                        .binding((int) (defaults.hudOpacity * 100),
                                () -> (int) (config.hudOpacity * 100),
                                v -> config.hudOpacity = v / 100f)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100)
                                .step(5))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("config.fishingstats.hud_scale"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.hud_scale.tooltip")))
                        .binding((int) (defaults.hudScale * 100),
                                () -> (int) (config.hudScale * 100),
                                v -> config.hudScale = v / 100f)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(50, 150)
                                .step(10))
                        .build())
                .build();
    }

    private static ConfigCategory createTrackingCategory(FishingStatsConfig config, FishingStatsConfig defaults) {
        return ConfigCategory.createBuilder()
                .name(Text.translatable("config.fishingstats.category.tracking"))
                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("config.fishingstats.track_treasure_only"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.track_treasure_only.tooltip")))
                        .binding(defaults.trackTreasureOnly, () -> config.trackTreasureOnly, v -> config.trackTreasureOnly = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Text.translatable("config.fishingstats.session_split_minutes"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.session_split_minutes.tooltip")))
                        .binding(defaults.sessionSplitMinutes,
                                () -> config.sessionSplitMinutes,
                                v -> config.sessionSplitMinutes = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(1, 60)
                                .step(1))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Text.translatable("config.fishingstats.persist_sessions"))
                        .description(OptionDescription.of(Text.translatable("config.fishingstats.persist_sessions.tooltip")))
                        .binding(defaults.persistSessions, () -> config.persistSessions, v -> config.persistSessions = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .build();
    }

    private static Text positionName(FishingStatsConfig.HudPosition position) {
        return Text.translatable("config.fishingstats.position."
                + position.name().toLowerCase(java.util.Locale.ROOT));
    }
}
