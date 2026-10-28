# Litematica Zone Focus

Splits a Litematica schematic into zones, renders only the zone you are building, tracks its progress and checks it against the schematic.

[Modrinth](https://modrinth.com/mod/litematica-zone-focus) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/litematica-zone-focus) · [Discord](https://discord.gg/jTU2yawThv)

This branch (`mc/1.21.11`) is the source of **0.1.2+1.21.11** for **Minecraft 1.21.11** (Fabric).
Branches: `main` (26.3), `mc/26.2` (26.2), `mc/1.21.11` (1.21.11). Release notes: [CHANGELOG.md](CHANGELOG.md).

## Building

You need JDK 21+ (Gradle comes with the wrapper).

```sh
./libs/fetch-libs.sh   # malilib, litematica, litematica-material-hud from Modrinth, SHA-1 checked
./gradlew build        # the jar lands in build/libs/
```

The jars in `libs/` are compile-only: the mod talks to them at runtime, none of their code is bundled.
`aell-ui/` is the shared settings-screen module (MIT, by Aell_); the build copies it into the `com.aell.zonefocus.shaded.ui` package so two mods carrying it do not clash.

## License

GPL-3.0-or-later — see [LICENSE](LICENSE). Third-party credits and the disclaimer are in [NOTICE.md](NOTICE.md).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

