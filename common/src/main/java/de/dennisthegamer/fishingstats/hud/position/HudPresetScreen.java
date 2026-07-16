package de.dennisthegamer.fishingstats.hud.position;

import de.dennisthegamer.fishingstats.config.FishingStatsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Picker: oben feste Anker-Presets (9 Buttons), unten eigene Slots (Anwenden/Umbenennen/Löschen)
 * plus ein Namensfeld zum Speichern der aktuellen Position. Änderungen an Slots werden sofort
 * persistiert; die Auswahl eines Presets/Slots setzt die Arbeitskopie im Editor.
 */
public class HudPresetScreen extends Screen {

    private final HudEditorScreen editor;
    private EditBox nameField;
    /** >=0: Namensfeld benennt diesen Slot um; -1: „Speichern" legt neuen Slot an. */
    private int renameIndex = -1;

    public HudPresetScreen(HudEditorScreen editor) {
        super(Component.translatable("fishingstats.hud.preset.title"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        FishingStatsConfig config = FishingStatsConfig.getInstance();

        // Feste Anker-Presets: 3x3-Raster oben.
        int gridLeft = this.width / 2 - 154;
        int gridTop = 40;
        HudAnchor[] anchors = HudAnchor.values();
        for (int i = 0; i < anchors.length; i++) {
            HudAnchor anchor = anchors[i];
            int col = i % 3;
            int row = i / 3;
            addRenderableWidget(Button.builder(
                            Component.translatable(anchor.translationKey()),
                            b -> {
                                editor.applyWorking(HudPlacement.of(anchor));
                                Minecraft.getInstance().setScreen(editor);
                            })
                    .bounds(gridLeft + col * 104, gridTop + row * 22, 100, 20)
                    .build());
        }

        // Eigene Slots: je eine Zeile mit Anwenden / Umbenennen / Löschen.
        int slotTop = gridTop + 3 * 22 + 12;
        for (int i = 0; i < config.hudSlots.size(); i++) {
            final int index = i;
            HudPreset slot = config.hudSlots.get(i);
            int y = slotTop + i * 22;
            addRenderableWidget(Button.builder(
                            Component.translatable("fishingstats.hud.preset.apply_named", slot.name()),
                            b -> {
                                editor.applyWorking(config.hudSlots.get(index).placement());
                                Minecraft.getInstance().setScreen(editor);
                            })
                    .bounds(gridLeft, y, 160, 20).build());
            addRenderableWidget(Button.builder(
                            Component.translatable("fishingstats.hud.preset.rename"),
                            b -> beginRename(index))
                    .bounds(gridLeft + 164, y, 70, 20).build());
            addRenderableWidget(Button.builder(
                            Component.translatable("fishingstats.hud.preset.delete"),
                            b -> {
                                config.hudSlots.remove(index);
                                config.save();
                                rebuild();
                            })
                    .bounds(gridLeft + 238, y, 70, 20).build());
        }

        // Namensfeld + Speichern-Button + Zurück.
        int bottom = this.height - 52;
        this.nameField = new EditBox(this.font, gridLeft, bottom, 160, 20,
                Component.translatable("fishingstats.hud.preset.name_hint"));
        this.nameField.setHint(Component.translatable("fishingstats.hud.preset.name_hint"));
        addRenderableWidget(this.nameField);
        addRenderableWidget(Button.builder(Component.translatable("fishingstats.hud.preset.save"), b -> saveCurrent())
                .bounds(gridLeft + 164, bottom, 144, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> Minecraft.getInstance().setScreen(editor))
                .bounds(this.width / 2 - 75, this.height - 26, 150, 20).build());
    }

    private void beginRename(int index) {
        this.renameIndex = index;
        this.nameField.setValue(FishingStatsConfig.getInstance().hudSlots.get(index).name());
    }

    private void saveCurrent() {
        FishingStatsConfig config = FishingStatsConfig.getInstance();
        String name = nameField.getValue().isBlank()
                ? ("Slot " + (config.hudSlots.size() + 1))
                : nameField.getValue().trim();
        if (renameIndex >= 0 && renameIndex < config.hudSlots.size()) {
            // Umbenennen: Placement behalten, nur Namen ersetzen.
            HudPreset old = config.hudSlots.get(renameIndex);
            config.hudSlots.set(renameIndex, new HudPreset(name, old.placement()));
            renameIndex = -1;
        } else {
            // Neuer Slot mit aktueller Arbeitskopie aus dem Editor.
            config.hudSlots.add(new HudPreset(name, editor.working()));
        }
        config.save();
        rebuild();
    }

    private void rebuild() {
        this.renameIndex = -1;
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(editor);
    }
}
