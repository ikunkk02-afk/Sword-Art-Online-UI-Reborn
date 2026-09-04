# Sword Art Online UI: Reborn

Sword Art Online UI: Reborn is an unofficial Fabric 1.21.1 port and continuation of MCUI / SAOUI.

This branch currently contains the first-stage porting foundation. The complete SAO HUD, screens, theme rendering, scripting, and legacy compatibility layers have not been ported yet. Progress and blockers are tracked in [PORTING_1.21.1.md](PORTING_1.21.1.md).

## Project identity

- Display name: Sword Art Online UI: Reborn
- Minecraft: 1.21.1
- Loader: Fabric
- Language/runtime: Kotlin on Java 21
- Mappings: Mojang Official Mappings
- Mod ID: `mcui`
- Legacy resource namespace: `saoui`

The `mcui` mod ID is intentionally retained for compatibility with modern MCUI resources, configuration, and integrations. Historical `saoui` resources remain part of the compatibility plan.

## Projects

- Original project: https://github.com/Bluexin/mcui
- Port / continuation: https://github.com/ikunkk02-afk/Sword-Art-Online-UI-Reborn

Bluexin and Tencao remain credited as the original project authors. This repository is an unofficial derivative port and is not affiliated with the Sword Art Online rights holders.

## Building

Use a Java 21 JDK and run:

```text
./gradlew clean build
```

For a development client:

```text
./gradlew runClient
```

## License

This derivative work follows the original MCUI project's GNU General Public License, version 3 or later. See [LICENSE](LICENSE).
