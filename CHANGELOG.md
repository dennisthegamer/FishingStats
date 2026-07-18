# Changelog

All notable changes to FishingStats will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-07-18

Brings the draggable HUD from the Minecraft 26.2 build to 26.1 (this branch goes
straight from 1.1.0 to 1.2.1, so there is no 1.2.0 release here).

### Added
- **Freely positionable HUD**: the config screen has a new "Edit HUD Position..." button that
  opens an editor - drag the HUD anywhere on screen and confirm
- **Position presets**: save, apply, rename and delete HUD positions
- HudLib is bundled inside the jar (jar-in-jar); there is nothing extra to install

### Changed
- The HUD is no longer limited to the four screen corners. An existing `hudPosition`
  setting is migrated automatically to the same spot; the obsolete field disappears from
  the config file on the next save
- HUD drawing and the catch flash now come from the shared HudLib instead of mod-local code

## [1.1.0] - 2026-07-14

### Changed
- **The NeoForge jar now requires Minecraft 26.1.2** — MC 26.1/26.1.1 are no longer accepted on NeoForge; on Fabric they remain supported

### Added
- NeoForge support: the repo now builds two jars from one codebase -
  `fishingstats-fabric-1.1.0+mc26.1-26.1.2.jar` and `fishingstats-neoforge-1.1.0+mc26.1-26.1.2.jar`
- Config screen on NeoForge via the mod list "Config" button (`IConfigScreenFactory`);
  on Fabric it remains available through ModMenu

### Changed
- Restructured into a multiloader layout: shared sources in `common/`, thin loader
  modules in `fabric/` and `neoforge/` (no behavior changes)
- Loader lookups (config directory, game version) now go through a small `Platform`
  service instead of calling Fabric Loader directly
- Build toolchain: Gradle 9.5.1, Fabric Loom 1.17.13 (pinned release instead of
  `1.15-SNAPSHOT`), NeoForge ModDevGradle 2.0.141, NeoForge 26.1.2.78,
  ModMenu 18.0.0 (was 18.0.0-alpha.8)
- Unified the mod version to 1.1.0 across all loaders and Minecraft versions so
  every build carries one release number
- Standardized jar naming to `fishingstats-<loader>-<version>+mc<range>`
- Corrected author and contact metadata (Modrinth + GitHub links)

## [1.0.0] - 2026-07-03

### Added
- Initial release
- Automatic client-side catch detection via the fishing bobber - no server-side
  hooks, works in singleplayer and on servers alike
- Catch classification into five categories: Fish, Books, Rods & Equipment,
  Other Treasure and Junk, plus vanilla rarity (fish/treasure/junk)
- Time-to-bite measurement for every catch
- Rod enchantment snapshot per catch (Luck of the Sea, Lure, Unbreaking)
- Open water detection per catch
- Enchanted book catches store their enchantments (e.g. "Mending, Unbreaking 3")
- Session management with start/pause/resume via keybind (`J`)
- Automatic session pause when opening the ESC menu, with automatic resume when
  returning to the game; paused time never counts towards the session duration
- Automatic session end after a configurable inactivity timeout (default: 10 minutes) -
  the idle tail is not counted
- Optional session persistence across world closes (save/load from JSON)
- FishingStats window (`O`) with two tabs:
  1. **Sessions**: all sessions with date, duration, catch count and treasure rate;
     detail view with time range, biome/dimension, casts, category breakdown and
     per-species catch list
  2. **Statistics**: catch rates by rod enchantment combination, average time to
     bite per Lure level, latest session vs. all-time average (catches/h, treasure
     share) and rare finds with coordinates
- Session reset function (`R` in sessions screen)
- HUD overlay with two display modes:
  1. **Full mode**: session duration, casts, catches, treasure rate and last catch
  2. **Compact mode**: one-line summary (casts + catches), toggled via keybind (`K`)
- Customizable HUD position (top-left, top-right, bottom-left, bottom-right)
- Adjustable HUD opacity (0-100%) and scale (50-150%)
- HUD visibility toggle: always visible or only while fishing
- "Track Treasure Only" mode: only treasure catches are stored, fish and junk
  still update the HUD
- Full YACL config screen via ModMenu
- Persistent config saved to `config/fishingstats.json`
- Session data saved to `config/fishingstats_sessions.json`
- Localization support (English & German)
- Client-side only - no server installation required
- Compatibility with Minecraft 26.2
- Fabric Loader 0.19.2+ support
- Fabric API integration
