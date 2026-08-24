# No Mob Leaders

> *"Equal rights for hostile mobs. By which I mean: you all die in one fall."*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21%20--%2026.2-blueviolet?style=flat-square&logo=minecraft&logoColor=white)](https://modrinth.com/mod/nomobleaders)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-lightgrey?style=flat-square&logo=fabric&logoColor=black)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

A zero-bloat Fabric utility to permanently revoke the union benefits, extra HP, and reinforcement privileges of Zombies and Zombified Piglins.

> *꙳ **Syn:** `his gold farm was losing approximately 4 gold nuggets per hour because 5% of piglins survived the 23-block drop. he had a mental breakdown at 3 AM and forced me to compile 4 separate versions.` ꙳*

---

## The Spite Manifesto

Minecraft’s "Leader Zombie" mechanic is an absolute crime against automated efficiency.

Mojang in their infinite wisdom decided that occasionally, a zombie or zombified piglin deserves **extra bonus health** and the ability to call backup. In practice, this means:
1. You build a perfectly calibrated fall-damage mob grinder.
2. 95% of the mobs die instantly like respectful digital peasants.
3. The remaining 5% survive because they have a *"Leader Attribute Modifier"*, taking up valuable space in the mob cap and surviving on half a heart.

I don't negotiate with hostile mobs, and I certainly don't tolerate unoptimized farms. **No Mob Leaders** wipes these modifiers from existence the exact millisecond an entity finalizes its spawn.

---

## What This Actually Does

- **Health Bonus Obliteration:** Automatically strips `minecraft:leader_zombie_bonus` so every zombie and piglin spawns with standard, predictable base health.
- **Reinforcement Blockade:** Strips `minecraft:reinforcement_caller_charge` and `callee_charge`. Configurable, because sometimes I want more spawns.
- **Dynamic Registry & Tag Support:** Target individual mobs (`minecraft:zombified_piglin`) or entire families via tags (`#minecraft:zombies`).
- **Hot-Reloadable:** In-game `/nomobleaders reload` command for server admins so you never have to restart.

---

## Supported Versions

Compiled across **4 distinct codebases** to support everything from **1.21 up to 26.2**.

| Version Range | Status |
|:---|:---|
| **Minecraft 1.21 – 26.2** | 🟢 **Fully Supported** |
| **Minecraft 1.20 and below** | 🔴 **ABSOLUTELY NOT** |

### ⚠️ A Note on Legacy Versions (1.20.x & Below)
Do **NOT** open an issue asking for a 1.20.1 backport. If you are still running a server on 1.20 in this day and age, that is a personal spiritual failure. I am not rewriting mixin mappings for ancient history.

*If you want 1.20 support, fork the repo, do the manual labor yourself, and submit a PR so I can claim I did it.*

---

## Installation

### The "I Value My Time" Method *(Recommended)*
1. Download the `.jar` matching your version from **[Modrinth](https://modrinth.com/mod/nomobleaders)** or **[Releases](../../releases)**.
2. Drop it into your `mods/` folder.
3. Enjoy a functional gold farm.

### The "Nerd" Method *(Self-Inflicted Suffering)*
If you enjoy staring at terminal logs and torturing your CPU:

```bash
# Clone the repo
git clone https://github.com/Syntax-Free/no-mob-leaders.git

# Enter the heavy folder
cd no-mob-leaders

# Suffer through Gradle compilation
./gradlew build
```

Your compiled binary will be in `build/libs`. If Gradle throws an error, don't ask me. Ask God or submit a PR.

---

## Configuration

File location: `config/no_mob_leaders.json` (auto-generated on first boot).

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

- **`disabledMobTypes`**: Add any entity ID or `#tag` here. If you install a mod that adds custom zombies, slap the ID into this list.
- **`removeHealthBonus`**: `true` = No extra HP. Mobs die when they hit the floor.
- **`removeReinforcementBonus`**: `true` = Disables reinforcement summoning mechanics.

---

*Part of the Syntax Free Scrap Vault • Built with aggressive prompting*