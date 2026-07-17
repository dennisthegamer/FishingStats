# Always-HUD — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der Schalter „Immer sichtbar" hält, was er verspricht — das HUD erscheint auch vor dem ersten Auswurf — und sagt im Leerlauf „keine Session". Auf allen sechs FishingStats-Branches mit HUD.

**Architecture:** Zwei kleine Änderungen an `FishingStatsHud.java` plus ein Lang-Key in zwei Sprachdateien, **wortgleich** auf sechs Branches wiederholt. Die `render()`-Bedingungen sind auf allen sechs identisch, nur die Zeilennummern verschieben sich. Kein Umbau, keine neuen Abhängigkeiten.

**Tech Stack:** Java 21, Gradle (Groovy DSL), Architectury multiloader (Fabric + NeoForge).

**Spec:** `docs/superpowers/specs/2026-07-17-always-hud-design.md`

## Global Constraints

- **Bei Schalter AUS darf sich NICHTS ändern.** Das `!snapshot.fishingNow()` in der zweiten Bedingung MUSS erhalten bleiben — ohne es verschwände das HUD bei Schalter-aus im Moment des ersten Auswurfs (`casts()` ist dann noch 0). Das wäre eine ungefragte Verhaltensänderung.
- **`computeLayout()` und `draw()` MÜSSEN dieselben Strings sehen**, sonst passt die Box nicht zum Text. Beide beziehen sie über `titleText()`/`castsText()` — diese Kopplung nicht aufbrechen.
- **Kein Default-Wechsel, keine Config-Migration.** `hudVisibleAlways` bleibt bei `false`.
- **Keine Tests** — FishingStats hat kein Test-Harness; die Verifikation ist der Ingame-Test.
- **`docs/` ist gitignored** → `git add -f`, sonst committet es nie.
- **Deploy-Ziel ausschließlich** `%APPDATA%\PandoraLauncher\instances\<Version> - <Loader>\.minecraft\mods\` — nie ModrinthApp.
- **`build/libs/` ist NICHT branch-getrennt** — dort liegen Jars anderer Branches. Jars immer namentlich wählen, nie `ls | head -1`.
- **Kein Push, kein Merge nach master** ohne ausdrückliche Ansage.

## File Structure

Pro Branch dieselben drei Dateien:

| Datei | Änderung |
|---|---|
| `common/src/main/java/de/dennisthegamer/fishingstats/render/FishingStatsHud.java` | `render()`-Guards + `statusSuffix()` |
| `common/src/main/resources/assets/fishingstats/lang/de_de.json` | Key `fishingstats.hud.no_session` |
| `common/src/main/resources/assets/fishingstats/lang/en_us.json` | Key `fishingstats.hud.no_session` |

Branches: `mc1.21.5`, `feat/draggable-hud`, `mc1.21-1.21.8`, `mc1.21.9-1.21.11`, `mc26.1`, `mc26.2`.
`main` hat das HUD in dieser Form nicht und bleibt außen vor.

---

### Task 1: Der Fix auf allen sechs Branches

**Files:** je Branch die drei Dateien oben.

**Interfaces:**
- Produces: auf jedem Branch ein Commit, dessen `render()` den Schalter durchgängig respektiert und dessen `statusSuffix()` nie leer ist.

- [ ] **Step 1: Arbeitsbaum prüfen**

```bash
cd "C:/Users/mager/Documents/Minecraft - Mods/FishingStats"
git status --porcelain    # MUSS leer sein
git branch --show-current
```
Nicht leer → stoppen und melden, **nicht** `git stash`.

- [ ] **Step 2: Pro Branch — `render()` umbauen**

Für jeden der sechs Branches: `git checkout <branch>`, dann in
`common/src/main/java/de/dennisthegamer/fishingstats/render/FishingStatsHud.java`
diese zwei Zeilen…

```java
        if (!config.hudVisibleAlways && !snapshot.fishingNow() && !sessionActive) return;
        if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;
```

…ersetzen durch:

```java
        // Der Schalter regiert BEIDE Ausstiege. Vorher fragte der zweite ihn nicht und versteckte
        // das HUD vor dem ersten Auswurf (casts() == 0), obwohl "Immer sichtbar" an war.
        if (!config.hudVisibleAlways) {
            if (!snapshot.fishingNow() && !sessionActive) return;
            // !fishingNow MUSS bleiben: sonst verschwaende das HUD bei Schalter-aus genau im
            // Moment des ersten Auswurfs, weil casts() dann noch 0 ist.
            if (snapshot.casts() == 0 && !snapshot.fishingNow()) return;
        }
```

- [ ] **Step 3: Pro Branch — `statusSuffix()` umbauen**

In derselben Datei, diese Methode…

```java
    private static String statusSuffix(HudState snapshot) {
        if (snapshot.duration().isEmpty()) return "";
        return " " + (char) 0x00B7 + " " + snapshot.duration()          // middle dot
                + (snapshot.paused() ? " " + (char) 0x23F8 : "");       // pause glyph
    }
```

…ersetzen durch:

```java
    private static String statusSuffix(HudState snapshot) {
        String status = snapshot.duration().isEmpty()
                ? I18n.get("fishingstats.hud.no_session")
                : snapshot.duration() + (snapshot.paused() ? " " + (char) 0x23F8 : "");  // pause glyph
        return " " + (char) 0x00B7 + " " + status;                                       // middle dot
    }
