# Lineage — Origins, evolved

Minecraft **1.20.1 / Forge 47.x**. Pick an origin, grow into it, branch into specializations.
Everything about origins, abilities and evolutions is **JSON** — no Java needed to add content.

## Build & run

1. Download the official **Forge 1.20.1 MDK** from files.minecraftforge.net and copy its
   `gradlew`, `gradlew.bat` and `gradle/` folder into this project (the wrapper can't be bundled here).
2. Open the project in IntelliJ IDEA (or Eclipse) and let Gradle sync. Requires **JDK 17**.
3. Run `./gradlew runClient` to test, `./gradlew build` for the jar (`build/libs/`).

## Playing

| Key | Action |
|-----|--------|
| `Z` `X` `C` | Use active ability 1 / 2 / 3 (rebindable) |
| `V` | Open the Evolution tree |

Keybinds only work in the world when no menu or chat screen is open. Rebind them in
**Options → Controls → Key Binds → Lineage**. The first active ability is on `Z` by default;
passive abilities do not use a key.

First join opens the origin selection screen. Open Evolutions with `V`, select a node, and
work through its quest checklist. Complete every objective, then claim the evolution in the
same screen. Nodes can require kills, carrying items, visiting dimensions, or spending time
under a condition. Some branches are **mutually exclusive** (`excludes`) — choose your path.

`/lineage list` shows loaded origins. Admin commands (op level 2):
`/lineage set <players> <origin>` and `/lineage reset <players>`. For testing, use
`/lineage complete <players> <node>` to finish its checklist while keeping the claim step,
or `/lineage unlock <players> <node>` to skip directly to an evolution and grant its abilities.
Both commands suggest node IDs; the node must belong to each target's current origin.

Origins show a subtle particle trail around the player's body, with an extra accent after
the first evolution. The particle style follows the origin (fire, bubbles, cloud, or spores).

## Adding an origin (no code)

Create `data/<namespace>/lineage/origins/<name>.json` in a datapack, or in
`src/main/resources/`. For a world datapack, put it under
`saves/<world>/datapacks/<pack>/data/<namespace>/lineage/origins/<name>.json`.
Run `/reload` to pick up changes live. Errors are logged with the file name. Use
`merling.json` as a template. The built-in test origin is `lineage:glowcap`.

```jsonc
{
  "name": "Display name", "description": "...", "icon": "minecraft:feather",
  "impact": 1,                // 1-3, shown in the picker
  "abilities": [ ... ],       // what you start with
  "evolutions": [ ... ]       // the tree
}
```

### Quest requirements

Each evolution node can contain a `quests` checklist. Every quest must be complete before
the node can be claimed. Quest progress is saved with the player and shown in the Evolution
Journal.

```json
{
  "id": "deep_hunter", "name": "Deep Hunter", "description": "Earn the title of the deep.",
  "icon": "minecraft:trident", "requires": ["thick_scales"],
  "quests": [
    { "id": "drowned", "type": "kill", "entity": "minecraft:drowned", "amount": 12,
      "description": "Defeat 12 Drowned" },
    { "id": "pearls", "type": "hold_item", "item": "minecraft:ender_pearl", "amount": 8,
      "description": "Carry 8 ender pearls" },
    { "id": "nether", "type": "visit_dimension", "dimension": "minecraft:the_nether",
      "description": "Travel to the Nether" },
    { "id": "deep_time", "type": "condition_time", "condition": "in_water", "seconds": 600,
      "description": "Spend 10 minutes underwater" }
  ],
  "abilities": [ ... ]
}
```

Quest types are `kill` (entity ID, entity tag such as `#minecraft:undead`, or `any`),
`hold_item` (current inventory count), `visit_dimension`, and `condition_time` (seconds).
Use a unique quest `id` within each node. The `description` is the text shown in the UI.

### Evolution nodes
```json
{ "id": "inferno", "name": "Inferno", "description": "...", "icon": "minecraft:ghast_tear",
  "requires": ["molten_core"], "excludes": ["magma_skin"],
  "quests": [ ... ], "abilities": [ ... ] }
```
The journal lays nodes out in a selectable path list. `requires` connects the path; `quests`
set the objectives for that specific evolution.

### Ability types
Every ability has: `id` (unique), `type`, `name`, `description`, `icon` (item id).
Active abilities also take: `cooldown` (seconds), `hunger_cost`, `use_condition`, `sound`.

| type | what it does | extra fields |
|------|--------------|--------------|
| `attribute` | Passive attribute modifier | `attribute`, `amount`, `operation` (`addition`/`multiply_base`/`multiply_total`) |
| `effect` | Passive potion effect | `effect`, `amplifier`, `duration`, `condition` |
| `damage_modifier` | Scale incoming damage (0 = immune) | `damage_type` (id or `#tag`), `multiplier`, `condition` |
| `dash` | **Active** — launch where you look | `strength`, `upward` |
| `active_effect` | **Active** — effect on self or nearby | `effect`, `amplifier`, `duration`, `target` (`self`/`nearby`), `radius` |
| `fireball` | **Active** — shoot a fireball | `power` (0 = small, >0 = explosive) |
| `command` | **Active** — run vanilla commands as the player | `command` or `commands[]` |

The `command` type means anything vanilla commands/functions can do, an ability can do.

### Conditions
`always`, `in_water`, `in_lava`, `in_rain`, `wet`, `sunlight`, `night`, `in_nether`, `in_end`,
`underground`, `on_fire`, `sprinting`, `sneaking`, `airborne`, `low_health`. Prefix with `!` to negate.

## Adding a brand-new ability type (Java)

```java
public class HealAbility extends Ability {
    public HealAbility(JsonObject json) { super(json); }
    @Override public boolean isActive() { return true; }
    @Override public boolean activate(ServerPlayer p) { p.heal(6); return true; }
}
// in your mod setup:
Abilities.register(new ResourceLocation("lineage", "heal"), HealAbility::new);
```
Then use `"type": "heal"` from any JSON. Other mods can register types the same way.

## Layout
- `ability/` — ability base class, registry, conditions, `types/` built-ins
- `origin/` — origin/evolution model, datapack loader, quest tracking and unlock rules
- `data/` — per-player capability (origin, quest progress, unlocked nodes, cooldowns)
- `network/` — sync + action packets
- `client/` — keybinds, HUD, origin picker, evolution quest journal
- `command/`, `event/` — `/lineage` command and Forge event wiring

## Known limitations / ideas
- Fire immunity cancels the damage but the player still visually burns.
- Only one origin at a time; changing origin needs `/lineage reset`. An "orb of reselection" item is an easy next step.
- Ideas: more quest types (block breaking, crafting, advancements), more ability types (teleport, summon, aura), resource pools (mana/stamina), per-origin shaders/skins, multiple origin layers.
