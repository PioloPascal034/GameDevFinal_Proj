# Development Status — Aurora Sprint

## Implemented

- Existing Swing game loop retained and normalized to a fixed 60 updates per second.
- Player movement, gravity, jumping, double-jump support, wall/ceiling/floor collision, map-edge blocking, and safe tile access hardened.
- Level loading now validates image dimensions, clamps offsets, handles invalid tile indexes during rendering, and chooses a fallback spawn when a map has no spawn marker.
- Camera utility supports centered following, 1.5x zoom, and world-boundary clamping; Z toggles zoom in play mode.
- Tile rendering is limited to visible columns and uses nearest-neighbor scaling for pixel assets.
- Player animation loading tolerates missing states and safely falls back to an available frame.
- F3 developer overlay reports engine version, player position/velocity/grounded state, camera position, and zoom.
- Dependency-free collision/camera regression smoke test added.
- Reusable 20-slot inventory model added with item definitions, stack-safe quantities, and bounded removal.
- Persistent player inventory integrated with collectible world items and sample Aurora pickups in the first level.
- I-key inventory overlay added with icons/placeholders, quantities, selected-item details, and repeat-safe input handling.
- Existing enemy hierarchy audited and hardened: patrol/platform collision, spawn tile-row initialization, facing-aware Crabby attacks, lesser-enemy player damage, bounded enemy health, and boss death deactivation.
- Existing attack, contact damage, hit-state gating, knockback, invulnerability, and level-transition enemy loading remain integrated through `EnemyManager` and `Playing`.

## Incomplete or intentionally limited

- The project uses PNG level images rather than `.map` text files; legacy image compatibility is preserved.
- Vertical camera following is mathematically supported, but the current presentation levels fit the fixed viewport vertically.
- The optional collision-hitbox visualization remains a future polish item; F3 is the stable diagnostic overlay.
- Audio and some legacy gameplay systems still use asset-specific behavior outside the core engine.
- Inventory is session-persistent only; it is not written to the existing savegame file.

## Planned

- Extract remaining world drawing into a dedicated renderer class.
- Add formal unit-test integration when a test framework is introduced.
- Add authored Aurora demonstration map variants without replacing the existing six levels.
- Continue cleanup of legacy debug prints and unused combat-era systems in a separate sprint.
