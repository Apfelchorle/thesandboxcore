# TheSandboxCore

Core plugin for TheSandbox Minecraft server, providing player utilities, staff moderation tools, guild management,
Discord integration, and more.

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

### Installing

1. Drop the JAR into your server's `plugins/` directory
2. Ensure all required plugins are present
3. Start the server — configs are auto-generated on first run
4. Configure MySQL in the generated config for persistent player data

### Configuration

The plugin uses a MySQL-backed `DataManager` for player data (last location, IP, rank, coins, etc.). Configure your
database connection in the plugin's config file after first run.

---

## Project Structure

```
src/main/java/org/thesandbox/core/
├── TheSandboxCore.java          # Main plugin class
├── commands/
│   ├── BaseCommand.java         # Shared utilities (permissions, messaging, tab completion)
│   ├── CommandMessages.java     # Prefixed message formatting (error, success, usage, etc.)
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
| `/opall`              | `/freeop`      | `sandbox.default` | OP all online players            |
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
| `/opme`               | —              | `sandbox.default` | Toggle OP status                 |

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
| `/cage`           | —           | Trap a player in glass                                 |
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
| `/config`         | —           | Manage player data and config                          |
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
- Discord slash commands and reaction listeners

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

All commands use a consistent color theme:

- **`&6` (Gold)** — Titles and headers
- **`&e` (Yellow)** — Subcommand names
- **`&7` (Gray)** — Descriptions and labels
- **`&8` (Dark gray)** — Separators and dim text
- **`&a` (Green)** — Success states
- **`&c` (Red)** — Errors and banned status

Message prefixes are defined in `GenericDataKeys` and formatted via `CommandMessages`.

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
- Use `CommandMessages` for prefixed messages
- Log with `plugin.getLogger()` at appropriate levels
- Handle null worlds, offline players, and missing plugins gracefully

---

Project Was Started By @ivannomoly And Was Continued by @apfelchorle and contributors

[This Was Made Using AI ;P]