```

Der Javadoc darüber behauptet danach das Gegenteil (`— leer, solange keine Session läuft`) und wird mit ersetzt:

```java
    /** " · 12:34", " · 12:34 ⏸" oder " · keine Session" — nie leer. */
```

`I18n` ist in der Datei bereits importiert (`net.minecraft.client.resources.language.I18n`) — kein neuer Import nötig. Falls doch: melden, das hieße die Datei sieht anders aus als erwartet.

- [ ] **Step 4: Pro Branch — Lang-Keys**

`common/src/main/resources/assets/fishingstats/lang/de_de.json`, bei den anderen `fishingstats.hud.*`-Keys einsortieren:
```json
	"fishingstats.hud.no_session": "keine Session",
```

`common/src/main/resources/assets/fishingstats/lang/en_us.json`, an gleicher Stelle:
```json
	"fishingstats.hud.no_session": "no session",
```

Auf gültiges JSON achten (Komma der Vorzeile!) und die Einrückung der Datei übernehmen (Tabs).

- [ ] **Step 5: Pro Branch — Compile-Gegenprobe**

Run: `./gradlew :common:build --console=plain`
Expected: `BUILD SUCCESSFUL`. Das fängt Syntaxfehler und kaputtes JSON. Ein voller Loader-Build ist hier nicht nötig — der kommt in Task 2 für den Branch, der deployt wird.

Schlägt es auf einem Branch fehl: diesen Branch melden und die anderen trotzdem fertigmachen; ein Branch kann eine abweichende Datei haben.

- [ ] **Step 6: Pro Branch — Commit**

```bash
git add common/src/main/java/de/dennisthegamer/fishingstats/render/FishingStatsHud.java \
        common/src/main/resources/assets/fishingstats/lang/de_de.json \
        common/src/main/resources/assets/fishingstats/lang/en_us.json
git commit -m "fix(hud): 'Immer sichtbar' zeigt das HUD auch vor dem ersten Auswurf"
```
Gezielt adden, **nicht** `git add -A` — `docs/` ist zwar gitignored, aber gezielt ist gezielt.

- [ ] **Step 7: Gegenprobe über alle sechs**

```bash
for b in mc1.21.5 feat/draggable-hud mc1.21-1.21.8 mc1.21.9-1.21.11 mc26.1 mc26.2; do
  n=$(git show "$b:common/src/main/java/de/dennisthegamer/fishingstats/render/FishingStatsHud.java" | grep -c "hud.no_session")
  m=$(git show "$b:common/src/main/resources/assets/fishingstats/lang/en_us.json" | grep -c "hud.no_session")
  echo "$b: render=$n lang=$m"
done
```
Expected: überall `render=1 lang=1`.

- [ ] **Step 8: Auf `mc1.21-1.21.8` zurückstellen** (Task 2 baut dort)

```bash
git checkout mc1.21-1.21.8
```

---

### Task 2: Bauen und nach 1.21.8 deployen

**Files:** keine — Build und Deploy.

- [ ] **Step 1: Beide Loader bauen**

Run:
```bash
cd "C:/Users/mager/Documents/Minecraft - Mods/FishingStats"
./gradlew :fabric:build :neoforge:build --console=plain
```
Expected: beide `BUILD SUCCESSFUL`.

- [ ] **Step 2: Deployen — Jars namentlich, nicht per `head -1`**

`build/libs/` enthält Jars anderer Branches (`+mc1.21-1.21.5`, `+mc26.2`). Die richtigen heißen:
- `fabric/build/libs/fishingstats-fabric-1.1.0+mc1.21.6-1.21.8.jar`
- `neoforge/build/libs/fishingstats-neoforge-1.1.0+mc1.21.6-1.21.8.jar`

```bash
I="$APPDATA/PandoraLauncher/instances"
cp -v "fabric/build/libs/fishingstats-fabric-1.1.0+mc1.21.6-1.21.8.jar"     "$I/1.21.8 - Fabric/.minecraft/mods/"
cp -v "neoforge/build/libs/fishingstats-neoforge-1.1.0+mc1.21.6-1.21.8.jar" "$I/1.21.8 - NeoForge/.minecraft/mods/"
rm -rf "$I/1.21.8 - Fabric/.minecraft/.fabric/processedMods/"*hudlib* 2>/dev/null
```
Gleichnamige vorhandene Jars werden ersetzt — das ist gewollt.

- [ ] **Step 3: Gegenprobe, dass wirklich das neue Jar liegt**

```bash
for i in "1.21.8 - Fabric" "1.21.8 - NeoForge"; do
  f=$(ls "$I/$i/.minecraft/mods/"fishingstats*)
  echo "--- $i: $(basename "$f") ($(date -r "$f" '+%H:%M'))"
  unzip -p "$f" assets/fishingstats/lang/en_us.json | grep no_session
done
```
Expected: aktueller Zeitstempel, und `"fishingstats.hud.no_session": "no session"` im deployten Jar. Fehlt der Key, liegt das alte Jar dort.

- [ ] **Step 4: Ingame-Test durch den Nutzer — das Tor**

In **beiden** Instanzen, Schalter „Immer sichtbar" in der Config **an**:
1. Welt betreten, **ohne** auszuwerfen → HUD ist da, sagt „FishingStats · keine Session"
2. Auswerfen → Zähler laufen, Suffix wechselt auf die Session-Zeit
3. Schalter wieder aus → altes Verhalten: HUD erscheint erst beim Angeln

Der Config-Schalter steht in der bestehenden Config auf `false` (bewusst kein Default-Wechsel), muss zum Testen also einmal angeschaltet werden.
