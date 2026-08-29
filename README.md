# Ring Bong OS

A wall terminal for Minecraft 1.21.11 (Fabric) with a home screen of rounded app icons — an iOS-looking little OS.

## The Bong Terminal

Craft it, stick it on a wall, right-click it. Tap an icon on the home screen; every app has a
`< Home` button back to it.

| App | What it does |
| --- | --- |
| **Ping** | Everyone within 64 blocks of the terminal, nearest first, with distances. |
| **Bong** | Rings a two-tone "ring… bong" chime. While it chimes, the panel puts out redstone power 15 — so a comparator, lamp, or door next to the terminal fires with the bell. |
| **Status** | Coordinates, dimension, day, weather, uptime, redstone state. |
| **Log** | The last dozen things that happened to this terminal: who opened it, who rang it. |

Feeding the terminal redstone power rings it too, so it works from a button or a pressure plate.

The screen refreshes off the server once a second, so Ping and Status stay live while you watch them.

## Recipe

```
I G I     I = iron ingot
R B R     G = glass pane
I I I     R = redstone, B = bell
```

## Notes

- The chime lives in the block's `stage` property and scheduled block ticks — no block entity.
- The log and uptime are held in server memory (`TerminalLog`), so they reset when the server
  restarts and when a terminal is re-placed.
- Nothing in the GUI is trusted: every button sends a payload, and the server re-checks the
  block and that you are within 8 blocks before acting.

## Building

```
./gradlew build
```

The jar lands in `build/libs/ringbongos-1.0.0.jar`.
