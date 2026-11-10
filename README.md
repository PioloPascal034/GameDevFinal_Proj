# Horizon Engine v0.1 Aurora

Horizon Engine is the reusable Java Swing/AWT foundation behind this 2D platformer demonstration. The existing game-state, level, entity, object, audio, and UI architecture is preserved; the engine pass adds safer tile queries, fixed-rate simulation, camera math, nearest-neighbor rendering, animation fallbacks, and presentation diagnostics.

## Run

Requires Java 21 or newer.

```powershell
$src = rg --files src -g '*.java'
javac -encoding UTF-8 -d bin $src
java -cp bin main.MainClass
```

Resources are loaded from `src/res` when running from the project root. The included `run.ps1` can be used after updating its Java executable path if needed.

## Controls

- A/D or Left/Right: move
- Space or Up: jump
- Left Shift: run
- Left mouse: attack
- Right mouse: dash/power attack
- F: ranged attack when unlocked
- Z: toggle 1.0x / 1.5x camera zoom
- I: open/close inventory
- Arrow keys while inventory is open: select a slot
- F3: toggle developer overlay
- Escape: pause or skip active dialogue

## Engine highlights

- Fixed 60 UPS simulation with bounded catch-up work
- Safe solid-tile queries at map boundaries
- Four-corner player collision checks for floors, ceilings, walls, and gaps
- Camera clamping that accounts for zoom and small maps
- Visible-column tile rendering and nearest-neighbor scaling
- Reusable state animation loading with static fallbacks for missing assets
- Presentation overlay for player position, velocity, grounded state, camera, and zoom
- 20-slot inventory with stack limits, world collectibles, and screen-space UI

See [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) and [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md) for the sprint status and demo script.
