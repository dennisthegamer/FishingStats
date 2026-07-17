# Always-HUD — „Immer sichtbar" soll wirklich immer sichtbar heißen

**Datum:** 2026-07-17
**Status:** Design vom Nutzer freigegeben
**Reichweite:** alle sechs FishingStats-Branches mit HUD

## Das Problem

Der Nutzer: *„Es soll nicht erst angezeigt werden, nachdem ich die Angel ausgeworfen habe, sondern
immer. Im HUD sollte auch stehen, wenn die Session noch nicht gestartet hat."*

Das ist **kein fehlendes Feature, sondern ein Bug**. Das Config-Feld `hudVisibleAlways` und der
Schalter „Immer sichtbar" existieren längst — `render()` hat aber zwei Ausstiege hintereinander:

```java
if (!config.hudVisibleAlways && !snapshot.fishingNow() && !sessionActive) return;  // respektiert den Schalter
if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;                       // fragt ihn NICHT
```

Die zweite Zeile kennt den Schalter nicht. Vor dem ersten Auswurf ist `casts() == 0`, also
verschwindet das HUD — **auch mit „Immer sichtbar" an**. Der Schalter ist damit wirkungslos für genau
den Fall, für den er gedacht war.

Zusätzlich liefert `statusSuffix()` einen leeren String, solange keine Session läuft, sodass das HUD
im Leerlauf nichts über seinen Zustand sagt.

## Befund: der Bug ist auf allen sechs Branches wortgleich

| Branch | MC-Bereich | Ausstieg vorhanden |
|---|---|---|
| `mc1.21.5` | 1.21–1.21.5 | ja |
| `feat/draggable-hud` | 1.21–1.21.5 + Draggable-HUD | ja |
| `mc1.21-1.21.8` | 1.21.6–1.21.8 | ja |
| `mc1.21.9-1.21.11` | 1.21.9–1.21.11 | ja |
| `mc26.1` | 26.1.x | ja |
| `mc26.2` | 26.2 | ja |

Die `render()`-Bedingungen sind auf allen sechs identisch; nur die Zeilennummern verschieben sich
(56/60/61 bis 57/61/62). `main` hat das HUD in dieser Form nicht und bleibt außen vor.

## Entscheidungen

- **Der Schalter bleibt** (Nutzerentscheidung). Kein Default-Wechsel, keine Migration bestehender
  Configs. Der Nutzer schaltet ihn selbst an. Verworfen: `hudVisibleAlways` streichen und `hudEnabled`
  allein regieren lassen.
- **Leerlauf-Text: „keine Session"** über die bestehende Suffix-Mechanik. Verworfen: „bereit"
  (unpräziser), gar kein Text (sagt dem Nutzer nichts).
- **Keine Tests.** FishingStats hat **null Testdateien**; die Tests sind bei der HudLib-Extraktion
  dorthin gewandert, zwei Branches tragen nur noch ein leeres JUnit-Gerüst. Die Sichtbarkeit hängt an
  Minecraft-gekoppelten Statics (`Minecraft.getInstance()`, `I18n`). Ein Harness auf sechs Branches
  für einen Zweizeiler wäre unverhältnismäßig. Verifikation ist der Ingame-Test.

## Änderung 1 — der Schalter regiert beide Ausstiege

```java
if (!config.hudVisibleAlways) {
    if (!snapshot.fishingNow() && !sessionActive) return;
    if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;
}
```

**Bei Schalter AUS ist das exakt das bisherige Verhalten** — der nicht angefragte Fall wird bewusst
nicht angefasst. Das `!snapshot.fishingNow()` in der zweiten Zeile MUSS bleiben: ohne es verschwände
das HUD bei Schalter-aus im Moment des ersten Auswurfs (`casts()` ist dann noch 0), was eine
ungefragte Verhaltensänderung wäre.

**Bei Schalter AN** bleibt das HUD stehen; `hudEnabled` und `client.options.hideGui` (F1) bleiben die
einzigen Ausschalter.

## Änderung 2 — Leerlauf-Text

```java
private static String statusSuffix(HudState snapshot) {
    String status = snapshot.duration().isEmpty()
            ? I18n.get("fishingstats.hud.no_session")
            : snapshot.duration() + (snapshot.paused() ? " " + (char) 0x23F8 : "");
    return " " + (char) 0x00B7 + " " + status;
}
```

Neuer Lang-Key in **beiden** Sprachdateien:
- `de_de.json`: `"fishingstats.hud.no_session": "keine Session"`
- `en_us.json`: `"fishingstats.hud.no_session": "no session"`

Wirkt automatisch in beiden Darstellungsmodi, weil `titleText()` (normal) und `castsText()` (kompakt)
beide durch `statusSuffix()` gehen.

**Layout-Invariante bleibt gewahrt:** `computeLayout()` und `draw()` müssen dieselben Strings sehen,
sonst passt die Box nicht zum Text. Beide beziehen ihre Strings über `titleText`/`castsText`, also
gilt das weiterhin. Die Box wird im Leerlauf breiter („keine Session" ist länger als „12:34") — das
ist korrekt und gewollt.

**Editor-Vorschau bleibt unberührt:** `sampleState()` liefert eine feste Dauer `"12:34"`, zeigt also
weiterhin die Zeit statt „keine Session", und die Box behält im Editor ihre stabile Breite.

## Verifikation

- Beide Loader bauen auf jedem der sechs Branches.
- **Ingame auf 1.21.8** (Fabric + NeoForge), durch den Nutzer:
  1. Welt betreten, **ohne** auszuwerfen → HUD ist da und sagt „FishingStats · keine Session"
  2. Auswerfen → Zähler laufen, Suffix wechselt auf die Session-Zeit
  3. Schalter „Immer sichtbar" aus → altes Verhalten, HUD erscheint erst beim Angeln

## Nicht in diesem Vorhaben

- Kein Default-Wechsel, keine Config-Migration.
- Kein Umbau der zwei überlappenden Schalter (`hudEnabled` / `hudVisibleAlways`).
- Keine Tests / kein Test-Harness.
- MiningStats (hat ein eigenes HUD mit eigener Sichtbarkeitslogik).
