# Changelog

All notable changes to FishingStats will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.2] - 2026-07-21

### Changed
- Internal: the NeoForge metadata now declares the bundled `hudlibcore` next to
  `hudlib`. Both have always shipped inside the jar (jar-in-jar), only the
  declaration was incomplete — there is nothing extra to install and nothing
  changes in game.

## [1.1.1] - 2026-07-19

### Changed
- Version aligned across the Minecraft 1.21.x line. The movable HUD reached the
  1.21–1.21.5 jar in 1.1.1; this branch already had it, so **nothing changes
  functionally here** — the number is bumped only so one version identifies the
  whole 1.21.x release.

## [1.1.0] - 2026-07-14

### Added
- **NeoForge support** for Minecraft 1.21.9-1.21.11 - FishingStats now ships as both
  a Fabric and a NeoForge jar from one codebase (Architectury multiloader layout)

### Changed
- Restructured into `common` / `fabric` / `neoforge` modules; all game logic is shared
- Keybinds, the HUD layer and the tick hook are now registered through Architectury API,
  making a single NeoForge jar safe across the 1.21.10 -> 1.21.11 `Identifier` rename
- Architectury API is now a required dependency
- Unified the mod version to 1.1.0 across all loaders and Minecraft versions so
  every build carries one release number
- Standardized jar naming to `fishingstats-<loader>-<version>+mc<range>`
  (e.g. `fishingstats-fabric-1.1.0+mc1.21.9-1.21.11.jar`)
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
