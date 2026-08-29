<img src="src/main/resources/assets/ringbongos/icon.png" width="128" align="right" alt="">

# Ring Bong OS

A wall terminal running Ring Bong OS, plus a pile of things that grew up around
it. The terminal is the centrepiece; the rest is the junk drawer, and the junk
drawer is the fun part.

- **Minecraft:** 1.21.11 · **Loader:** Fabric · **Mod id:** `ringbongos`

## The terminal

Place a **Bong Terminal** on a wall and open it. Four little apps:

- ping who is nearby
- ring the bong — which also pulses redstone, so you can wire it to things
- read out world status
- scroll back through the terminal's log

## Everything else

- **Courier Arrow** — put something in your off hand, shoot a container, and the
  stack flies into it. Tells you what got delivered, or why it didn't.
- **Giant Pickaxe** — sweeps up everything worth having below you and reports the
  haul, diamonds counted separately.
- **Trampoline** — a block you bounce on.
- **One Cycle Bed** — sleep near the dragon and it's one cycle. Away from a
  dragon it is, disappointingly, just a bed.
- **Villager Leg** — an item, and a throwable entity. Do not ask.
- **Herobrine mode** — a keybind. *You never leave.*
- **Riding people** — put a saddle on a player or mob and climb on. Sneak to get
  off. They get told they're being ridden.
- **Baby ravagers**, because why not.
- **Smarter respawning** — you wake up in a village, in somebody's house, or deep
  in a stronghold, and it tells you where.
- **Commands** — `/pants` takes your armour off, plus helpers for finding an End
  portal room and summoning things.

## Building

```bash
./gradlew build
```

Some third-party mods are compiled against but **not** redistributed here.
Download them from Modrinth into `libs/` before building.

## Releasing

Push a tag (`v1.0.1`). The workflow builds the jar and attaches it to a GitHub
Release, and uploads to Modrinth once `MODRINTH_PROJECT_ID` is set.
