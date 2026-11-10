# Horizon Engine v0.1 Aurora — Presentation Guide

## 1. What it is

Horizon Engine is a small reusable Java 2D platformer foundation built on Swing, AWT, and standard Java image/audio APIs. Aurora is its presentation-ready playable demonstration.

## 2. Why it was developed

The engine separates repeatable platformer concerns—game timing, tile maps, collision, camera behavior, animation, and rendering—from game-specific content such as the player, enemies, objects, levels, dialogue, and progression.

## 3. Game loop

The loop runs simulation updates at a fixed 60 UPS and requests Swing repaints independently. A bounded accumulator prevents a paused window from causing an update spiral.

## 4. Tile rendering

Each level is a PNG tile map. Red values identify tile data, green values identify entities/spawn markers, and blue values identify objects. Only columns near the camera are drawn, and pixel art uses nearest-neighbor interpolation.

## 5. Physics and collision

The player uses a rectangular collision box. Gravity and jumps update vertical velocity; horizontal movement is resolved independently. Safe four-corner queries prevent passing through solid tiles, corner clipping, and array errors at map boundaries.

## 6. Camera and zoom

The camera follows the player while clamping to world bounds. Its visible world dimensions are calculated from zoom, so Z can switch between 1.0x and 1.5x without exposing outside-map space.

## 7. Animation

Player states include idle, running, jumping, falling, attack, hit, and death. Frames are loaded from the existing resource folders, with a safe static frame fallback when an optional asset is missing.

## 8. Engine versus game classes

Reusable math and diagnostics live in `engine/`, while the current compatible project systems remain in `main/`, `levels/`, `entities/`, `objects/`, `gamestates/`, and `utilz/`. This keeps existing public APIs and content intact.

## 9. Live demonstration sequence

1. Start at the menu and enter the first level.
2. Walk left and right to show movement and camera tracking.
3. Jump onto platforms and walk off an edge to show gravity and landing.
4. Jump beneath a ceiling and press against walls to show collision resolution.
5. Press Z to demonstrate 1.5x zoom and map-boundary clamping.
6. Press F3 to show player, velocity, grounded, camera, and zoom diagnostics.
7. Walk into the Aurora Shard and Moonleaf pickups, press I, and show stacking, quantities, and selected-item details.
8. Press Z while the inventory is open/closed to show that the UI stays fixed while world zoom changes.
9. Collect a fragment, then use Escape to demonstrate dialogue skip and progression.
10. Approach an existing enemy to demonstrate patrol/detection, contact damage, player attack hitboxes, knockback, and enemy death.

## 10. Future development

The next sprint should add save-file persistence for inventory, extract the remaining renderer, add formal automated tests, author a dedicated Aurora showcase map, and complete optional hitbox visualization. Advanced boss behavior and multiplayer remain outside this sprint.
