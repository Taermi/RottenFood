# RottenFood

> **This is a fork of the original [RottenFood](https://github.com/TBlueF/RottenFood) Sponge plugin by [Blue](https://www.bluecolored.de), ported to Paper.**

A Paper plugin that gives items the ability to age. Configure any item to change over time — the longer it sits in a chest, the older it gets.

---

## What does RottenFood do?

With **RottenFood** you can define items that grow older the longer they exist in the world.
Each item tracks its age independently, and you can define multiple age states it passes through.

Some examples of what you can do:

- 🥩 Steaks stored in a chest become `rotten_flesh` after a week if nobody eats them
- 🥔 All potatoes become poisonous if you log off for a month
- 🌸 Flowers placed in a chest dry out after 3 days without water and sunlight
- 🍎 Apples in your hotbar age slower because you're carrying them around

---

## Features

- **Age states** — Define multiple stages an item passes through as it ages. Each stage can change the item's name, lore, and/or replace it with a completely different item.
- **Aging modifiers** — Speed up or slow down aging based on what else is in the same inventory. For example, ice in the same chest could slow food aging down.
- **Configurable update interval** — Control how often the plugin checks inventories for aging.
- **Item stacking** — Items with different ages can be configured to stack together within a time window (`item-stacking-intervall`).
- **Show age** — Optionally display the current age of an item in its lore.
- **Reload command** — `/rottenfood reload` reloads the config without restarting the server.

---

## Configuration

The plugin generates a `defaultConfig.conf` (HOCON format) in its data folder on first launch. You can define as many item configurations as you want.

Each item config supports:

| Key | Description |
|---|---|
| `items` | List of items this config applies to |
| `states` | List of age states, each with an `age` (in seconds), optional `name`, `lore`, and `replacement-item` |
| `ageing-modifier` | List of modifier rules — an `item` + `min-item-count` + `multiplier` |
| `item-stacking-intervall` | Seconds within which items are considered the same age for stacking (default: 60) |
| `show-age` | Whether to show the item's age in its lore (default: false) |

### Example

```hocon
{
    items: [
        { type: "minecraft:beef" }
        { type: "minecraft:cooked_beef" }
    ]
    states: [
        { age: 0,      name: "&fFresh Steak" }
        { age: 86400,  name: "&eOld Steak",        lore: "&7Starting to smell..." }
        { age: 604800, name: "&cRotten Steak",      replacement-item: { type: "minecraft:rotten_flesh" } }
    ]
    ageing-modifier: [
        { item: { type: "minecraft:ice" }, min-item-count: 1, multiplier: 0.5 }
    ]
    item-stacking-intervall: 3600
    show-age: false
}
```

---

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/rottenfood reload` | Reloads the plugin config | `rottenfood.reload` (default: op) |

---

## Building

```bash
./gradlew build
```

The output jar (with all dependencies shaded) will be in `build/libs/`.

---

## Installing

Drop the compiled `.jar` into the `plugins/` folder of your **Paper** server (Paper 26.1.2+ recommended) and start the server.
The plugin will generate a default config on first launch.

---

## Contributing

Pull requests are welcome!
Explain what and why you changed it, and make sure everything is well documented and formatted.
There is no styleguide — just try to match the rest of the code.

---

## License

[MIT](LICENSE)
