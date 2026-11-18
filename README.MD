# TheSandboxCore

Core plugin for TheSandbox Minecraft server, providing player utilities, staff moderation tools, guild management,
Discord integration, and more.

> The standalone [`thesurvivalbox`](https://github.com/Apfelchorle/thesurvivalbox) repository is superseded by this
> repository. The survival-safe subset is now built from here with `-Pflavor=survival`.

**Started by** [@ivannomoly](https://github.com/ivannomoly) · **Continued
by** [@apfelchorle](https://github.com/Apfelchorle) and contributors

---

## Quick Start

### Prerequisites

- **Java 25** (toolchain enforced in `build.gradle`)
- **Paper 1.26.x** (or compatible fork like Purpur)
- **Required plugins**: LuckPerms, Essentials, packetevents
- **Optional plugins**: LiteBans, PremiumVanish, LibsDisguises, SkinsRestorer, Vault, CoreProtect

### Building

```bash
./gradlew build
```

The shaded JAR is output to `build/libs/thesandboxcore-1.0.0.jar`.

### Build flavors

The same source tree builds two variants. Features are gated at build time, not with a runtime config flag.

| Flavor | Command | Output |
|---|---|---|
| `sandbox` (default) | `./gradlew build` | `build/libs/thesandboxcore-1.0.0.jar` |
| `survival` | `./gradlew build -Pflavor=survival` or `./gradlew survival` | `build/libs/thesurvivalbox-1.0.0.jar` |

Flavors live in `flavors/<name>.properties` and define three comma-separated allowlists:

- **`commands`** — keys from `plugin.yml`. Disabled commands are stripped from the packaged `plugin.yml` and skipped by the runtime registrar.
- **`items`** — item ids such as `wind-rod` or `float-boat`. Disabled items are never constructed, so their Bukkit/PacketEvents listeners and scheduled tasks never start.
- **`features`** — `animated-maps`, `rideable-dragon`, `discord-console`, `discord-uploads`, `discord-masterbuilder`. Disabled services are not created and disabled Discord slash commands are not registered.

`*` means everything, an empty value means nothing. The survival list is fail-closed: a new command stays sandbox-only until it is added to `flavors/survival.properties`. The Gradle build fails if an allowlisted command key does not exist in `plugin.yml`.

The generated `flavor.properties` is packaged into the jar, and `BuildFlavor` gates registration at runtime as a second layer for IDE runs and hand-edited `plugin.yml` files.

A few feature dependencies to keep in mind:

- `/shop` constructs every custom item, so it requires `items=*` (or all item ids).
- `/gifmap` requires `features=animated-maps`.
- The stacking-potato item requires `features=rideable-dragon`.
- `/dismount` works in a survival build without custom items; its item/dragon hooks are optional.
- Discord slash commands follow the same gate: `/console` needs `discord-console`, `/uploadschem` needs `discord-uploads`, `/masterbuilder` needs `discord-masterbuilder` plus `rank`, and `/list`, `/update`, `/devtoggle` follow their in-game command entries.

### Installing

1. Drop the JAR into your server's `plugins/` directory
2. Ensure all required plugins are present
3. Start the server — configs are auto-generated on first run
4. Configure MySQL in the generated config for persistent player data

### Configuration

The plugin uses a MySQL-backed `DataManager` for player data (last location, IP, rank, coins, etc.). Configure your
database connection in the plugin's config file after first run.

- `config.yml` — server settings, including MySQL TLS, updater verification, and `trusted-uuids`.
- `messages.yml` — every command prefix and common message. Edit this file to re-theme the plugin.
- `/config` — safe, typed player/global settings. Only allowlisted keys are exposed; secrets, database credentials,
  API keys, and `trusted-uuids` cannot be read or written through it.
- `/thesandboxcore reload` — reloads `config.yml` and `messages.yml`.

`worlds.preload` lists worlds that are loaded during startup (never generated implicitly). This keeps
`/world`, `/nether`, `/end`, and `/flatlands` from blocking on world creation during gameplay. Set
`worlds.preload-enabled: false` to restore on-demand loading.

`/opme` requires both the `sandbox.opme` permission and a UUID listed in `trusted-uuids`. An empty list denies
everyone; name-based fallbacks are deliberately not used.

---

## Project Structure

```
src/main/java/org/thesandbox/core/
├── TheSandboxCore.java          # Main plugin class
├── commands/
│   ├── BaseCommand.java         # Shared utilities (permissions, messaging, tab completion)
│   ├── MessageService.java      # messages.yml theme loader and Component helpers
│   ├── CommandMessages.java     # String compatibility facade over MessageService
│   ├── SafeConfigService.java   # Typed /config allowlist and validation
│   ├── ISubCommand.java         # Command interface
│   ├── TheSandboxCoreCommand.java
│   ├── guilds/                  # Guild commands (GuildCommand, GChatCommand)
│   ├── misc/                    # Miscellaneous commands (GifMap, Freeze, List, etc.)
│   ├── player/                  # Player-facing commands (Nick, Tag, Coins, Shop, etc.)
│   ├── staff/                   # Staff moderation commands (Rank, Sudo, Gtfo, etc.)
│   ├── trusted/                 # Dev/trusted commands (Config, DevTest, Opme)
│   └── utility/                 # Utility commands (Player, World, Gamemode, etc.)
├── data/
│   ├── PlayerDataListener.java  # Player data persistence (IP, rank, location, etc.)
│   ├── PlayerDataKeys.java      # Data key constants
│   └── GenericDataKeys.java     # Global config keys and message prefixes
├── services/
│   ├── discord/                 # Discord bot and media services
│   │   └── media/
│   │       ├── AnimatedMapService.java   # GIF-to-map conversion
│   │       └── GifFrameDecoder.java      # GIF frame decoding
│   ├── display/                 # Scoreboard and display services
│   ├── login/                   # Login and rank services
│   └── listeners/               # Event listeners (LiteBans, etc.)
├── util/
│   ├── BuildFlavor.java         # Build-flavor allowlists and runtime gate
│   ├── colors/
│   │   ├── Colors.java          # Legacy color code handling
│   │   ├── ComponentBuilder.java # Fluent Adventure component builder
│   │   └── HexColorUtil.java    # Hex color utilities
│   ├── Utils.java               # General utilities (god class — planned refactor)
│   └── registry/
│       └── CommandAutoRegistrar.java  # DI-based command registration
└── services/
    └── ...
```

## Commands Reference

### Player Commands

| Command               | Aliases        | Permission        | Description                      |
|-----------------------|----------------|-------------------|----------------------------------|
| `/adventure`          | `/gma`         | `sandbox.default` | Switch to adventure mode         |
| `/creative`           | `/gmc`         | `sandbox.default` | Switch to creative mode          |
| `/survival`           | `/gms`         | `sandbox.default` | Switch to survival mode          |
| `/spectator`          | `/gmsp`        | `sandbox.default` | Switch to spectator mode         |
| `/coins`              | —              | `sandbox.default` | View/manage in-game currency     |
| `/colorme`            | —              | `sandbox.default` | Change your name color           |
| `/discord`            | `/ds`          | `sandbox.default` | Get Discord invite link          |
| `/dismount`           | —              | `sandbox.default` | Force dismount from entity       |
| `/glow`               | —              | `sandbox.default` | Toggle glowing outline           |
| `/gravity`            | —              | `sandbox.default` | Toggle gravity                   |
| `/heal`               | —              | `sandbox.default` | Heal yourself or another player  |
| `/jumppads`           | `/jp`          | `sandbox.default` | Toggle jump boost mode           |
| `/loginmessage`       | `/lmsg`        | `sandbox.default` | Set your login message           |
| `/marry`              | `/marriage`    | `sandbox.default` | Marriage system                  |
| `/nickgradient`       | `/nickgr`      | `sandbox.default` | Gradient nickname                |
| `/nicknyan`           | —              | `sandbox.default` | Randomized nick colors           |
| `/nickrainbow`        | —              | `sandbox.default` | Rainbow nickname                 |
| `/overworld`          | —              | `sandbox.default` | Teleport to overworld            |
| `/nether`             | —              | `sandbox.default` | Teleport to nether               |
| `/end`                | —              | `sandbox.default` | Teleport to end                  |
| `/flatlands`          | —              | `sandbox.default` | Teleport to flatlands            |
| `/masterbuilderworld` | `/mbw`         | `sandbox.default` | Teleport to MB world             |
| `/potion`             | `/effect`      | `sandbox.default` | Manage potion effects            |
| `/report`             | —              | `sandbox.default` | Report a player                  |
| `/shop`               | —              | `sandbox.default` | Open the shop                    |
| `/skull`              | —              | `sandbox.default` | Get a player skull               |
| `/tag`                | —              | `sandbox.default` | Set custom chat prefix           |
| `/tagnyan`            | —              | `sandbox.default` | Randomized tag colors            |
| `/tagrainbow`         | —              | `sandbox.default` | Rainbow tag                      |
| `/world`              | —              | `sandbox.default` | Teleport to a world              |
| `/gifmap`             | `/animatedmap` | `sandbox.default` | Create animated map from GIF URL |
| `/localspawn`         | `/worldspawn`  | `sandbox.default` | Teleport to world spawn          |
| `/gamemode`           | `/gm`          | `sandbox.default` | Change gamemode                  |
| `/list`               | —              | `sandbox.default` | List online players              |
| `/rank`               | —              | `sandbox.default` | Show or change ranks             |
| `/attributelist`      | `/attrs`       | `sandbox.default` | List all attributes              |
| `/chatfilter`         | —              | `sandbox.default` | Toggle chat filter               |
| `/clearinventory`     | `/clearinv`    | `sandbox.default` | Clear inventory                  |
| `/dismount`           | —              | `sandbox.default` | Force dismount                   |
| `/opme`               | —              | `sandbox.opme`    | Toggle OP (trusted UUIDs only)   |

### Staff Commands (`sandbox.staff`)

| Command           | Aliases     | Description                                            |
|-------------------|-------------|--------------------------------------------------------|
| `/blockcmd`       | —           | Block player commands for 5 minutes                    |
| `/commandspy`     | `/cmdspy`   | Toggle command spy                                     |
| `/expel`          | `/push`     | Push players away                                      |
| `/freeze`         | `/fr`       | Freeze players globally or per-player                  |
| `/gtfo`           | —           | Temporarily ban a player (24h default)                 |
| `/goldwipe`       | —           | Remove all dropped gold                                |
| `/kicknoob`       | `/kickall`  | Kick all non-staff                                     |
| `/managechat`     | `/mchat`    | Mute or clear public chat                              |
| `/mediatoggle`    | `/asciiart` | Toggle Discord media ASCII rendering                   |
| `/mobpurge`       | `/mp`       | Despawn mobs in all worlds                             |
| `/orbit`          | —           | Send a player into space                               |
| `/potionspy`      | `/potspy`   | Toggle potion effect spy                               |
| `/say`            | —           | Broadcast staff message                                |
| `/shush`          | `/hush`     | Hide staff notifications                               |
| `/staffchat`      | `/sc`       | Staff-only chat                                        |
| `/sudo`           | `/gcmd`     | Run command as another player                          |
| `/wildcard`       | —           | Run command for all online players                     |
| `/whoHas`         | `/wh`       | See who has an item                                    |
| `/cage`           | —           | Trap a player in a glass/block/skull cage (terrain auto-restored) |
| `/autoclear`      | `/acr`      | Auto-clear a player's inventory                        |
| `/autotp`         | `/atp`      | Auto-teleport a player on join                         |
| `/deafen`         | —           | Deafen a player                                        |
| `/disguisetoggle` | `/dtoggle`  | Toggle disguises for all players                       |
| `/entitywipe`     | `/ew`       | Remove server entities                                 |
| `/explode`        | —           | Explode a player                                       |
| `/invisible`      | `/invis`    | List/clear invisible players                           |
| `/nickclean`      | `/nc`       | Clean blocked codes from nicknames                     |
| `/denick`         | —           | Remove all online nicknames                            |
| `/aeclear`        | `/aec`      | Clear potion area effects                              |
| `/player info`    | —           | Show detailed player info (IP, UUID, rank, ban status) |
| `/player tp`      | —           | Teleport to player or logout spot                      |
| `/player nickname`| —           | Show a player's nickname (click to copy)               |
| `/config`         | —           | Safe, typed allowlisted player/global config           |
| `/waterflow`      | —           | Toggle water/lava flow                                 |

### Admin/Superuser Commands

| Command             | Aliases         | Permission          | Description                   |
|---------------------|-----------------|---------------------|-------------------------------|
| `/plugincontrol`    | `/plc`          | `sandbox.superuser` | Enable/disable/reload plugins |
| `/loop`             | —               | `sandbox.superuser` | Loop a command (debug)        |
| `/devtoggle`        | —               | `sandbox.superuser` | Developer toggles             |
| `/devtest`          | —               | `sandbox.operator`  | Dev test command              |
| `/slowrestart`      | —               | `sandbox.operator`  | Schedule server restart       |
| `/rawsay`           | —               | `sandbox.admin`     | Broadcast raw message         |
| `/updateallproxies` | —               | `sandbox.superuser` | Update all proxies            |
| `/updatelocal`      | `/pyrorefrence` | `sandbox.superuser` | Update local server           |

### Guild Commands

| Command  | Aliases | Description                                  |
|----------|---------|----------------------------------------------|
| `/guild` | `/g`    | Guild management (create, join, leave, etc.) |
| `/gchat` | `/gc`   | Guild chat                                   |

---

## Key Features

### Player Data Persistence

All player data (last location, IP address, rank, coins, marriage, vanish status) is stored in MySQL via
`PlayerDataListener`. Data is captured on join and saved on quit.

### LiteBans Integration

`/player info` shows active ban/mute status with reason, executor, and duration — revealed on hover. Requires LiteBans
plugin.

### Discord Bridge

- Animated GIF maps via `/gifmap` (download GIF → decode frames → render as animated map item)
- Discord media-to-ASCII art rendering (toggle with `/mediatoggle`)
- Chat-art renderer with `braille`, `block`, `shade`, and `emoji` styles. Braille packs 2×4 subpixels per character
  for much higher detail; configure it with `discord.image-render.style` (the shipped `config.yml` uses `braille`) and
  tune `width` / `height` (defaults `40` × `22`).
- Discord slash commands and reaction listeners

### Cage safety

`/cage` snapshots every block it replaces. Uncaging restores the exact previous block state, including container
contents, sign text, and skull profiles. The snapshot is also written to `cages.yml`; if the server crashes or loses
power while a cage is active, the original terrain is restored automatically on the next startup. Cages may not
overlap each other, and when WorldGuard is installed `/cage` respects its build permissions for players without
bypass.

### ComponentBuilder

Use `ComponentBuilder` for fluent Adventure component creation:

```java 
ComponentBuilder.text("Click me")
    .

gold()
    .

clickEvent(ClickEvent.openUrl("https://example.com"))
        .

hoverText("Visit our website")
    .

sendTo(player);
```

### Command Theme

All command prefixes and common messages live in `messages.yml` and are loaded by `MessageService`. Values use the
legacy `&` / `&#RRGGBB` syntax, so existing text keeps working.

- **`prefixes.server`** — Broadcast/server-wide announcements
- **`prefixes.info`** — Neutral information
- **`prefixes.error`** — Command failures and validation errors
- **`prefixes.usage`** — Syntax/help output
- **`prefixes.command`** — Routine command output
- **`prefixes.success`** — Confirmed state changes

`CommandMessages` remains as a String-returning compatibility facade over `MessageService`, while new code can use
`BaseCommand`'s Component helpers (`sendError`, `sendSuccess`, `sendUsage`, `sendInformation`).

---

## Development

### Adding a New Command

1. Create a class extending `BaseCommand` in the appropriate package
2. Implement `execute()` and optionally `tabComplete()`
3. Register via `CommandAutoRegistrar` (constructor injection) or `plugin.yml`
4. Use `send()`, `sendError()`, `sendSuccess()`, `sendUsage()` from `BaseCommand`

### Build & Test

```bash
./gradlew compileJava    # Check compilation
./gradlew build          # Full build with shadowJar
```

### Code Conventions

- Extend `BaseCommand` — never implement `ISubCommand` directly
- Use `ComponentBuilder` for Adventure components
- Use `Colors.component()` for legacy-to-component conversion
- Use `MessageService` / `messages.yml` for themed prefixes and common messages
- Keep `CommandMessages` only for backwards-compatible String output
- Log with `plugin.getLogger()` at appropriate levels
- Handle null worlds, offline players, and missing plugins gracefully

---

Project Was Started By @ivannomoly And Was Continued by @apfelchorle and contributors

This Was Made Using AI ;P
