# Litematica Zone Focus — changelog

English release notes as published on [Modrinth](https://modrinth.com/mod/litematica-zone-focus/changelog), newest first.
The original Russian notes are in [CHANGELOG.ru.md](CHANGELOG.ru.md).

## 0.1.2 — 2026-10-25

Minecraft 26.3, 26.2, 1.21.11

The first Zone Focus build for Minecraft 1.21.11.

#### New
- **Sections — several schematics, each split its own way.** A section is one or more schematics plus a rule: a grid, the schematic's sub-regions, or zones drawn by hand. **New section** window, **Combined bounds** for several schematics, a section list in *Manage zones* (rename, hide, split again, delete), **S2 · Z7/24** labels in the HUD, **Next / Previous section** keys.
- **Other sections don't get in the way:** *Hide other sections* (on by default) and *Other sections' frames*.
- **Cut with the mouse** on the 2D map, one block per step; right-click for *Cut here* / *Remove cut*. **2D map from the top, front and side.**
- **Look: Aell UI, Vanilla or Vanilla+** (*Counting* → *Interface style*).
- **Chat messages are off by default** — feedback appears above the hotbar.

#### Fixed
- fix — a clear Fabric window instead of a crash on world join with an old Fabric Loader.
- fix — Material HUD numbers did not refresh when a second schematic was enabled, moved or turned off inside a zone; a manual zone and a grid counted overlapping schematics differently; auto-switching jittered where two zones overlap.

#### Not yet on 1.21.11
3D of a section made of several schematics, the cut handles in 3D and the height slider need a newer Material HUD than the one available for 1.21.11. Zone Focus detects this by itself: 3D works for single-schematic sections, as before.

#### 📦 You need

Minecraft 1.21.11 · Fabric Loader 0.19.3+ · Fabric API · malilib · Litematica · Java 21 · Litematica Material HUD (optional)

## 0.1.1 — 2026-10-22

Minecraft 26.3

#### New
- **Look: Aell UI, Vanilla or Vanilla+.** Zone Focus settings → *Counting* → *Interface style*. **Vanilla** and **Vanilla+** switch the Zone Focus windows to Minecraft's own look — the zones screen, the zone manager and its map, section split, tool buttons and key hints get vanilla buttons, fields, frames and the Minecraft click sound. Settings are tiles in two columns (**Vanilla**) or rows like Litematica's (**Vanilla+**).
- With **Aell UI** chosen and **Material HUD 1.0.19+** installed, Zone Focus simply uses Material HUD's look, so the two always match.

#### 📦 You need

Minecraft 26.3 · Fabric Loader · Fabric API · malilib · Litematica · Java 25 · Litematica Material HUD 1.0.18+ for 3D sections and library zones (optional)

## 0.1.0 — 2026-10-21

Minecraft 26.3

The biggest Zone Focus update so far: one world can now hold **several schematics, each split into zones its own way**. Your existing zones are kept.

#### Sections — several schematics, each with its own split

A **section** is one or more schematics plus a rule for cutting them into zones: a grid, the schematic's sub-regions, or zones drawn by hand. A castle can be a 3×2×3 grid, a bridge split by sub-regions and a tower one manual zone — all at once in the same world.

- **New section window.** Tick the enabled Litematica placements on the left, pick the method on the right, give the section a name. A schematic that already belongs to another section is marked "in section #N" — ticking it moves it here. **Split again…** in the section menu reopens the same window.
- **Combined bounds.** With two or more schematics ticked, choose **Each separately** or **Combined bounds**: four schematics placed end to end can be cut by one grid, as one build.

- **Section list in Manage zones.** Above the zone panel: number, name, schematic count, rule and progress. Click a section to make it active; right-click for **Rename**, **Hide section**, **Split again…**, **Delete section**. **+ Section** creates a new one. The map, the zone list and 3D always show the active section.

- **Other sections don't get in the way.** **Hide other sections** (on by default) hides the holograms of the other sections' schematics, so you only see what you are building now. **Other sections' frames** draws thin outlines of their zones if you still want to see where they are.
- **Section → zone in the HUD.** With several sections the label reads **S2 · Z7/24** (section · zone); with one section nothing changes.
- **Keys:** **Next section** / **Previous section** (unbound by default). **Next zone crosses sections** lets "Next zone" continue into the next section after the last zone.

#### Cutting and preview

- **Cut with the mouse, one block per step.** Drag the cut lines on the 2D map, or the plane handles in 3D. Right-click the map or the model for **Cut here** / **Remove cut**.

- **2D map from the top, front and side.** 3D gets a **Side** view too.

- **3D for any section**, even one made of several schematics (needs Material HUD 1.0.18). A **height slider** under the 3D view hides everything above the chosen layer, so you can look inside.

- **Voxy world under the hologram.** If Voxy is installed, the 3D view draws the real world around the section. No setup.

#### Other changes

- **Chat messages are off by default.** Feedback (marking tool on/off, check results) now appears above the hotbar; turn **Chat messages** on to get it in chat again.
- **Your zones carry over.** The old zone file is read as one section. A backup (`zonefocus_zones.json.bak-0.0.9`) is made before the first save in the new format.

#### Fixed

- fix — **Material HUD numbers did not refresh** when a second schematic was enabled, moved or turned off inside a zone.
- fix — **a manual zone and a grid counted materials differently** when two schematics overlapped.
- fix — **auto-switching jittered** when you stood where two zones overlap.
- fix — **the zone number on the Material HUD card could disagree** with Zone Focus.

#### Known limitation

Material HUD still counts every enabled schematic inside the active zone's box. If a schematic of another section stands inside that box, its blocks show up in the HUD. Zone Focus's own check and the holograms already keep sections apart.

#### 📦 You need

Minecraft 26.3 · Fabric Loader · Fabric API · malilib · Litematica · Java 25 · Litematica Material HUD 1.0.18+ for 3D sections and library zones (optional)

## 0.0.8 — 2026-10-10

Minecraft 26.3, 26.2

#### New: "Manage zones" — one screen instead of three
Split method, zones per axis, a top-down map with a number on every cell (click to make a zone active) and the zone list with progress, all on one screen.

#### New: 3D zones (with Material HUD 1.0.5+)
- **2D | 3D** in the header: in 3D the schematic preview from the Material HUD library shows zone frames and numbers. Click a zone to make it active.
- **Isometric, Top, Front** camera buttons, as above the library preview.
- **Block-by-block progress:** built blocks as they are, missing ones as a blue ghost, wrong ones red.
- In 2D, a straight-down shot of the schematic sits under the zone cells, turned and mirrored like the placement.

#### New: split a schematic file without placing it
From the Material HUD library: the grid is saved for the file and applied to its placement automatically.

#### Smaller changes
- **Zone auto-switching is off by default** (both "By player position" and "Next one when finished"). Updating turns them off once; turn them back on under "Auto switching".
- With Material HUD 1.0.5+, block verification is Material HUD's Ctrl+B; Ctrl+P toggles it.
- Russian texts call schematic blocks "голограммы", as Litematica does.

#### Fixes
- fix — **Keys could not be bound in the settings:** the click on the field was recorded as the key.

Also out now for **Minecraft 26.2** as 0.0.8+26.2.

#### 📦 You need

Minecraft 26.3 · Fabric Loader · Fabric API · malilib · Litematica · Java 25 · Litematica Material HUD 1.0.5+ for 3D and library zones (optional)

## 0.0.7 — 2026-10-03

Minecraft 26.3

Settings screen update, shared with the other Aell_ Litematica mods:
- **Esc unbinds a hotkey** while capturing; a second click on the field brings the previous key back, the reset button gives the default.
- The settings panel grows to fit every tab name, up to two thirds of the window.
- **Fix:** the search icon is a gold magnifier aligned with the text, and the hint disappears once you click the field.

#### 📦 You need

## 0.0.6 — 2026-08-31

Minecraft 26.3, 26.2

Build for Minecraft 26.3. No functional changes — the same mod, compiled against Litematica 0.29.0 and malilib 0.30.1, with the settings screen moved from GLFW to the new SDL input.

#### 📦 You need

Minecraft 26.3 · Fabric Loader · Fabric API · malilib · Litematica · Java 25 · Litematica Material HUD 0.0.21+ for the shared settings tab (optional)

## 0.0.5 — 2026-08-30

Minecraft 26.2

- **The "Mark the zone" button in the inventory no longer hides under the effects panel.** Since 26.x the game draws active effects to the right of the inventory window — exactly where the button used to sit.
- **Its spot is recalculated every frame** instead of once when the screen opens, so a new effect, an opened recipe book or a GUI scale change are picked up immediately.
- **Order of preference:** right of the window, level with its top → left when effects are active → right below the effects panel when the recipe book occupies the left → below the window or against the screen edge on cramped resolutions. In creative the spot above the window is skipped: the tab row lives there.
- **Buttons other mods add to that screen are taken into account** — it will not sit on top of them.

#### 📦 You need

Minecraft 26.2 · Fabric Loader · Fabric API · malilib · Litematica · Java 25

## 0.0.4 — 2026-08-29

Minecraft 26.2

- **"Zone Focus mod"** on the Counting tab turns the whole mod off: zone counting, schematic clipping, zone boxes, zone verification, mouse marking and the action hotkeys. The settings screen always opens, so you can always switch it back on. On by default — nothing changes if you leave it alone.

#### 🔀 Changed: with Material HUD installed, zone settings live on its "Zone Focus" tab

- This mod's own zones screen is not opened at all in that case. Mod Menu, the mod's button on Litematica's screens and the settings hotkey all lead to that tab, and the same actions there drive this mod.
- **Zone verification and hotkeys stay here**, on this mod's own settings screen — reachable by "Zone Focus settings..." on that tab.
- The duplicate **"Zones" button** in Material HUD's settings header is gone.
- Without Material HUD nothing changes: the zones screen is where it was.

#### 🐛 Fixed

- **The mod's button on Litematica's Render Layers screen** no longer overlaps the new "All" tab added in Litematica 0.28.6.

#### 📦 You need

Minecraft 26.2 · Fabric Loader · Fabric API · malilib · Litematica · Java 25

## 0.0.3 — 2026-08-22

Minecraft 26.2

- New: type the numbers on the split screen — click a zone count or zone size and type it in (Enter to accept, Esc to cancel, Tab for the next axis, ↑/↓ for ±1).
- New: a top-down cut diagram on the split screen — the schematic's real shape with the cut lines and zone "1" drawn in the same colours you see in the world; Y cuts get a "Y ×n" reminder.
- Shared settings with Material HUD's new built-in Zone Focus module: with both mods installed this mod keeps running the engine, and the Zone Focus tab in Material HUD edits the same live settings. Keybinds stay separate. Settings migrate from the built-in module on first run.
- fix — verification markers no longer go stale beyond render distance: reloaded chunks are re-checked.
- Key binding capture shows the pressed keys immediately, and Escape restores the previous binding instead of losing it.
- Settings tabs never wrap to a second row; the panel widens itself to fit them. "Reset all?" no longer wipes settings when you click "No".
- The button on Litematica screens got its own 16×16 icon; add-on buttons no longer vanish in a narrow window.

## 0.0.2 — 2026-08-16

Minecraft 26.2

- Verification is a live mode now. It scans the zone layer by layer, counts what's in the slice you're on, and the markers update the moment you place a block. Changing layers or zones doesn't reset it.
- fix — markers no longer get stuck showing "missing" after you walk away from a zone and come back.
- Switching zones fades one into the other instead of blanking the schematic, and it no longer rebuilds the whole thing.
- Easy Place works into a zone from outside it. Hidden ghost blocks past the zone border stop eating your clicks.
- New settings screen: search across all tabs with Ctrl + F, sections, reset per row, per tab or everything, marks on values you changed, keybind conflicts shown, and full keyboard control.
- A "Zone Focus" button with its own icon now sits in Litematica's own row of tabs, on both of its screens.
- Settings tabs stay on one row. They shrink, shorten with a tooltip, or scroll instead of jumping to a second line.
- Ctrl + J opens the zones screen. Default binds were reworked, a rebind applies right away, and you can bind mouse buttons (all but left).
- fix — clicking "No" on "Reset all?" used to wipe every setting.
- fix — while binding a key you now see what you're pressing, and Escape keeps your old bind instead of clearing it.
- fix — the add-on buttons no longer cover the search bar, and no longer disappear when the window is narrow.
- fix — space in the search box no longer switched the setting you had selected.
- fix — "Format Error" showing up instead of some labels and tooltips.
- fix — Zone Focus no longer affects layers in MiniHUD, Tweakeroo and other malilib mods.
- Dark style matching Litematica, colour only where it carries meaning, and a 512x512 icon.

## 0.0.1 — 2026-08-02

Minecraft 26.2

_No notes._

