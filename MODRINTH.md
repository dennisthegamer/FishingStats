# FishingStats

**Track every cast. Know every catch. Master the art of fishing!**

FishingStats is a lightweight, client-side mod for **Fabric and NeoForge** that automatically tracks and analyzes all your fishing activity. It records every cast and catch, times your bites, classifies your loot and keeps a full session history and statistics behind a clean live HUD - so you can see which rod, biome and technique actually pays off.

## Features

### Catch Tracking
- **Automatic detection** - Every cast and catch is recorded instantly, purely client-side (works in singleplayer and on servers, auto-fishing mods included)
- **Catch categories** - Fish (per species), Books, Rods & Equipment, Other Treasure and Junk
- **Bite timing** - Time from cast to bite is measured for every catch
- **Rod awareness** - Luck of the Sea, Lure and Unbreaking levels are stored with each catch
- **Open water detection** - Records whether a catch happened in open water
- **Session management** - Start, pause and resume tracking with a single keybind; the ESC menu pauses automatically and resumes when you return to the game
- **Session persistence** - Optionally keep the active session across world closes

### Live HUD Overlay
- **Full mode** - Session duration, casts, catches, treasure rate and the last caught item
- **Compact mode** - Clean one-line summary with casts and catches
- **Customizable** - Freely placeable position (drag editor with presets), opacity, scale and visibility all adjustable

### Session History & Statistics
- **Sessions screen** - Browse all fishing sessions with date, duration, catches and treasure rate
- **Session details** - Time range, biome and dimension, cast/catch counts, category breakdown and full catch list per species
- **Enchantment statistics** - Catch rates compared by rod enchantment combination
- **Bite time analysis** - Average time to bite, broken down per Lure level
- **Session comparison** - Latest session vs. your all-time average (catches/h, treasure share)
- **Rare finds** - Treasure catches with biome and coordinates, so you can find that spot again

## Controls

| Key | Action |
|-----|--------|
| **O** | Open the FishingStats window (Sessions & Statistics) |
| **J** | Start / pause / resume session |
| **K** | Toggle compact HUD mode |
| **R** | Reset session (in the sessions screen) |

## Customization

Configure everything in-game via the YACL config screen (opened through ModMenu on Fabric, or the **Config** button in the mod list on NeoForge):

**Tracking**
- Track treasure only (fish and junk still update the HUD)
- Session auto-split after a configurable inactivity timeout
- Session persistence on/off

**HUD**
- Position: top-left, top-right, bottom-left, bottom-right
- Opacity: 0-100%
- Scale: 50-150%
- Visibility: always visible or only while fishing

Config is saved to `config/fishingstats.json`, session data to `config/fishingstats_sessions.json`.

## Requirements

- **Minecraft:** 26.2
- **Loader:** Fabric (Fabric Loader 0.19.2 or higher) or NeoForge (26.2 or higher)
- **Fabric API:** Required on Fabric
- **Java:** 25 or higher
- **ModMenu + YACL:** Recommended on Fabric (YACL is optional on NeoForge, only needed to open the config screen)

## Installation

**Fabric:** Install Fabric Loader + Fabric API, then drop the `fishingstats-fabric-...` jar into your `mods` folder. ModMenu and YACL are recommended for the config screen.

**NeoForge:** Install the NeoForge loader, then drop the `fishingstats-neoforge-...` jar into your `mods` folder. YACL is optional if you want the in-game config screen.

## Perfect For

- **Anglers** - See which rod, biome and technique actually pays off
- **Treasure Hunters** - Track treasure rates and remember where the rare finds dropped
- **AFK Fishers** - Compare sessions and catches per hour at a glance
- **Everyone** - Anyone who casts a fishing rod in Minecraft!

## Languages

Available in **English** and **German**.

## Links

- **Issues & Bugs:** [GitHub Issues](https://github.com/DennisTheGamer/FishingStats/issues)
- **Source Code:** [GitHub Repository](https://github.com/DennisTheGamer/FishingStats)

## License

This mod is open-source and licensed under the **MIT License**.

---

**Made with love by Dennis_thegamer**
