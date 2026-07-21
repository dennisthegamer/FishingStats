# FishingStats

**Track every cast. Know every catch. Master the art of fishing!**

A client-side Minecraft Fabric mod that automatically tracks and analyzes all your fishing activity, recording casts, catches, bite times and treasure rates in real-time with a detailed session history, statistics and HUD overlay.

## Features

- Automatic client-side catch detection - works in singleplayer and on servers, auto-fishing mods included
- Catch classification: Fish (per species), Books, Rods & Equipment, Other Treasure and Junk
- Time-to-bite measurement, rod enchantment snapshot and open water detection per catch
- Session management with start/pause/resume keybind; the ESC menu pauses automatically
- Optional session persistence across world closes
- Session history with detail view: time range, biome, casts, category breakdown, per-species catch list
- Overall statistics: catch rates per enchantment combination, bite-time histogram, average per Lure level, latest session vs. all-time average, rare finds with coordinates
- HUD overlay with full and compact display modes
- Full in-game config screen (YACL + ModMenu)
- English and German localization
- Client-side only - no server installation required

## Compatibility

| Branch | Minecraft | Fabric Loader | Java |
|--------|-----------|---------------|------|
| `mc26.2` | 26.2 | 0.19.2+ | 25+ |
| `mc26.1` | 26.1 | 0.18.4+ | 25+ |
| `mc1.21.9-1.21.11` | 1.21.9 - 1.21.11 | 0.18.3+ | 21+ |
| `mc1.21.6-1.21.8` | 1.21.6 - 1.21.8 | 0.18.3+ | 21+ |
| `mc1.21.5` | 1.21 - 1.21.5 | 0.18.3+ | 21+ |

**Fabric API** is required; **ModMenu** and **YACL** are recommended for the in-game config screen.

## Download

Download the matching jar for your Minecraft version from [GitHub Releases](https://github.com/DennisTheGamer/FishingStats/releases).

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download [ModMenu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl)
4. Download FishingStats (this mod)
5. Place all JAR files in your `mods` folder
6. Launch Minecraft

## Configuration

Open the config screen via ModMenu. Available settings:

- **HUD** - Overlay on/off, compact mode, freely placeable position (drag editor with presets), opacity (0-100%), scale (50-150%), visibility mode
- **Tracking** - Track treasure only, session auto-split timeout, session persistence

Keybinds are listed under **Controls > FishingStats**:
- `O` - Open the FishingStats window (Sessions & Statistics)
- `J` - Start/pause/resume session
- `K` - Toggle compact HUD mode
- `R` - Reset session (in the sessions screen)

Config is saved to `config/fishingstats.json`, session data to `config/fishingstats_sessions.json`.

## Building from Source

```bash
git clone https://github.com/DennisTheGamer/FishingStats.git
cd FishingStats
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

- **Author**: Dennis_thegamer
- **Built with**: Fabric, Fabric API, YACL, ModMenu

## Support

Report bugs on [GitHub Issues](https://github.com/DennisTheGamer/FishingStats/issues).
