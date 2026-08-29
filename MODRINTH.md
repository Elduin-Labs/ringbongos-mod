# Modrinth listing — ringbongos-mod

Everything below is ready to paste. Create the project at
https://modrinth.com/dashboard/projects, then run the command at the bottom.

- **Name:** Ring Bong OS
- **Slug:** `ring-bong-os`
- **Summary:** A wall terminal running Ring Bong OS, plus courier arrows, a giant pickaxe, trampolines, rideable players and Herobrine mode.
- **Categories:** Adventure, Game Mechanics, Utility
- **Environment:** client and server (required on both)
- **License:** MIT
- **Source:** https://github.com/Elduin-Labs/ringbongos-mod
- **Icon:** `src/main/resources/assets/ringbongos/icon.png`
- **Minecraft:** 1.21.11 · **Loader:** Fabric

---

## Description (paste into the body)

A wall terminal running **Ring Bong OS**, plus a pile of things that grew up
around it. The terminal is the centrepiece. The junk drawer is the fun part.

### The terminal

Place a **Bong Terminal** on a wall and open it. Four little apps:

- ping who is nearby
- ring the bong — it pulses redstone too, so you can wire it to things
- read out world status
- scroll back through the log

### Everything else

- **Courier Arrow** — hold something in your off hand, shoot a container, and the
  stack flies into it
- **Giant Pickaxe** — sweeps up everything worth having below you, diamonds
  counted separately
- **Trampoline** — a block you bounce on
- **One Cycle Bed** — sleep near the dragon and it's one cycle. Away from one,
  it's disappointingly just a bed
- **Herobrine mode** — a keybind. *You never leave.*
- **Riding people** — saddle a player or a mob and climb on; sneak to get off
- **Smarter respawning** — wake up in a village, in somebody's house, or deep in
  a stronghold, and be told where
- **`/pants`** — takes your armour off

---

## Then wire it up

```
gh variable set MODRINTH_PROJECT_ID --repo Elduin-Labs/ringbongos-mod --body "<project id>"
cd ~/ringbongos-mod && git tag v1.0.0 && git push origin v1.0.0
```
