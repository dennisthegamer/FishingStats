# FishingStats

**Track every cast. Know every catch. Master the art of fishing!**

A client-side Minecraft mod for Fabric and NeoForge that automatically tracks and analyzes all your fishing activity, recording casts, catches, bite times and treasure rates in real-time with a detailed session history, statistics and HUD overlay.

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

| Branch | Minecraft | Loaders | Java |
|--------|-----------|---------|------|
| `mc26.2` | 26.2 | Fabric 0.19.2+ / NeoForge 26.2+ | 25+ |
| `mc26.1` | 26.1 - 26.1.2 | Fabric 0.18.4+ / NeoForge 26.1.2 | 25+ |

Both a Fabric and a NeoForge jar are built from one codebase. On Fabric, **Fabric API** is required and **ModMenu** + **YACL** are recommended for the in-game config screen; on NeoForge, **YACL** is optional and the config screen opens from the mod list.

## Download

Download the matching jar for your Minecraft version from [GitHub Releases](https://github.com/DennisTheGamer/FishingStats/releases).

## Installation

### Fabric

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download [ModMenu](https://modrinth.com/mod/modmenu) and [YACL](https://modrinth.com/mod/yacl)
4. Download the `fishingstats-fabric-...` jar
5. Place all JAR files in your `mods` folder
6. Launch Minecraft

### NeoForge

1. Install the [NeoForge](https://neoforged.net/) loader
2. (Optional) Download [YACL](https://modrinth.com/mod/yacl) if you want the in-game config screen
3. Download the `fishingstats-neoforge-...` jar and place it in your `mods` folder
4. Launch Minecraft

## Configuration

Open the config screen via ModMenu (on NeoForge, use the **Config** button in the mod list). Available settings:

- **HUD** - Overlay on/off, compact mode, position (4 corners), opacity (0-100%), scale (50-150%), visibility mode
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
- **Built with**: Fabric, NeoForge, Fabric API, YACL, ModMenu

## Support

Report bugs on [GitHub Issues](https://github.com/DennisTheGamer/FishingStats/issues).
