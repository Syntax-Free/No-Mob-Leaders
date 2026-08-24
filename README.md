A small percentage of zombies spawn as "leaders," granting them massive bonus health pools and high reinforcement spawn rates (read more [here](https://minecraft.wiki/w/Zombie)). **No Mob Leaders** strips these hidden bonuses.

---

## Features
* Strips the vanilla leader bonus attribute modifiers (`minecraft:leader_zombie_bonus`) that give mobs bonus HP.
* Strips bonus reinforcement caller and callee chance modifiers.
* Choose exactly which bonuses to disable and which mobs or entity tags (`#tag`) are affected.
* Only required on the server (or singleplayer world).

---

## Configuration
A very simple and straightforward config file located at `config/no-mob-leaders`:

```json
{
  "configVersion": 1,
  "disabledMobTypes": [
    "minecraft:zombie",
    "minecraft:zombified_piglin",
    "minecraft:husk",
    "minecraft:drowned",
    "minecraft:zombie_villager"
  ],
  "removeHealthBonus": true,
  "removeReinforcementBonus": true
}
```
* Supports entity tags using `#namespace:tag_name` syntax.
* `/nomobleaders reload` to reload the config file without restarting the server.